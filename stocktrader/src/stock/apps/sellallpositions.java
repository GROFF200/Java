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

public class sellallpositions {

	public static void main(final String argv[]) {

		try {
			AlpacaAPI alpacaAPI = new AlpacaAPI();
			ArrayList<Position> pos = alpacaAPI.getOpenPositions();
			ResourceBundle rb = ResourceBundle.getBundle("alpaca");
			String exclude = rb.getString("exclude");
			String excludeList[] = exclude.split(",");
			String executeOrders = rb.getString("executeorders");
			boolean executeOrder = false;
			if (executeOrders.equals("yes"))
				executeOrder = true;
			for (int x = 0; x < pos.size(); x++) {
				Position position = pos.get(x);
				String stockSymbol = position.getSymbol();
				String avgEntryPrice = position.getAvgEntryPrice();
				String changeToday = position.getChangeToday();
				String costBasis = position.getCostBasis();
				String currentPrice = position.getCurrentPrice();
				String exchange = position.getExchange();
				String marketValue = position.getMarketValue();
				String unrealizedProfitLossPercent = position.getUnrealizedPlpc();
				String qty = position.getQty();
				System.out.println("===================================");
				System.out.println("Stock symbol: " + stockSymbol);
				System.out.println("Average Entry Price: " + avgEntryPrice);
				System.out.println("Change Today: " + changeToday);
				System.out.println("Cost Basis: " + costBasis);
				System.out.println("Current Price: " + currentPrice);
				System.out.println("Exchange: " + exchange);
				System.out.println("Market Value: " + marketValue);
				System.out.println("Quantity: " + qty);
				System.out.println("Unrealized Profit Loss Percent: " + unrealizedProfitLossPercent);
				boolean sellStock = true;
				for (int y = 0; y < excludeList.length; y++) {
					String stockName = excludeList[y];
					if (stockSymbol.equals(stockName))
						sellStock = false;
				}
				if (sellStock) {
					System.out.println("**Sending sell order for: " + stockSymbol);
					try {
						if (executeOrder) {
							Order aaplLimitOrder = alpacaAPI.requestNewLimitOrder(stockSymbol, Integer.parseInt(qty),
									OrderSide.SELL, OrderTimeInForce.DAY, new Double(currentPrice), true);
							aaplLimitOrder.getClientOrderId();
						} else {
							System.out.println("Not placing order as that is disabled in configuration file.");
						}
					} catch (AlpacaAPIRequestException ae) {
						System.out.println("Problem selling!");
						ae.printStackTrace();
					}
				} else {
					System.out.println("Not selling stock because it is on exclude list.");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
