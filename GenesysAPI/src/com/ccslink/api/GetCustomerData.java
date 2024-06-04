package com.ccslink.api;

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

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import com.ccslink.objects.CustomerData;
import com.ccslink.objects.JwtBuilder;

public class GetCustomerData {
	private CustomerData customerData = new CustomerData();

	// Set the required parameters for the JWT payload
	private String subject = "clientApplicationID";
	private String tokenType = "Bearer";

	// Set the expiration time of the token to one hour from now
	private long expirationTimeMillis = System.currentTimeMillis() + 3600000; // 1 hour

	public CustomerData getCustomerData() {
		return customerData;
	}

	public boolean retrieveCustomerData(final String phoneNumber, final String serviceUrl,
			final String privateKeyPath) {
		try {
			String privateKeyContent = readPrivateKey(privateKeyPath);
			PrivateKey privateKey = getPrivateKey(privateKeyContent);
			String jwtToken = createJWTToken(subject, tokenType, expirationTimeMillis, privateKey);
			String finalUrl = serviceUrl + "?PhoneNumber=" + phoneNumber;
			HttpClient client = new DefaultHttpClient();
			System.out.println("Calling serviceURL " + finalUrl);
			HttpGet request = new HttpGet(finalUrl);
			request.addHeader("Content-type", "application/json");
			request.addHeader("Authorization", "Bearer " + jwtToken);

			HttpResponse response = client.execute(request);
			System.out.println("Getting response");
			StatusLine status = response.getStatusLine();
			System.out.println("STATUS CODE: " + status.getStatusCode());
			if (status.getStatusCode() == 200) {
				StringBuffer sb = new StringBuffer();
				BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

				String line = "";
				while ((line = rd.readLine()) != null) {
					sb.append(line);
				}
				rd.close();
				System.out.println("Response: " + sb.toString());
				JSONObject jsonObject = new JSONObject(sb.toString());
				if (jsonObject.has("PhoneNumber"))
					customerData.setPhoneNumber(jsonObject.getString("PhoneNumber"));
				if (jsonObject.has("CustomerNumber"))
					customerData.setCustomerNumber(jsonObject.getString("CustomerNumber"));
				if (jsonObject.has("SocialSecurityNumber"))
					customerData.setSocialSecurityNumber(jsonObject.getString("SocialSecurityNumber"));
				if (jsonObject.has("AccountNumber"))
					customerData.setAccountNumber(jsonObject.getString("AccountNumber"));
				if (jsonObject.has("Address1"))
					customerData.setAddress1(jsonObject.getString("Address1"));
				if (jsonObject.has("Address2"))
					customerData.setAddress2(jsonObject.getString("Address2"));
				if (jsonObject.has("Address3"))
					customerData.setAddress3(jsonObject.getString("Address3"));
				if (jsonObject.has("CustomerName"))
					customerData.setCustomerName(jsonObject.getString("CustomerName"));
				if (jsonObject.has("DNIS"))
					customerData.setDNIS(jsonObject.getString("DNIS"));
				if (jsonObject.has("DateOfBirth"))
					customerData.setDateOfBirth(jsonObject.getString("DateOfBirth"));
				if (jsonObject.has("HomePhone"))
					customerData.setHomePhone(jsonObject.getString("HomePhone"));
				if (jsonObject.has("LocalTelNumber"))
					customerData.setLocalTelNumber(jsonObject.getString("LocalTelNumber"));
				if (jsonObject.has("PIN"))
					customerData.setPIN(jsonObject.getString("PIN"));
				if (jsonObject.has("ANI"))
					customerData.setANI(jsonObject.getString("ANI"));
				if (jsonObject.has("Language"))
					customerData.setLanguage(jsonObject.getString("Language"));
				if (jsonObject.has("Queue"))
					customerData.setQueue(jsonObject.getString("Queue"));
				if (jsonObject.has("message"))
					customerData.setMessage(jsonObject.getString("message"));
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
			InputStream in = GetCustomerData.class.getResourceAsStream("/main/resources/private_key.pem");
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
