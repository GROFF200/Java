package stock.apps;

import java.util.ArrayList;
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
 * Figure out what stocks we have and sell them for profit if possible.
 * 
 * @author aarond
 *
 */
public class getpositiondata {

	public static void main(final String argv[]) {

		try {
			ResourceBundle rb = ResourceBundle.getBundle("alpaca");
			String percentProfitMargin = rb.getString("percentprofitmargin");
			String marketMaker = rb.getString("marketmakersell");
			String marketPercent = rb.getString("marketpercent");
			String sellForAnyProfit = rb.getString("sellforanyprofit");
			String excludeList = rb.getString("exclude");
			String doNotSell[] = excludeList.split(",");
			boolean marketMakerEnabled = false;
			if (marketMaker.equals("yes"))
				marketMakerEnabled = true;
			float fMarketPercent = 0;
			fMarketPercent = Float.parseFloat(marketPercent);
			boolean sellForAny = false;
			if (sellForAnyProfit.equals("yes"))
				sellForAny = true;
			//System.out.println("The profit margin required to sell is: " + percentProfitMargin);
			String executeOrders = rb.getString("executeorders");
			boolean executeOrder = false;
			if (executeOrders.equals("yes"))
				executeOrder = true;
			float percentProfit = Float.parseFloat(percentProfitMargin);
			net.jacobpeterson.alpaca.AlpacaAPI alpacaAPI = new net.jacobpeterson.alpaca.AlpacaAPI();
			ArrayList<net.jacobpeterson.domain.alpaca.position.Position> pos = alpacaAPI.getOpenPositions();
			/*for (int x = 0; x < pos.size(); x++) {
				Position position = pos.get(x);
				String stockSymbol = position.getSymbol();
				String avgEntryPrice = position.getAvgEntryPrice();
				String changeToday = position.getChangeToday();
				String costBasis = position.getCostBasis();
				String currentPrice = position.getCurrentPrice();
				String exchange = position.getExchange();
				String marketValue = position.getMarketValue();
				String unrealizedProfitLossPercent = position.getUnrealizedPlpc();
				float unrealizedProfitPercent = Float.parseFloat(unrealizedProfitLossPercent);
				unrealizedProfitPercent *= 100;
				String qty = position.getQty();
				// Now make sure this isn't a stock we aren't supposed to sell
				boolean canSell = true;
				for (int y = 0; y < doNotSell.length; y++) {
					String checkSymbol = doNotSell[y];
					if (checkSymbol.equals(stockSymbol)) {
						canSell = false;
					}
				}
				float entryPrice = Float.parseFloat(avgEntryPrice);
				float currentStockPrice = Float.parseFloat(currentPrice);
				if (currentStockPrice > entryPrice && canSell) {
					float buyPrice = entryPrice * Integer.parseInt(qty);
					float sellPrice = currentStockPrice * Integer.parseInt(qty);
					float diff = sellPrice - buyPrice;
					if (unrealizedProfitPercent >= percentProfit) {
						float salePrice = Float.parseFloat(currentPrice);
						if (marketMakerEnabled) {
							System.out.println("Market maker strategy enabled.  Adding " + fMarketPercent
									+ " to the purchase price");
							float priceAdd = salePrice * fMarketPercent;
							salePrice += priceAdd;
							System.out.println("Documented sale price is:" + currentPrice + " and we are selling for "
									+ salePrice);
						}
						if (executeOrder) {
							System.out.println("\n!!! We are within the " + percentProfit
									+ "% profit margin.  Sending sell order for: " + stockSymbol
									+ " with potential profit of " + diff);
							Order aaplLimitOrder = alpacaAPI.requestNewLimitOrder(stockSymbol, Integer.parseInt(qty),
									OrderSide.SELL, OrderTimeInForce.DAY, new Double(salePrice), true, null);
						} else {
							System.out.println("Not executing order, this is disabled.");
						}
					} else if (sellForAny && unrealizedProfitPercent > 0 && canSell) {
						System.out.println("\n!!! Configured to sell for any profit.  Sending sell order for: "
								+ stockSymbol + " with potential profit of " + diff);
						try {
							if (executeOrder) {
								Order aaplLimitOrder = alpacaAPI.requestNewLimitOrder(stockSymbol,
										Integer.parseInt(qty), OrderSide.SELL, OrderTimeInForce.DAY,
										new Double(currentPrice), true, null);
								System.out.println("Order Id from sale is: " + aaplLimitOrder.getClientOrderId());
							} else {
								System.out.println("Not executing order, disabled.");
							}
						} catch (AlpacaAPIRequestException ai) {
							ai.printStackTrace();
							System.out.println("Unable to sell!  Exception with API.");
						}
					}
				}
			}*/
			net.jacobpeterson.domain.alpaca.account.Account account = alpacaAPI.getAccount();
			String accountNumber = account.getAccountNumber();
			String buyingPower = account.getBuyingPower();
			String cash = account.getCash();
			String initialMargin = account.getInitialMargin();
			String portfolioValue = account.getPortfolioValue();
			System.out.println("============ Account Information =======================");
			System.out.println("Account Number: " + accountNumber);
			System.out.println("Buying Power: " + buyingPower);
			System.out.println("Cash: " + cash);
			System.out.println("Initial Margin: " + initialMargin);
			System.out.println("Portfolio Value: " + portfolioValue);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
