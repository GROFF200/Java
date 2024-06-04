package stock.market;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.logging.log4j.LogManager;

import net.jacobpeterson.alpaca.*;
import net.jacobpeterson.alpaca.enums.BarsTimeFrame;
import net.jacobpeterson.alpaca.enums.OrderSide;
import net.jacobpeterson.alpaca.enums.OrderTimeInForce;
import net.jacobpeterson.alpaca.rest.exception.AlpacaAPIRequestException;
import net.jacobpeterson.domain.alpaca.account.Account;
import net.jacobpeterson.domain.alpaca.marketdata.Bar;
import net.jacobpeterson.domain.alpaca.order.Order;

public class BuyStocks {

	private static org.apache.logging.log4j.Logger logger = LogManager.getLogger(BuyStocks.class);

	private ArrayList<String> stocksToBuy = new ArrayList<String>();

	public void setStocksToBuy(final ArrayList<String> buy) {
		stocksToBuy = buy;
	}

	public boolean validTime() {
		
		try {
			ResourceBundle rb = ResourceBundle.getBundle("alpaca");
			String buyStartTime = rb.getString("buyStartTime");
			String buyEndTime = rb.getString("buyEndTime");
			logger.info("Buy start time: "+buyStartTime);
			logger.info("End start time: "+buyEndTime);
			String startInfo[] = buyStartTime.split(":");
			String startHour = startInfo[0];
			String startMinute = startInfo[1];
			String endInfo[] = buyEndTime.split(":");
			String endHour = endInfo[0];
			String endMinute = endInfo[1];
			int iStartHour = Integer.parseInt(startHour);
			int iStartMinute = Integer.parseInt(startMinute);
			int iEndHour = Integer.parseInt(endHour);
			int iEndMinute = Integer.parseInt(endMinute);
			Calendar cal = Calendar.getInstance();
			logger.info("Current time is: "+cal.getTime().toString());
			long currentTime = cal.getTimeInMillis();
			Calendar startCal = Calendar.getInstance();
			startCal.set(Calendar.HOUR_OF_DAY, iStartHour);
			startCal.set(Calendar.MINUTE, iStartMinute);
			long startTime = startCal.getTimeInMillis();
			logger.info("Start time is: "+startCal.getTime().toString());
			Calendar endCal = Calendar.getInstance();
			endCal.set(Calendar.HOUR_OF_DAY, iEndHour);
			endCal.set(Calendar.MINUTE, iEndMinute);
			long endTime = endCal.getTimeInMillis();
			logger.info("End time is: "+endCal.getTime().toString());
			if (currentTime > startTime && currentTime < endTime) return true;
		} catch (Exception e) { e.printStackTrace(); }
		return false;
	}
	
	public boolean buyStocks() {

		try {
			// Let's first see if we are in the allowed time
			if (!validTime()) {
				logger.info("Outside valid time for buying.");
				return false;
			}
			// First we need to know how much cash we have
			AlpacaAPI alpacaAPI = new AlpacaAPI();
			// First found out how much cash we have to play with
			Account account = alpacaAPI.getAccount();
			String cash = account.getCash();
			// Let's keep about 5% of cash on hand. Calculations in real time can
			// run into problems if you push up to the limit constantly
			float percentAmount = (float) (Float.parseFloat(cash) * .05);
			float cashValue = Float.parseFloat(cash);
			cashValue = cashValue - percentAmount;
			logger.info("Have $" + cashValue + " on hand.");
			// Now let's run through the stocks to buy, get their current price,
			// then order
			logger.info("Looking at stocks to buy.  There are " + stocksToBuy.size() + " entries to examine.");
			for (int x = 0; x < stocksToBuy.size(); x++) {
				String symbolInfo = stocksToBuy.get(x);
				String info[] = symbolInfo.split("\\^");
				String symbol = info[1];
				logger.info("Getting current price info for stock " + symbol);
				float priceVal = 0;
				// Let's get current stock info for the symbol.
				// Need the current price to buy it
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
					ZonedDateTime start = ZonedDateTime.of(startYear, startMonth, startDay, startHour, startMinute, 0,
							0, ZoneId.of("America/Chicago"));
					ZonedDateTime end = ZonedDateTime.of(endYear, endMonth, endDay, endHour, endMinute, 0, 0,
							ZoneId.of("America/Chicago"));
					logger.info("Calling bars API for symbol: " + symbol);
					Map<String, ArrayList<Bar>> bars = alpacaAPI.getBars(BarsTimeFrame.ONE_DAY, symbol.trim(), null,
							start, end, null, null);
					logger.info("Size of Bars returned: "+bars.size());
					Set<String> set = bars.keySet();
					Iterator i = set.iterator();
					for (Bar bar : bars.get(symbol)) {
						Double closePrice = bar.getC();
						priceVal = closePrice.floatValue();
						logger.info("Current price for " + symbol + " is " + priceVal);
					}
					logger.info("Retrieved price information for stock");
				} catch (AlpacaAPIRequestException ae) {
					ae.printStackTrace();
					System.out.println("Exception calling API!");
				}
				int orderQuantity = 0;
				float purchasePrice = 0;
				logger.info("Determining purchase price and quantity");
				while (purchasePrice < cashValue && priceVal > 0 && orderQuantity < 10) {
					orderQuantity++;
					purchasePrice += priceVal;
					// logger.info("For "+orderQuantity+" price is "+purchasePrice+". Cash on hand
					// is: "+cashValue);
				}
				if (priceVal == 0) {
					logger.info("Stock has no price.  Bars API isn't giving data as expected for " + symbol);
				} else {
					// Subtract to get things within limits here
					orderQuantity--;
					purchasePrice -= priceVal;
					logger.info("Quantity is " + orderQuantity + " and price is " + purchasePrice);
					if (orderQuantity > 0) {
						boolean orderPlaced = false;
						// Okay, now we can purchase this stock
						logger.info("Submitting order for: " + orderQuantity + " units of " + symbol);
						logger.info("Current cash on hand is: " + cashValue);
						logger.info("Amount of purchase is " + (orderQuantity * purchasePrice));
						try {
							Order aaplLimitOrder = alpacaAPI.requestNewLimitOrder(symbol, orderQuantity, OrderSide.BUY,
									OrderTimeInForce.DAY, new Double(purchasePrice), true);
							logger.info("Order ID: " + aaplLimitOrder.getId());
							orderPlaced = true;
						} catch (AlpacaAPIRequestException ae) {
							logger.info("Exception placing order!");
							ae.printStackTrace();
						}
						if (orderPlaced)
							cashValue -= purchasePrice;
					} else {
						logger.info("Can't buy stock, not enough cash or nothing to buy.");
					}
				}

			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
}
