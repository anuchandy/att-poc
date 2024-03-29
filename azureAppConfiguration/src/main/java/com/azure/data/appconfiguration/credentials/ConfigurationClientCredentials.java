// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.data.appconfiguration.credentials;

import android.text.TextUtils;
import android.util.Base64;

import com.azure.core.implementation.util.ImplUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import okio.Buffer;

/**
 * Credentials that authorizes requests to Azure App Configuration. It uses content within the HTTP request to
 * generate the correct "Authorization" header value.
 */
public class ConfigurationClientCredentials {
    private static final String HOST_HEADER = "Host";
    private static final String DATE_HEADER = "Date";
    private static final String CONTENT_HASH_HEADER = "x-ms-content-sha256";
    private static final String[] SIGNED_HEADERS = new String[]{HOST_HEADER, DATE_HEADER, CONTENT_HASH_HEADER };
    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final CredentialInformation credentials;
    private final AuthorizationHeaderProvider headerProvider;

    /**
     * Creates an instance that is able to authorize requests to Azure App Configuration service.
     *
     * @param connectionString Connection string in the format "endpoint={endpoint_value};id={id_value};secret={secret_value}"
     * @throws NoSuchAlgorithmException When the HMAC-SHA256 MAC algorithm cannot be instantiated.
     * @throws InvalidKeyException When the {@code connectionString} secret is invalid and cannot instantiate the HMAC-SHA256 algorithm.
     */
    public ConfigurationClientCredentials(String connectionString) throws InvalidKeyException, NoSuchAlgorithmException {
        credentials = new CredentialInformation(connectionString);
        headerProvider = new AuthorizationHeaderProvider(credentials);
    }

    /**
     * Gets the base URI of the Azure App Configuration instance based on the provided connection string.
     *
     * @return The base URI of the configuration service extracted from connection string provided.
     */
    public URL baseUri() {
        return this.credentials.baseUri();
    }

    /**
     * Gets a list of headers to add to a request to authenticate it to the Azure APp Configuration service.
     * @param url the request url
     * @param httpMethod the request HTTP method
     * @param contents the body content of the request
     * @return a map of headers to add for authorization
     */
    public Map<String, String> getAuthorizationHeaders(URL url, String httpMethod, Buffer contents) {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        messageDigest.update(contents.readByteArray());
        return headerProvider.getAuthenticationHeaders(url, httpMethod, messageDigest);
    }

    private static class AuthorizationHeaderProvider {
        private final String signedHeadersValue = TextUtils.join(";", SIGNED_HEADERS);
        private final CredentialInformation credentials;
        private final Mac sha256HMAC;

        AuthorizationHeaderProvider(CredentialInformation credentials) throws NoSuchAlgorithmException, InvalidKeyException {
            this.credentials = credentials;

            sha256HMAC = Mac.getInstance("HmacSHA256");
            sha256HMAC.init(new SecretKeySpec(credentials.secret(), "HmacSHA256"));
        }

        private Map<String, String> getAuthenticationHeaders(final URL url, final String httpMethod, final MessageDigest messageDigest) {
            final Map<String, String> headers = new HashMap<>();
            final String contentHash = Base64.encodeToString(messageDigest.digest(), Base64.NO_WRAP);

            // All three of these headers are used by ConfigurationClientCredentials to generate the
            // Authentication header value. So, we need to ensure that they exist.
            headers.put(HOST_HEADER, url.getHost());
            headers.put(CONTENT_HASH_HEADER, contentHash);

            if (headers.get(DATE_HEADER) == null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
                dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                headers.put(DATE_HEADER, dateFormat.format(new Date()));
            }

            addSignatureHeader(url, httpMethod, headers);
            return headers;
        }

        private void addSignatureHeader(final URL url, final String httpMethod, final Map<String, String> httpHeaders) {
            String pathAndQuery = url.getPath();
            if (url.getQuery() != null) {
                pathAndQuery += '?' + url.getQuery();
            }

            String[] headers = new String[SIGNED_HEADERS.length];
            int i = 0;
            for (String hk : SIGNED_HEADERS) {
                headers[i] = httpHeaders.get(hk);
                i++;
            }
            // String-To-Sign=HTTP_METHOD + '\n' + path_and_query + '\n' + signed_headers_values
            // Signed headers: "host;x-ms-date;x-ms-content-sha256"
            // The line separator has to be \n. Using %n with String.format will result in a 401 from the service.
            String stringToSign = httpMethod.toUpperCase(Locale.US) + "\n" + pathAndQuery + "\n" + TextUtils.join(";", headers);

            final String signature = Base64.encodeToString(sha256HMAC.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8)), Base64.NO_WRAP);
            httpHeaders.put(AUTHORIZATION_HEADER, String.format("HMAC-SHA256 Credential=%s, SignedHeaders=%s, Signature=%s",
                    credentials.id(),
                    signedHeadersValue,
                    signature));
        }
    }

    private static class CredentialInformation {
        private static final String ENDPOINT = "endpoint=";
        private static final String ID = "id=";
        private static final String SECRET = "secret=";

        private URL baseUri;
        private String id;
        private byte[] secret;

        URL baseUri() {
            return baseUri;
        }

        String id() {
            return id;
        }

        byte[] secret() {
            return secret;
        }

        CredentialInformation(String connectionString) {
            if (ImplUtils.isNullOrEmpty(connectionString)) {
                throw new IllegalArgumentException(connectionString);
            }

            String[] args = connectionString.split(";");
            if (args.length < 3) {
                throw new IllegalArgumentException("invalid connection string segment count");
            }

            for (String arg : args) {
                String segment = arg.trim();
                String lowerCase = segment.toLowerCase(Locale.US);

                if (lowerCase.startsWith(ENDPOINT)) {
                    try {
                        this.baseUri = new URL(segment.substring(ENDPOINT.length()));
                    } catch (MalformedURLException ex) {
                        throw new IllegalArgumentException(ex);
                    }
                } else if (lowerCase.startsWith(ID)) {
                    this.id = segment.substring(ID.length());
                } else if (lowerCase.startsWith(SECRET)) {
                    String secretBase64 = segment.substring(SECRET.length());
                    this.secret = Base64.decode(secretBase64, Base64.DEFAULT);
                }
            }

            if (this.baseUri == null || this.id == null || this.secret == null) {
                throw new IllegalArgumentException("Could not parse 'connectionString'."
                        + " Expected format: 'endpoint={endpoint};id={id};secret={secret}'. Actual:" + connectionString);
            }
        }
    }
}