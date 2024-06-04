package stock.apps;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.*;

import stock.market.AnalyzeMarketData;
import stock.market.BuyStocks;
import stock.market.RetrieveMarketData;
import stock.market.SellPositions;

public class daytrader {

	private static org.apache.logging.log4j.Logger logger = LogManager.getLogger(daytrader.class);

	// This application is meant for day trading.
	// It will run continuously, and will periodically
	// pull market data and buy stocks. It will sell
	// them when profitable. And it clears all positions
	// at end of day.
	public static void main(final String argv[]) {

		try {
			logger.info("Starting daytrader");
			ResourceBundle rb = ResourceBundle.getBundle("alpaca");
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
			while (1 == 1) {
				if (isMarketOpen(marketOpen, marketClose)) {
					logger.info("Today is a week day, market should be open");
					// First, let's sell any profitable positions we have
					SellPositions sellPos = new SellPositions();
					BuyStocks buyStocks = new BuyStocks();
					if (sellPos.sellProfitablePositions()) {
						logger.info("Selling processed without errors.");
					} else {
						logger.info("Error when trying to sell positions");
					}
					RetrieveMarketData marketData = new RetrieveMarketData();
					if (buyStocks.validTime()) {
						if (marketData.getMarketData())
							logger.info("Retrieved market data.  Continuing with analysis");
						AnalyzeMarketData analyze = new AnalyzeMarketData();
						if (analyze.analyzeStocks()) {
							logger.info("Finished analyzing stocks");
							ArrayList<String> stocksToBuy = analyze.getStocksToBuy();
							buyStocks.setStocksToBuy(stocksToBuy);
							logger.info("Buying stocks now.");
							if (buyStocks.buyStocks()) {
								logger.info("Finished buying stocks");
							} else {
								logger.info("Error buying stocks");
							}
						}
					} else logger.info("Outside valid buying time, not getting market data");
				}
				logger.info("Sleeping for " + timeToSleep + " seconds.");
				Thread.sleep(timeToSleep);
			}
		} catch (

		Exception e) {
			e.printStackTrace();
		}
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
