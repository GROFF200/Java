package com.ccslink.test;

import com.ccslink.api.*;
import com.ccslink.objects.CustomerData;

public class TestService {

	public static void main(final String argv[]) {
		
		try {
			String serviceUrl = "https://httpbin.org/get";
			String token = "*****";
			//String serviceUrl = "https://yudw2r7ut5-vpce-05c20c52044b20120.execute-api.us-east-1.amazonaws.com/comm/customer";
			//String serviceUrl = "";
			//Retrieve the customer data
			GetCustomerData data = new GetCustomerData();
			System.out.println("*** Calling retrieveCustomerData");
			String phone = "%2B88876666666";
			data.retrieveCustomerData(phone, serviceUrl, null);
			System.out.println("*** After calling retrieveCustomerData");
			
			//Let's get the data that was returned from the previous request
			CustomerData customerInfo = data.getCustomerData();
			//Here's how you get every field that could be returned
			String accountNumber = customerInfo.getAccountNumber();
			String address1 = customerInfo.getAddress1();
			String address2 = customerInfo.getAddress2();
			String address3= customerInfo.getAddress3();
			String ANI = customerInfo.getANI();
			String customerName = customerInfo.getCustomerName();
			String customerNumber = customerInfo.getCustomerNumber();
			String dateOfBirth = customerInfo.getDateOfBirth();
			String DNIS = customerInfo.getDNIS();
			String homePhone = customerInfo.getHomePhone();
			String language = customerInfo.getLanguage();
			String localTelNumber = customerInfo.getLocalTelNumber();
			String phoneNumber = customerInfo.getPhoneNumber();
			String PIN = customerInfo.getPIN();
			String queue = customerInfo.getQueue();
			String socialSecurityNumber = customerInfo.getSocialSecurityNumber();
			
			//Now if you want to change one of these values, just call the corresponding
			//"set" method and pass in the new value.
			customerInfo.setANI("9999999999");
			
			//Now let's send the updated customer info
			UpdateCustomerData update = new UpdateCustomerData();
			System.out.println("*** Calling updateCustomerData");
			update.updateCustomerData(customerInfo, serviceUrl, null);
			System.out.println("*** After calling updateCustomerData");
		} catch (Exception e) { e.printStackTrace(); }
	}
}
