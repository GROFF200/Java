package stock.apps;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.ResourceBundle;

import stock.util.SortByPrice;
import stock.util.stockproperties;

public class marketanalyzer {

	private static ArrayList<String> stocksToBuy = new ArrayList<String>();
	private static ArrayList<String> stocksToSell = new ArrayList<String>();
	// If the price difference between the beginning and end dates is greater than
	// this, suggest selling
	private static int buyDifferenceThreshold = 1;
	// If the price difference between the beginning and end dates is greater than
	// this suggest buying
	private static int sellDifferenceThreshold = -1;
	// Check if the volume trading is below this before suggesting selling
	private static int sellVolumeThreshold = -100000;
	// Make sure volume difference is greater than this before buying
	private static int buyVolumeThreshold = 10000;

	private static ArrayList<stockproperties> readFile(final String location, final String fileName) {

		try {
			// System.out.println("Reading file: " + fileName);
			ArrayList<stockproperties> values = new ArrayList<stockproperties>();
			BufferedReader fin = new BufferedReader(new FileReader(location + fileName));
			String line = "";
			while ((line = fin.readLine()) != null) {
				if (line.charAt(0) != '#') {
					stockproperties props = new stockproperties();
					String lineInfo[] = line.split("\\,");
					String openPrice = lineInfo[1];
					String highPrice = lineInfo[2];
					String lowPrice = lineInfo[3];
					String closePrice = lineInfo[4];
					String volume = lineInfo[5];
					props.setOpenPrice(openPrice);
					props.setHighPrice(highPrice);
					props.setLowPrice(lowPrice);
					props.setClosePrice(closePrice);
					props.setVolume(volume);
					values.add(props);
				}
			}
			fin.close();
			return values;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void analyzeStockBasedOnTrends(final String fileName, final ArrayList<stockproperties> props,
			                                     final String algorithmDays) {

		try {
			int numDays = Integer.parseInt(algorithmDays);
			String fileNameInfo[] = fileName.split("\\.");
			String stockName = fileNameInfo[0];
			System.out.println("======================= Analyzing "+stockName+" ====================");
			BigDecimal priceAverage = new BigDecimal("0");
			BigDecimal volumeAverage = new BigDecimal("0");
			BigDecimal totalPrice = new BigDecimal("0");
			BigDecimal totalVolume = new BigDecimal("0");
			int numCounter = numDays;
			if (numDays > props.size()) numCounter = props.size();
			boolean trending = false;
			String lastPrice = "";
			float priceDiff = 0;
			for (int x = 0; x < numCounter; x++) {
				stockproperties stock = props.get(x);
				String closePrice = stock.getClosePrice();
				if (lastPrice != null && lastPrice.length() > 0) {
					float price1 = Float.parseFloat(closePrice);
					float price2 = Float.parseFloat(lastPrice);
					priceDiff = price1-price2;
					if (price1 > price2) {
						System.out.println("Stock is trending as last price was "+lastPrice+" and close price is "+closePrice);
						trending = true;					   
					} else {
						System.out.println("Stock is NOT trending as last price was "+lastPrice+" and close price is "+closePrice);
						trending = false;
						break;
					}
				}
				lastPrice = closePrice;
			}
			System.out.println("======================= Analyzing "+stockName+" ====================");
			if (trending) {
				System.out.println("Stock "+stockName+" is growing in price.  Adding to buy list.");
				stocksToBuy.add(new Float(priceDiff).toString()+"^" + stockName + "^" + lastPrice.toString());
			} else {
				stocksToSell.add(new Float(priceDiff).toString()+"^" + stockName + "^" + lastPrice.toString());
			}
			Collections.reverse(stocksToBuy);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	public static void analyzeStockBasedOnAverages(final String fileName, final ArrayList<stockproperties> props) {

		try {
			System.out.println("StockProperties size: "+props.size());
			String fileNameInfo[] = fileName.split("\\.");
			String stockName = fileNameInfo[0];
			BigDecimal priceAverage = new BigDecimal("0");
			BigDecimal volumeAverage = new BigDecimal("0");
			BigDecimal totalPrice = new BigDecimal("0");
			BigDecimal totalVolume = new BigDecimal("0");
			int avgDiv = props.size();
			for (int x = 0; x < props.size(); x++) {
				stockproperties stock = props.get(x);
				String closePrice = stock.getClosePrice();
				String volume = stock.getVolume();
				/*System.out.println("ClosePrice: "+closePrice);
				System.out.println("Volume: "+volume);*/
				totalPrice = totalPrice.add(new BigDecimal(closePrice));
				totalVolume = totalVolume.add(new BigDecimal(volume));
				/*System.out.println("New ClosePrice: "+totalPrice);
				System.out.println("New Volume: "+totalVolume);*/
			}
			System.out.println("Dividing totalPrice("+totalPrice+") by ("+avgDiv+")");
			if (avgDiv == 0) {
				System.out.println("*** Can't divide by 0.  Abandoning this calculation.");
				return;
			}
			MathContext mc = new MathContext(2, RoundingMode.HALF_UP);
			priceAverage = totalPrice.divide(new BigDecimal(avgDiv), mc);
			volumeAverage = totalVolume.divide(new BigDecimal(avgDiv), mc);
			stockproperties lastStock = props.get(props.size() - 1);
			String finalPrice = lastStock.getClosePrice();
			String finalVolume = lastStock.getVolume();
			BigDecimal lastPrice = new BigDecimal(finalPrice);
			BigDecimal lastVolume = new BigDecimal(finalVolume);
			float lPrice = lastPrice.floatValue();
			float lVolume = lastPrice.floatValue();
			float pAverage = priceAverage.floatValue();
			float pVolume = volumeAverage.floatValue();
			float priceDiff = pAverage - lPrice;
			System.out.println("======================");
			System.out.println("STOCK: "+stockName);
			System.out.println("Average Price: "+pAverage);
			System.out.println("Average Volume: "+pVolume);
			System.out.println("Last Price: "+lastPrice);
			System.out.println("Last Volume: "+lVolume);
			System.out.println("======================");
			if (lPrice < pAverage && lVolume < pVolume && priceDiff <= 2) {
				stocksToBuy.add(new Float(priceDiff).toString()+"^" + stockName + "^" + lastPrice.toString());
			} else {
				stocksToSell.add(new Float(priceDiff).toString()+"^" + stockName + "^" + lastPrice.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static boolean analyzeStock(final String fileName, final ArrayList<stockproperties> props) {

		try {
			// Analyze the history and determine whether this is one we should buy, or one
			// we should sell. We're going to look at prices and volumes from the first day
			// to the last day.
			String fileNameInfo[] = fileName.split("\\.");
			String stockName = fileNameInfo[0];
			stockproperties stockInfo = props.get(0);
			String closePrice = stockInfo.getClosePrice();
			BigDecimal bdClosePrice = new BigDecimal(closePrice);
			String highPrice = stockInfo.getHighPrice();
			BigDecimal bdHighPrice = new BigDecimal(highPrice);
			String lowPrice = stockInfo.getLowPrice();
			BigDecimal bdLowPrice = new BigDecimal(lowPrice);
			String openPrice = stockInfo.getOpenPrice();
			BigDecimal bdOpenPrice = new BigDecimal(openPrice);
			String volume = stockInfo.getVolume();
			BigDecimal bdVolume = new BigDecimal(volume);
			stockproperties endStockInfo = props.get(props.size() - 1);
			String endClosePrice = endStockInfo.getClosePrice();
			BigDecimal bdEndClosePrice = new BigDecimal(endClosePrice);
			String endHighPrice = endStockInfo.getHighPrice();
			BigDecimal bdEndHighPrice = new BigDecimal(endHighPrice);
			String endLowPrice = endStockInfo.getLowPrice();
			BigDecimal bdEndLowPrice = new BigDecimal(endLowPrice);
			String endOpenPrice = endStockInfo.getOpenPrice();
			BigDecimal bdEndOpenPrice = new BigDecimal(endOpenPrice);
			String endVolume = endStockInfo.getVolume();
			BigDecimal bdEndVolume = new BigDecimal(endVolume);
			// Now let's compare beginning and ending values
			BigDecimal priceDiff = bdEndClosePrice.subtract(bdClosePrice);
			float priceDifference = priceDiff.floatValue();
			BigDecimal volumeDiff = bdEndVolume.subtract(bdVolume);
			float volumeDifference = volumeDiff.floatValue();
			if (priceDifference > buyDifferenceThreshold) {
				// System.out.println("Stock "+stockName+" has grown in price by
				// "+priceDifference+". Should sell.");
				if (volumeDifference < sellVolumeThreshold) {
					/*
					 * System.out.println( "Stock " + stockName + " has increased in price by " +
					 * priceDifference + ".   Should sell.");
					 * System.out.println("Volume difference is: " + volumeDiff);
					 */
					stocksToSell.add(priceDifference + "^" + stockName + "^" + endClosePrice);
				}
			}
			if (priceDifference < sellDifferenceThreshold) {
				if (volumeDifference > buyVolumeThreshold) {
					/*
					 * System.out.println( "Stock " + stockName + " has dropped in price by " +
					 * priceDifference + ".   Should buy.");
					 * System.out.println("Volume difference is: " + volumeDiff);
					 */
					stocksToBuy.add(priceDifference + "^" + stockName + "^" + endClosePrice);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public static void sortByPrice() {

		try {
			ArrayList<String> sortedStocks = new ArrayList<String>();
			for (int x = 0; x < stocksToBuy.size(); x++) {
				String stockInfo = stocksToBuy.get(x);
				String info[] = stockInfo.split("\\^");
				String priceDiff = info[0];
				String stockName = info[1];
				String closePrice = info[2];
				sortedStocks.add(closePrice + "^" + stockName + "^" + priceDiff);
			}
			Collections.sort(sortedStocks, new SortByPrice());
			stocksToBuy.clear();
			for (int x = 0; x < sortedStocks.size(); x++) {
				String stockInfo = sortedStocks.get(x);
				String info[] = stockInfo.split("\\^");
				String closePrice = info[0];
				String stockName = info[1];
				String priceDiff = info[2];
				stocksToBuy.add(priceDiff + "^" + stockName + "^" + closePrice);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(final String argv[]) {

		try {
			ResourceBundle rb = ResourceBundle.getBundle("alpaca");
			String algo = rb.getString("algorithm");
			String algorithmDays = rb.getString("algorithmdays");
			String fileLocation = rb.getString("marketdatalocation");
			// First we need to read the market data and put it into a format we can analyze
			File file = new File(fileLocation);
			String fileName[] = file.list();
			ArrayList<stockproperties> toSell = new ArrayList<stockproperties>();
			ArrayList<stockproperties> toBuy = new ArrayList<stockproperties>();
			for (int x = 0; x < fileName.length; x++) {
				String fileToRead = fileName[x];
				ArrayList<stockproperties> stockProps = readFile(fileLocation, fileToRead);
				if (algo.equals("1")) {
					System.out.println("** Using algorithm 1, based on highest/lowest price and volume");
					analyzeStock(fileToRead, stockProps);
				}
				else if (algo.equals("2")) {
					System.out.println("** Using algorithm 2, based on average price and volume");
					analyzeStockBasedOnAverages(fileToRead, stockProps);
				}
				else if (algo.equals("3")) {
					System.out.println("** Using algorithm 3, based on rising stock prices.");
					analyzeStockBasedOnTrends(fileToRead, stockProps, algorithmDays);
				}
				Collections.sort(stocksToSell);
				Collections.reverse(stocksToSell);
				Collections.sort(stocksToBuy);
				Collections.reverse(stocksToBuy);
			}
			System.out.println("Top 10 stocks to sell:");
			int size = 10;
			if (stocksToSell.size() < 10)
				size = stocksToSell.size();
			for (int x = 0; x < size; x++) {
				System.out.println(stocksToSell.get(x));
			}
			System.out.println("Top 10 stocks to buy:");
			size = 10;
			if (stocksToBuy.size() < 10)
				size = stocksToBuy.size();
			for (int x = 0; x < size; x++) {
				System.out.println(stocksToBuy.get(x));
			}
			String writeLocation = rb.getString("tradedatalocation");
			PrintWriter fpw = new PrintWriter(new FileOutputStream(writeLocation + "sell.txt"));
			for (int x = 0; x < stocksToSell.size(); x++) {
				fpw.println(stocksToSell.get(x));
			}
			fpw.close();
			fpw = null;
			fpw = new PrintWriter(new FileOutputStream(writeLocation + "buy.txt"));
			// Was wanting to sort by purchase price. Holding off on this as previous algo
			// seemed to make profit
			sortByPrice();
			for (int x = 0; x < stocksToBuy.size(); x++) {
				fpw.println(stocksToBuy.get(x));
			}
			fpw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
