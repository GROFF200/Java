package com.ccslink.api;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.codec.binary.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.ccslink.objects.CustomerData;
import com.ccslink.objects.JwtBuilder;

public class UpdateCustomerData {

	// Set the required parameters for the JWT payload
	private String subject = "clientApplicationID";
	private String tokenType = "Bearer";

	// Set the expiration time of the token to one hour from now
	private long expirationTimeMillis = System.currentTimeMillis() + 3600000; // 1 hour

	public boolean updateCustomerData(final CustomerData customerData, final String serviceUrl, final String privateKeyPath) {

		try {
			String privateKeyContent = readPrivateKey(privateKeyPath);
			PrivateKey privateKey = getPrivateKey(privateKeyContent);
			String jwtToken = createJWTToken(subject, tokenType, expirationTimeMillis, privateKey);
			HttpPost post = new HttpPost(serviceUrl);
			post.addHeader("Content-type", "application/json");
			post.addHeader("Authorization", "Bearer " + jwtToken);

			String result = "";
			HttpClient client = new DefaultHttpClient();

			JSONObject json = new JSONObject();
			json.put("PhoneNumber", customerData.getPhoneNumber());
			json.put("CustomerNumber", customerData.getCustomerNumber());
			json.put("SocialSecurityNumber", customerData.getSocialSecurityNumber());
			json.put("AccountNumber", customerData.getAccountNumber());
			json.put("Address1", customerData.getAddress1());
			json.put("Address2", customerData.getAddress2());
			json.put("Address3", customerData.getAddress3());
			json.put("CustomerName", customerData.getCustomerName());
			json.put("DNIS", customerData.getDNIS());
			json.put("DateOfBirth", customerData.getDateOfBirth());
			json.put("HomePhone", customerData.getHomePhone());
			json.put("LocalTelNumber", customerData.getLocalTelNumber());
			json.put("PIN", customerData.getPIN());
			json.put("ANI", customerData.getANI());
			json.put("Language", customerData.getLanguage());
			json.put("Queue", customerData.getQueue());
			// send a JSON data
			String jsonStr = json.toString();
			System.out.println("JSON: " + jsonStr);
			post.setEntity(new StringEntity(jsonStr));

			HttpResponse response = client.execute(post);
			int code = response.getStatusLine().getStatusCode();
			System.out.println("STATUS: " + code);
			if (code == 200 || code == 201) {
				result = EntityUtils.toString(response.getEntity());
				System.out.println("RESULT: " + result);
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
    private static String readPrivateKey(String filePath) throws IOException {
        BufferedReader reader = null;
        if (filePath != null && !filePath.isEmpty()) {
            reader = new BufferedReader(new FileReader(filePath));
        } else {
            InputStream in = UpdateCustomerData.class.getResourceAsStream("/main/resources/private_key.pem");
            reader = new BufferedReader(new InputStreamReader(in));
        }
        StringBuilder privateKey = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            privateKey.append(line).append("\n");
        }
        reader.close();
        return privateKey.toString();
    }
    
    private static PrivateKey getPrivateKey(String privateKeyContent) throws Exception {
		privateKeyContent = privateKeyContent.replaceAll("\\n", "").replace("-----BEGIN PRIVATE KEY-----", "")
				.replace("-----END PRIVATE KEY-----", "");
		;
		KeyFactory kf = KeyFactory.getInstance("RSA");
		PKCS8EncodedKeySpec keySpecPKCS8 = new PKCS8EncodedKeySpec(Base64.decodeBase64(privateKeyContent));
		return kf.generatePrivate(keySpecPKCS8);
	}

	private static String createJWTToken(String subject, String tokenType, long expirationTimeMillis,
			PrivateKey privateKey) {
		try {
			Map<String, Object> payload = new HashMap();
			payload.put("sub", subject);
			payload.put("token_type", tokenType);
			payload.put("expires_in", expirationTimeMillis / 1000); // Convert to seconds
			if (privateKey instanceof RSAPrivateKey) {
				return JwtBuilder.buildJWT(subject, tokenType, expirationTimeMillis, (RSAPrivateKey) privateKey);
			} else {
				throw new IllegalArgumentException("Expected RSAPrivateKey");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
