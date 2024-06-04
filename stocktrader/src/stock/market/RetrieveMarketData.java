package stock.market;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.jacobpeterson.alpaca.*;
import net.jacobpeterson.alpaca.enums.BarsTimeFrame;
import net.jacobpeterson.alpaca.enums.OrderSide;
import net.jacobpeterson.alpaca.enums.OrderTimeInForce;
import net.jacobpeterson.alpaca.rest.exception.AlpacaAPIRequestException;
import net.jacobpeterson.domain.alpaca.account.Account;
import net.jacobpeterson.domain.alpaca.marketdata.Bar;
import net.jacobpeterson.domain.alpaca.order.Order;
import stock.apps.daytrader;
import stock.util.MarketDataWrapper;

public class RetrieveMarketData {

	private static org.apache.logging.log4j.Logger logger = LogManager.getLogger(RetrieveMarketData.class);
	private static ArrayList<MarketDataWrapper> sandpData = new ArrayList<MarketDataWrapper>();

	private static String getFileContents(final String fileName) {

		try {
			StringBuffer sb = new StringBuffer();
			BufferedReader fin = new BufferedReader(new FileReader(fileName));
			String line = "";
			while ((line = fin.readLine()) != null) {
				sb.append(line);
			}
			return sb.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void deleteFiles(final String location) {

		try {
			File file = new File(location);
			String fileList[] = file.list();
			for (int x = 0; x < fileList.length; x++) {
				File tempFile = new File(location + fileList[x]);
				tempFile.delete();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static boolean getMarketData() {

		try {
			// Read the configured file and store the information
			ResourceBundle rb = ResourceBundle.getBundle("alpaca");
			String sandpFile = rb.getString("sandp_symbols_file");
			String marketDataDays = rb.getString("marketdatadays");
			int marketDays = Integer.parseInt(marketDataDays);
			String sandpFileContents = getFileContents(sandpFile);
			ArrayList<String> namesUsed = new ArrayList<String>();
			JsonArray convertedObject = new Gson().fromJson(sandpFileContents, JsonArray.class);
			for (int x = 0; x < convertedObject.size(); x++) {
				JsonElement arr = convertedObject.get(x);
				JsonObject obj = arr.getAsJsonObject();
				String name = obj.get("Name").getAsString();
				String sector = obj.get("Sector").getAsString();
				String symbol = obj.get("Symbol").getAsString();
				// System.out.println("NAME: " + name + " SYMBOL: " + symbol);
				if (!namesUsed.contains(name)) {
					MarketDataWrapper wrapper = new MarketDataWrapper();
					wrapper.setName(name);
					wrapper.setSector(sector);
					wrapper.setSymbol(symbol);
					sandpData.add(wrapper);
					namesUsed.add(name);
					wrapper = null;
				}
			}
			// Now let's go through and call Alpaca to get some information on these guys
			// First we need to create the Calendar objects that give us a date range
			Calendar cal = Calendar.getInstance();
			// Get the past 10 days of market data for this stock
			cal.add(Calendar.DAY_OF_YEAR, -1 * marketDays);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			Calendar cal2 = Calendar.getInstance();
			cal2.add(Calendar.DAY_OF_YEAR, -1);
			cal2.set(Calendar.HOUR_OF_DAY, 23);
			cal2.set(Calendar.MINUTE, 59);
			AlpacaAPI alpacaAPI = new AlpacaAPI();
			int startYear = cal.get(Calendar.YEAR);
			int startMonth = cal.get(Calendar.MONTH);
			startMonth++;
			int startDay = cal.get(Calendar.DAY_OF_MONTH);
			int startHour = cal.get(Calendar.HOUR_OF_DAY);
			int startMinute = cal.get(Calendar.MINUTE);
			int endYear = cal2.get(Calendar.YEAR);
			int endMonth = cal2.get(Calendar.MONTH);
			endMonth++;
			int endDay = cal2.get(Calendar.DAY_OF_MONTH);
			int endHour = cal2.get(Calendar.HOUR_OF_DAY);
			int endMinute = cal2.get(Calendar.MINUTE);
			// Before we retrieve data, lets delete any files in the location we will be
			// writing to
			String marketDataLocation = rb.getString("marketdatalocation");
			deleteFiles(marketDataLocation);
			int numRequests = 0;
			logger.info("Retrieving S&P stock information.   There are " + sandpData.size() + " items.");
			for (int x = 0; x < sandpData.size(); x++) {
				if (numRequests > 190) {
					logger.info("We can only send 200 requests per minute.  Sleeping for 1 minute then resuming");
					Thread.sleep(60000);
					numRequests = 0;
				}
				MarketDataWrapper wrapper = sandpData.get(x);
				String name = wrapper.getName();
				String symbol = wrapper.getSymbol();
				ZonedDateTime start = ZonedDateTime.of(startYear, startMonth, startDay, startHour, startMinute, 0, 0,
						ZoneId.of("America/Chicago"));
				ZonedDateTime end = ZonedDateTime.of(endYear, endMonth, endDay, endHour, endMinute, 0, 0,
						ZoneId.of("America/Chicago"));

				try {
					Map<String, ArrayList<Bar>> bars = alpacaAPI.getBars(BarsTimeFrame.ONE_DAY, symbol, null, start, end,
							null, null);
					numRequests++;
					logger.info("Writing information for " + symbol);
					for (Bar bar : bars.get(symbol)) {
						PrintWriter fpw = new PrintWriter(
								new FileOutputStream(marketDataLocation + symbol + ".txt", true));
						fpw.println("#time,open,high,low,close,volume");
						fpw.print(ZonedDateTime.ofInstant(Instant.ofEpochSecond(bar.getT()), ZoneOffset.UTC));
						fpw.print("," + bar.getO());
						fpw.print("," + bar.getH());
						fpw.print("," + bar.getL());
						fpw.print("," + bar.getC());
						fpw.println("," + bar.getV());
						fpw.close();
					}
				} catch (AlpacaAPIRequestException ae) {
					ae.printStackTrace();
					logger.info("Exception calling API!");
				}
			}
			logger.info("Finished retrieving market data");
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
}
