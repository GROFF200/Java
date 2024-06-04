package stock.apps;

import java.io.BufferedReader;
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

import net.jacobpeterson.alpaca.*;
import net.jacobpeterson.alpaca.enums.BarsTimeFrame;
import net.jacobpeterson.alpaca.enums.OrderSide;
import net.jacobpeterson.alpaca.enums.OrderTimeInForce;
import net.jacobpeterson.alpaca.rest.exception.AlpacaAPIRequestException;
import net.jacobpeterson.domain.alpaca.account.Account;
import net.jacobpeterson.domain.alpaca.marketdata.Bar;
import net.jacobpeterson.domain.alpaca.order.Order;
import net.jacobpeterson.domain.alpaca.position.Position;
import stock.util.simplestockwrapper;

public class buyorders {

	public static ArrayList<simplestockwrapper> readFile(String fileLocation) {

		try {
			ArrayList<simplestockwrapper> values = new ArrayList<simplestockwrapper>();
			BufferedReader fin = new BufferedReader(new FileReader(fileLocation + "buy.txt"));
			String line = "";
			while ((line = fin.readLine()) != null) {
				String info[] = line.split("\\^");
				String symbol = info[1];
				String price = info[2];
				simplestockwrapper s = new simplestockwrapper();
				s.setSymbol(symbol);
				s.setPrice(price);
				values.add(s);
			}
			fin.close();
			return values;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void main(final String argv[]) {

		try {
			ResourceBundle rb = ResourceBundle.getBundle("alpaca");
			String fileLocation = rb.getString("tradedatalocation");
			String reserveCash = rb.getString("reserve");
			String maxPercent = rb.getString("maxpercent");
			float fReserveCash = Float.parseFloat(reserveCash);
			float fPercent = Float.parseFloat(maxPercent);
			String marketMaker = rb.getString("marketmakerbuy");
			String marketPercent = rb.getString("marketpercent");
			String executeOrders = rb.getString("executeorders");
			boolean executeOrder = false;
			if (executeOrders.equals("yes"))
				executeOrder = true;
			boolean marketMakerEnabled = false;
			if (marketMaker.equals("yes"))
				marketMakerEnabled = true;
			float fMarketPercent = 0;
			fMarketPercent = Float.parseFloat(marketPercent);
			ArrayList<simplestockwrapper> symbols = readFile(fileLocation);
			AlpacaAPI alpacaAPI = new AlpacaAPI();
			// First found out how much cash we have to play with
			Account account = alpacaAPI.getAccount();
			String cash = account.getCash();
			float cashValue = Float.parseFloat(cash);
			if (cashValue > fReserveCash) {
				System.out.println("Our cash on hand is actually greater than the reserve.  Setting max cash to: "
						+ (cashValue - fReserveCash));
				cashValue = cashValue - fReserveCash;
			}
			// Next let's make sure to find out what stocks we're holding. Don't buy a stock
			// if we already have it
			ArrayList<Position> pos = alpacaAPI.getOpenPositions();
			ArrayList<String> ownedStocks = new ArrayList<String>();
			for (int x = 0; x < pos.size(); x++) {
				Position position = pos.get(x);
				position.getSymbol();
				if (!ownedStocks.contains(position.getSymbol())) {
					System.out.println("We already own stock for: " + position.getSymbol());
					ownedStocks.add(position.getSymbol());
				}
			}
			for (int x = 0; x < symbols.size(); x++) {
				simplestockwrapper s = symbols.get(x);
				String symbol = s.getSymbol();
				System.out.println("*** Checking on symbol: " + symbol);
				if (!ownedStocks.contains(symbol)) {
					String price = s.getPrice();
					float priceVal = Float.parseFloat(price);
					// Let's make sure no single order can consume more than the configured
					// percentage of our cash
					float percentCash = (float) (cashValue * fPercent);
					float newCashDiff = cashValue - percentCash;
					float newCashLimit = cashValue - newCashDiff;
					System.out.println("Our cash on hand is " + cashValue + ".  Our spending limit for this stock is: "
							+ newCashLimit);
					// Let's make an API call to get the price within the last 15 minutes. Reduce
					// changes this order
					// will fail.
					try {
						Calendar cal = Calendar.getInstance();
						// Get the past 10 days of market data for this stock
						cal.add(Calendar.MINUTE, -15);
						Calendar cal2 = Calendar.getInstance();
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
						ZonedDateTime start = ZonedDateTime.of(startYear, startMonth, startDay, startHour, startMinute,
								0, 0, ZoneId.of("America/Chicago"));
						ZonedDateTime end = ZonedDateTime.of(endYear, endMonth, endDay, endHour, endMinute, 0, 0,
								ZoneId.of("America/Chicago"));
						Map<String, ArrayList<Bar>> bars = alpacaAPI.getBars(BarsTimeFrame.FIFTEEN_MINUTE, symbol, null,
								start, end, null, null);
						for (Bar bar : bars.get(symbol)) {
							Double closePrice = bar.getC();
							priceVal = closePrice.floatValue();
							// System.out.println("Current price for " + symbol + " is " + priceVal);
						}
					} catch (AlpacaAPIRequestException ae) {
						ae.printStackTrace();
						System.out.println("Exception calling API!");
					}
					System.out.println("The estimated cost for 1 unit of " + symbol + " stock is: " + priceVal);
					// Figure out how many we can buy without hitting our limit
					int orderQuantity = 0;
					float purchasePrice = 0;
					// If the price of a single stock is greater than our limit
					// then don't go ahead to buy anything.
					if (priceVal < newCashLimit) {
						while (purchasePrice < newCashLimit) {
							orderQuantity++;
							purchasePrice += priceVal;
						}
						// Subtract to get things within limits here
						orderQuantity--;
						purchasePrice -= priceVal;
						/*
						 * System.out.println( "We can order " + orderQuantity +
						 * " stocks for a purchase price of: " + purchasePrice);
						 */
						System.out.println("Submitting order for: " + orderQuantity + " units of " + symbol);
						cashValue -= purchasePrice;
						float buyPrice = priceVal;
						if (marketMakerEnabled) {
							System.out.println("Market maker strategy is enabled.  Decreasing purchase price.");
							float priceAdd = priceVal * fMarketPercent;
							buyPrice -= priceAdd;
							System.out.println(
									"Documented purchase price is:" + priceVal + " and we are buying for " + buyPrice);
						}
						/*try {
							if (executeOrder) {
								Order aaplLimitOrder = alpacaAPI.requestNewLimitOrder(symbol, orderQuantity,
										OrderSide.BUY, OrderTimeInForce.DAY, new Double(buyPrice), true, null);
								System.out.println("Order ID: " + aaplLimitOrder.getId());
							} else {
								System.out.println("Execution of orders is disabled, not buying.");
							}
						} catch (AlpacaAPIRequestException ae) {
							ae.printStackTrace();
							System.out.println("Exception calling API!");
						}*/
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
