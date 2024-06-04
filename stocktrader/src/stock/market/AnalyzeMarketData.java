package stock.market;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;

import stock.util.SortByPrice;
import stock.util.stockproperties;

public class AnalyzeMarketData {

	private static org.apache.logging.log4j.Logger logger = LogManager.getLogger(AnalyzeMarketData.class);
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
	
	public static ArrayList<String> getStocksToBuy() {
		return stocksToBuy;
	}
	public static void setStocksToBuy(ArrayList<String> stocksToBuy) {
		AnalyzeMarketData.stocksToBuy = stocksToBuy;
	}
	
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

	/**
	 * For this strategy, we want to find a stock that has dropped in price but has a high trading volume.
	 * We hope this indicates it will grow again soon.
	 */
	public static void analyzeStockBasedOnLowPriceAndVolume(final String fileName, final ArrayList<stockproperties> props,
                                                           final String algorithmDays) {
		
		try {
			int numDays = Integer.parseInt(algorithmDays);
			String fileNameInfo[] = fileName.split("\\.");
			String stockName = fileNameInfo[0];
			logger.info("======================= Analyzing "+stockName+" ====================");
			BigDecimal priceAverage = new BigDecimal("0");
			BigDecimal volumeAverage = new BigDecimal("0");
			BigDecimal totalPrice = new BigDecimal("0");
			BigDecimal totalVolume = new BigDecimal("0");
			int numCounter = numDays;
			if (numDays > props.size()) numCounter = props.size();
			boolean trending = false;
			String lastPrice = "";
			float priceDiff = 0;
			logger.info("Analyzing "+numCounter+" days of market data.");
			for (int x = 0; x < numCounter; x++) {
				stockproperties stock = props.get(x);
				logger.info("Looking at stock "+stockName);
				String closePrice = stock.getClosePrice();
				if (lastPrice != null && lastPrice.length() > 0) {
					float price1 = Float.parseFloat(closePrice);
					float price2 = Float.parseFloat(lastPrice);
					priceDiff = price2-price1;
					if (priceDiff < 0) {
						logger.info("Stock is dropping.  Pricediff is: "+priceDiff);
						String lastVolume = stock.getVolume();
						float fVolume = Float.parseFloat(lastVolume);
						if (fVolume > 500000) {
							//We want high volume
							logger.info("Volume is high.  Let's buy this one.");
							trending = true;
						}
					} else {
						logger.info("Stock is growing.  Pricediff is: "+priceDiff);
						trending = false;
					}
					
				}
				lastPrice = closePrice;
			}
			logger.info("======================= Analyzing "+stockName+" ====================");
			if (trending) {
				logger.info("Stock "+stockName+" is growing in price.  Adding to buy list.");
				stocksToBuy.add(new Float(priceDiff).toString()+"^" + stockName + "^" + lastPrice.toString());
			} else {
				stocksToSell.add(new Float(priceDiff).toString()+"^" + stockName + "^" + lastPrice.toString());
			}
			Collections.reverse(stocksToBuy);
		}  catch (Exception e) { e.printStackTrace(); }
	}
	
	public static void analyzeStockBasedOnTrends(final String fileName, final ArrayList<stockproperties> props,
			                                     final String algorithmDays) {

		try {
			int numDays = Integer.parseInt(algorithmDays);
			String fileNameInfo[] = fileName.split("\\.");
			String stockName = fileNameInfo[0];
			logger.info("======================= Analyzing "+stockName+" ====================");
			BigDecimal priceAverage = new BigDecimal("0");
			BigDecimal volumeAverage = new BigDecimal("0");
			BigDecimal totalPrice = new BigDecimal("0");
			BigDecimal totalVolume = new BigDecimal("0");
			int numCounter = numDays;
			if (numDays > props.size()) numCounter = props.size();
			boolean trending = false;
			String lastPrice = "";
			float priceDiff = 0;
			logger.info("Analyzing "+numCounter+" days of market data.");
			for (int x = 0; x < numCounter; x++) {
				stockproperties stock = props.get(x);
				logger.info("Looking at stock "+stockName);
				String closePrice = stock.getClosePrice();
				if (lastPrice != null && lastPrice.length() > 0) {
					float price1 = Float.parseFloat(closePrice);
					float price2 = Float.parseFloat(lastPrice);
					priceDiff = price2-price1;
					if (priceDiff > 0) {
						logger.info("Stock is trending.  Pricediff is: "+priceDiff);
						trending = true;
					} else {
						trending = false;
						break;
					}
					
				}
				lastPrice = closePrice;
			}
			logger.info("======================= Analyzing "+stockName+" ====================");
			if (trending) {
				logger.info("Stock "+stockName+" is growing in price.  Adding to buy list.");
				stocksToBuy.add(new Float(priceDiff).toString()+"^" + stockName + "^" + lastPrice.toString());
			} else {
				stocksToSell.add(new Float(priceDiff).toString()+"^" + stockName + "^" + lastPrice.toString());
			}
			Collections.reverse(stocksToBuy);
		} catch (Exception e) {
			e.printStackTrace();
		}

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
	
	/**
	 * Figure out which stocks are going up.   Get a top 10 list of them, then
	 * write that to a text file.
	 */
    public static boolean analyzeStocks() {
    	
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
				//analyzeStockBasedOnTrends(fileToRead, stockProps, algorithmDays);
				analyzeStockBasedOnLowPriceAndVolume(fileToRead, stockProps, algorithmDays);
				Collections.sort(stocksToBuy);
				Collections.reverse(stocksToBuy);
			}
			logger.info("Top 10 stocks to buy:");
			int size = 10;
			if (stocksToBuy.size() < 10)
				size = stocksToBuy.size();
			for (int x = 0; x < size; x++) {
				logger.info(stocksToBuy.get(x));
			}
			return true;
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	return false;
    }
}
