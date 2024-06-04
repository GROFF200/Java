package stock.apps;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.ResourceBundle;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

import net.jacobpeterson.alpaca.*;
import net.jacobpeterson.alpaca.enums.BarsTimeFrame;
import net.jacobpeterson.alpaca.enums.OrderSide;
import net.jacobpeterson.alpaca.enums.OrderTimeInForce;
import net.jacobpeterson.alpaca.rest.exception.AlpacaAPIRequestException;
import net.jacobpeterson.domain.alpaca.account.Account;
import net.jacobpeterson.domain.alpaca.marketdata.Bar;
import net.jacobpeterson.domain.alpaca.order.Order;
import net.jacobpeterson.domain.alpaca.position.Position;

import org.apache.logging.log4j.LogManager;

public class StockManager {

	private static org.apache.logging.log4j.Logger logger = LogManager.getLogger(StockManager.class);

	public static void main(final String argv[]) {

		try {
			net.jacobpeterson.alpaca.AlpacaAPI alpacaAPI = new net.jacobpeterson.alpaca.AlpacaAPI();
			ResourceBundle rb = ResourceBundle.getBundle("alpaca");
			String marketOpen = rb.getString("marketOpen");
			String marketClose = rb.getString("marketClose");
			String audioFile = rb.getString("AlertFile");
			String goingUpFile = rb.getString("StockGoingUpAlertFile");
			String goingDownFile = rb.getString("StockBackDownAlertFile");
			String sellList = rb.getString("ManagerSellList");
			String stocksToSell[] = sellList.split(",");
			String sleepInterval = rb.getString("ManagerSleepInterval");
			long sleepInMinutes = Long.parseLong(sleepInterval);
			long sleepInMs = (sleepInMinutes * 60) * 1000;
			HashMap<String, Boolean> sellAlerts = new HashMap<String, Boolean>();
			HashMap<String, Float> trackingStock = new HashMap<String, Float>();
			ArrayList<String> sellStocks = new ArrayList<String>();
			for (int x = 0; x < stocksToSell.length; x++) {
				String stockName = stocksToSell[x];
				sellStocks.add(stockName);
				sellAlerts.put(stockName, false);
			}
			float totalPotentialProfit = 0;
			while (1 == 1) {
				boolean soundPlayed = false;
				if (isMarketOpen(marketOpen, marketClose)) {
					net.jacobpeterson.domain.alpaca.account.Account account = alpacaAPI.getAccount();
					System.out.println("Cash: " + account.getCash());
					logger.info("Cash on hand: " + account.getCash());
					ArrayList<net.jacobpeterson.domain.alpaca.position.Position> pos = alpacaAPI.getOpenPositions();
					totalPotentialProfit = 0;
					
					for (int x = 0; x < pos.size(); x++) {
						net.jacobpeterson.domain.alpaca.position.Position position = pos.get(x);
						String symbol = position.getSymbol();
						String currentPrice = position.getCurrentPrice();
						String originalPrice = position.getCostBasis();
						String quantity = position.getQty();
						String unrealizedProfitLossPercent = position.getUnrealizedPlpc();
						//System.out.println("Symbol: "+symbol);
						float unrealizedProfitPercent = 0;
						if (symbol.equals("339CVR011")) { unrealizedProfitPercent=0; } 
						else {
						     //System.out.println("Unrealized Profit Loss Percent: "+unrealizedProfitLossPercent);
						     unrealizedProfitPercent = Float.parseFloat(unrealizedProfitLossPercent);
						     unrealizedProfitPercent *= 100;
						}
						if (unrealizedProfitPercent > 0) {
							float currentPriceVal = Float.parseFloat(currentPrice);
							float originalPriceVal = Float.parseFloat(originalPrice);
							float quantityVal = Float.parseFloat(quantity);
							float currentSaleVal = quantityVal * currentPriceVal;
							float currentProfitVal = currentSaleVal - originalPriceVal;
							totalPotentialProfit += currentProfitVal;
						}
						if (unrealizedProfitPercent < 10 && sellStocks.contains(symbol)) {
							float lastProfitValue = 0;
							try {
								lastProfitValue = trackingStock.get(symbol);
							} catch (Exception ee) {
								lastProfitValue = 0;
							}
							if (lastProfitValue > 0) {
								System.out.println("Stock " + symbol
										+ " dropped below 10% since last time we looked at it.   Removing it from tracking list for now.");
								trackingStock.remove(symbol);
								playSound(goingDownFile);
							}
						}
						if (unrealizedProfitPercent >= 10 && sellStocks.contains(symbol)) {
							System.out.println("Stock " + symbol + " has more than 10% profit!  Profit gain is: "
									+ unrealizedProfitPercent);
							float lastProfitValue = 0;
							try {
								lastProfitValue = trackingStock.get(symbol);
							} catch (Exception ee) {
								lastProfitValue = 0;
							}
							if (unrealizedProfitPercent > lastProfitValue) {
								System.out.println(
										symbol + " is going up.  New profit percent: " + unrealizedProfitPercent);
								trackingStock.put(symbol, unrealizedProfitPercent);
								sellAlerts.put(symbol, false);
								playSound(goingUpFile);
							} else if (lastProfitValue > 0 && unrealizedProfitPercent < lastProfitValue) {
								// Stock is dropping let's sell
								boolean alertSent = sellAlerts.get(symbol);
								if (alertSent == false) {
									sellAlerts.put(symbol, true);
									System.out.println("Stock " + symbol + " has started going down.  Time to sell for "
											+ unrealizedProfitPercent);
									float originalBuyPrice = Float.parseFloat(originalPrice);
									float currentSellPrice = Float.parseFloat(currentPrice)
											* Integer.parseInt(quantity);
									System.out.println(quantity + " shares were bought for $" + originalBuyPrice
											+ " and will sell for $" + currentSellPrice);
									logger.info(quantity + " shares were bought for $" + originalBuyPrice
											+ " and will sell for $" + currentSellPrice);
									if (!soundPlayed) {
										playSound(audioFile);
										soundPlayed = true;
									}
									System.out.println("Selling stock " + symbol);
									net.jacobpeterson.domain.alpaca.position.Position currentPos = alpacaAPI.getOpenPositionBySymbol(symbol);
									currentPrice = currentPos.getCurrentPrice();
									currentSellPrice = Float.parseFloat(currentPrice) + Integer.parseInt(quantity);
									System.out.println("Checking latest price which is: " + currentSellPrice);
									try {
										// Order aapNewMarketOrder = alpacaAPI.requestNewMarketOrder(symbol,
										// Integer.parseInt(quantity), OrderSide.SELL, OrderTimeInForce.DAY, false,
										// null);
										/*net.jacobpeterson.domain.alpaca.order.Order aaplLimitOrder = alpacaAPI.requestNewLimitOrder(symbol,
												Integer.parseInt(quantity), net.jacobpeterson.alpaca.enums.OrderSide.SELL, net.jacobpeterson.alpaca.enums.OrderTimeInForce.DAY,
												new Double(currentPrice), true);*/
										Order aaplLimitOrder = alpacaAPI.requestNewMarketOrder(symbol, Integer.parseInt(quantity), OrderSide.SELL, OrderTimeInForce.DAY);
										System.out.println("Sent sell order for stock: " + symbol);
									} catch (Exception ee) {
										ee.printStackTrace();
									}
								}
							}
						}
					}
				}
				System.out.println("Total potential profit from selling stocks: " + totalPotentialProfit);
				System.out.println("Sleeping " + sleepInterval + " minutes.");
				Thread.sleep(sleepInMs);
			}
		} catch (Exception e) {
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
				// logger.info("Current time is: " + cal.getTime().toString());
				long openTime = cal1.getTimeInMillis();
				// logger.info("Open time calendar object is: " + cal1.getTime().toString());
				Calendar cal2 = Calendar.getInstance();
				String closeInfo[] = marketClose.split(":");
				String closeHour = closeInfo[0];
				String closeMinute = closeInfo[1];
				cal2.set(Calendar.HOUR_OF_DAY, Integer.parseInt(closeHour));
				cal2.set(Calendar.MINUTE, Integer.parseInt(closeMinute));
				long closeTime = cal2.getTimeInMillis();
				// logger.info("Close time calendar object is: " + cal2.getTime().toString());
				/*
				 * if (currentTime > openTime) logger.info("Current time > open Time"); if
				 * (currentTime < closeTime) logger.info("current time < close time");
				 */
				if (currentTime > openTime && currentTime < closeTime)
					return true;
				return false;
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	private static void playSound(String soundFile) {
		try {
			File f = new File(soundFile);
			AudioInputStream audioIn = AudioSystem.getAudioInputStream(f.toURI().toURL());
			Clip clip = AudioSystem.getClip();
			clip.open(audioIn);
			clip.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
