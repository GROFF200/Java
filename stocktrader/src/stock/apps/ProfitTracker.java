package stock.apps;

import java.io.BufferedReader;
import java.io.FileReader;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.HashMap;
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

/**
 * Reads data from a text file, then figures out dividend income and adds it to
 * recorded profits
 * 
 * @author aarond
 *
 */
public class ProfitTracker {

	public static void main(final String argv[]) {

		try {
			ArrayList<String> ticket = new ArrayList<String>();
			ArrayList<String> profitList = new ArrayList<String>();
			HashMap<String, String> shareCount = new HashMap<String, String>();
			HashMap<String, String> divPercent = new HashMap<String, String>();
			ResourceBundle rb = ResourceBundle.getBundle("alpaca");
			String inputFile = rb.getString("profitinputfile");
			AlpacaAPI alpacaAPI = new AlpacaAPI();
			ArrayList<Position> pos = alpacaAPI.getOpenPositions();
			BufferedReader fin = new BufferedReader(new FileReader(inputFile));
			String line = "";
			boolean otherProfits = false;
			while ((line = fin.readLine()) != null) {
				if (line.indexOf("Other Profits") == -1 && otherProfits == false) {
					String info[] = line.split(",");
					String shareName = info[0];
					String shareCountStr = info[1];
					String dividendPercent = info[2];
					ticket.add(shareName);
					shareCount.put(shareName, shareCountStr);
					divPercent.put(shareName, dividendPercent);
				} else if (line.indexOf("Other Profits") != -1)
					otherProfits = true;
				if (otherProfits && line.indexOf("#") == -1) {
					profitList.add(line.trim());
				}
			}
			fin.close();
			float divProfitTotal = 0; 
			for (int x=0; x<pos.size(); x++) {
				Position position = pos.get(x);
				String currentPrice = position.getCurrentPrice();
				String stockSymbol = position.getSymbol();
				String currentShareCount = shareCount.get(stockSymbol);	
				if (currentShareCount != null) {
					int shareCountInt = Integer.parseInt(currentShareCount);
					System.out.println("Found entry for stock: "+stockSymbol);
					String divPercentStr = divPercent.get(stockSymbol);
					System.out.println("Dividend percent is: "+divPercentStr);
					if (divPercentStr != null) {
						float fCurrentPrice = new Float(currentPrice).floatValue();
						float divPercentage = new Float(divPercentStr).floatValue();
						BigDecimal percentYield = new BigDecimal(fCurrentPrice * (divPercentage/100));
						System.out.println("Percent yield per share is: "+percentYield);
						BigDecimal totalYield = new BigDecimal(percentYield.floatValue() * (float)shareCountInt);
						float percentYieldTotal = totalYield.floatValue();
						System.out.println("Total yield for all shares each payout: "+percentYieldTotal);
						//Assume a pay out quarterly
						divProfitTotal += percentYieldTotal;
						System.out.println("Total payout for the year: "+divProfitTotal);
					}
				}
			}
			System.out.println("Total dividend yield for year is estimated to be: "+divProfitTotal);
			float totalOtherProfit = 0;
	        for (int x=0; x<profitList.size(); x++) {
	        	String profitVal = profitList.get(x);
	        	float profitFloat = new Float(profitVal).floatValue();
	        	totalOtherProfit += profitFloat;
	        }
	        System.out.println("Total other profit is: "+totalOtherProfit);
	        System.out.println("Total profit is: "+(totalOtherProfit + divProfitTotal));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
