package stock.market;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;

import net.jacobpeterson.alpaca.*;
import net.jacobpeterson.alpaca.enums.BarsTimeFrame;
import net.jacobpeterson.alpaca.enums.OrderSide;
import net.jacobpeterson.alpaca.enums.OrderTimeInForce;
import net.jacobpeterson.alpaca.rest.exception.AlpacaAPIRequestException;
import net.jacobpeterson.domain.alpaca.account.Account;
import net.jacobpeterson.domain.alpaca.marketdata.Bar;
import net.jacobpeterson.domain.alpaca.order.Order;
import net.jacobpeterson.domain.alpaca.position.Position;

public class SellPositions {

	private static org.apache.logging.log4j.Logger logger = LogManager.getLogger(SellPositions.class);

	public boolean sellProfitablePositions() {

		try {
			AlpacaAPI alpacaAPI = new AlpacaAPI();
			ArrayList<Position> pos = alpacaAPI.getOpenPositions();
			if (pos.size() == 0) {
				logger.info("No open positions currently");
				return true;
			} else {
				for (int x = 0; x < pos.size(); x++) {
					Position p = pos.get(x);
					String symbol = p.getSymbol();	
					logger.info("Looking at current profit for " + symbol);
					String profitPercent = p.getUnrealizedPlpc();
					float fProfitPercent = Float.parseFloat(profitPercent);
					float fFinalPercent = fProfitPercent * 100;
					//We need at least .5% profit
					if (fFinalPercent > .5) {
						String qty = p.getQty();
						int quantity = Integer.parseInt(qty);
						String currentPriceStr = p.getCurrentPrice();
						float fCurrentPrice = Float.parseFloat(currentPriceStr);
						logger.info("Selling " + quantity + " of stock " + symbol + " at $" + fCurrentPrice
								+ " per share.");
						Order aaplLimitOrder = alpacaAPI.requestNewLimitOrder(symbol, quantity, OrderSide.SELL,
								OrderTimeInForce.DAY, new Double(fCurrentPrice), true);
					} else if (fFinalPercent <= -10) {
						logger.info("We have lost at least 10% on this stock.  Selling and cuttig our losses");
						String qty = p.getQty();
						int quantity = Integer.parseInt(qty);
						String currentPriceStr = p.getCurrentPrice();
						float fCurrentPrice = Float.parseFloat(currentPriceStr);
						logger.info("Selling " + quantity + " of stock " + symbol + " at $" + fCurrentPrice
								+ " per share.");
						Order aaplLimitOrder = alpacaAPI.requestNewLimitOrder(symbol, quantity, OrderSide.SELL,
								OrderTimeInForce.DAY, new Double(fCurrentPrice), true);
					}
				}
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
}
