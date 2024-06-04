package com.ccslink.objects;

import org.apache.commons.codec.binary.Base64;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import org.json.JSONObject;

public class JwtBuilder {
    public static String buildJWT(String subject, String tokenType, long expirationTimeMillis, RSAPrivateKey privateKey) throws Exception {
        // Header
        JSONObject header = new JSONObject();
        header.put("alg", "RS256");
        header.put("typ", "JWT");

        // Payload
        JSONObject payload = new JSONObject();
        payload.put("subject", subject);
        payload.put("token_type", tokenType);
        payload.put("expires_in", expirationTimeMillis / 1000); // Convert to seconds

        // Base64Url encode the header and payload
        String encodedHeader = Base64.encodeBase64URLSafeString(header.toString().getBytes("UTF-8"));
        String encodedPayload = Base64.encodeBase64URLSafeString(payload.toString().getBytes("UTF-8"));

        // Create the signature
        String toSign = encodedHeader + "." + encodedPayload;
        Signature signatureInstance = Signature.getInstance("SHA256withRSA");
        signatureInstance.initSign(privateKey);
        signatureInstance.update(toSign.getBytes("UTF-8"));
        byte[] signatureBytes = signatureInstance.sign();

        // Base64Url encode the signature
        String encodedSignature = Base64.encodeBase64URLSafeString(signatureBytes);

        // The final JWT token is the concatenation of the encoded parts
        String jwtToken = encodedHeader + "." + encodedPayload + "." + encodedSignature;
        return jwtToken;
    }
}

