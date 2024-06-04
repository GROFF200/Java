package stock.apps;

import java.util.Calendar;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;

import stock.market.BuyStocks;

/**
 * Read a file to know what stocks to buy, and which ones to sell.
 * That file will also indicate the point at which each should be sold.
 * Note that this is meant to run throughout the day, but not to run
 * continuously forever and ever.
 * @author aarond
 *
 */
public class swingtrader {

	private static org.apache.logging.log4j.Logger logger = LogManager.getLogger(swingtrader.class);
	
	public static void main(final String argv[]) {
		
		try {
			ResourceBundle rb = ResourceBundle.getBundle("alpaca");
			String stocksToBuy = rb.getString("StocksToBuy");
			String stocksToSell = rb.getString("StocksToSell");
			String marketOpen = rb.getString("marketOpen");
			String marketClose = rb.getString("marketClose");
			String sleepInterval = rb.getString("sleepInterval");
			long interval = 0;
			try {
				interval = Long.parseLong(sleepInterval);
			} catch (Exception ee) {
				interval = 15;
			}
			long timeToSleep = (15 * 60) * 1000;
			boolean stocksBought = false;
			while (1==1) {
				if (isMarketOpen(marketOpen, marketClose)) {
					if (!stocksBought) {
						//Buy stocks on the list, then make sure we don't issue any
						//more buy orders.
						//Leaving this blank for now, because I want the software to sell
						//but not buy for me
						stocksBought = true;
					}
					String sellStocks[] = stocksToSell.split(",");
					for (int x=0; x<sellStocks.length; x++) {
						
					}
					Thread.sleep(timeToSleep);
				}
			}
		} catch (Exception e) { e.printStackTrace(); }
	}
	
	private static boolean isMarketOpen(final String marketOpen, final String marketClose) {

		try {
			Calendar cal = Calendar.getInstance();
			int day = cal.get(Calendar.DAY_OF_WEEK);
			if (day != Calendar.SATURDAY && day != Calendar.SUNDAY) {
				// It isn't a weekend, so now see if we are in the correct time
				Calendar cal1 = Calendar.getInstance();
				String openInfo[] = marketOpen.split(":");
				String openHour = openInfo[0];
				String openMinute = openInfo[1];
				cal1.set(Calendar.HOUR_OF_DAY, Integer.parseInt(openHour));
				cal1.set(Calendar.MINUTE, Integer.parseInt(openMinute));
				long currentTime = cal.getTimeInMillis();
				logger.info("Current time is: " + cal.getTime().toString());
				long openTime = cal1.getTimeInMillis();
				logger.info("Open time calendar object is: " + cal1.getTime().toString());
				Calendar cal2 = Calendar.getInstance();
				String closeInfo[] = marketClose.split(":");
				String closeHour = closeInfo[0];
				String closeMinute = closeInfo[1];
				cal2.set(Calendar.HOUR_OF_DAY, Integer.parseInt(closeHour));
				cal2.set(Calendar.MINUTE, Integer.parseInt(closeMinute));
				long closeTime = cal2.getTimeInMillis();
				logger.info("Close time calendar object is: " + cal2.getTime().toString());
				if (currentTime > openTime)
					logger.info("Current time > open Time");
				if (currentTime < closeTime)
					logger.info("current time < close time");
				if (currentTime > openTime && currentTime < closeTime)
					return true;
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
}
