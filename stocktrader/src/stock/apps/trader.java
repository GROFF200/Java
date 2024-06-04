package stock.apps;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import net.jacobpeterson.alpaca.*;
import net.jacobpeterson.alpaca.enums.BarsTimeFrame;
import net.jacobpeterson.alpaca.enums.OrderSide;
import net.jacobpeterson.alpaca.enums.OrderTimeInForce;
import net.jacobpeterson.alpaca.rest.exception.AlpacaAPIRequestException;
import net.jacobpeterson.alpaca.websocket.broker.listener.AlpacaStreamListenerAdapter;
import net.jacobpeterson.alpaca.websocket.broker.message.AlpacaStreamMessageType;
import net.jacobpeterson.domain.alpaca.account.Account;
import net.jacobpeterson.domain.alpaca.marketdata.Bar;
import net.jacobpeterson.domain.alpaca.order.Order;
import net.jacobpeterson.domain.alpaca.position.Position;
import net.jacobpeterson.domain.alpaca.streaming.AlpacaStreamMessage;
import net.jacobpeterson.domain.alpaca.streaming.account.AccountUpdateMessage;
import net.jacobpeterson.domain.alpaca.streaming.trade.TradeUpdateMessage;
import net.jacobpeterson.domain.alpaca.watchlist.Watchlist;

public class trader {

	public static void main(final String argv[]) {
		
		try {
			AlpacaAPI alpacaAPI = new AlpacaAPI();
			alpacaAPI.addAlpacaStreamListener(new AlpacaStreamListenerAdapter(
			        AlpacaStreamMessageType.ACCOUNT_UPDATES,
			        AlpacaStreamMessageType.TRADE_UPDATES) {
			    @Override
			    public void onStreamUpdate(AlpacaStreamMessageType streamMessageType, AlpacaStreamMessage streamMessage) {
			        switch (streamMessageType) {
			            case ACCOUNT_UPDATES:
			                AccountUpdateMessage accountUpdateMessage = (AccountUpdateMessage) streamMessage;
			                Date date = new Date();
			                System.out.println("Update Received At: "+date.toString());
			                System.out.println("\nReceived Account Update: \n\t" +
			                        accountUpdateMessage.toString().replace(",", ",\n\t"));
			                break;
			            case TRADE_UPDATES:
			                TradeUpdateMessage tradeUpdateMessage = (TradeUpdateMessage) streamMessage;
			                date = null;
			                date = new Date();
			                System.out.println("Update Received At: "+date.toString());
			                System.out.println("\nReceived Order Update: \n\t" +
			                        tradeUpdateMessage.toString().replace(",", ",\n\t"));
			                break;
			        }
			    }
			});
			
			// Get Account Information
			try {
			    Account alpacaAccount = alpacaAPI.getAccount();
			    System.out.println("\n\nAccount Information:");
			    System.out.println("\t" + alpacaAccount.toString().replace(",", ",\n\t"));
			} catch (AlpacaAPIRequestException e) {
			    e.printStackTrace();
			}

			ArrayList<Position> pos = alpacaAPI.getOpenPositions();
			
			// Request an Order
			try {
			    Order aaplLimitOrder = alpacaAPI.requestNewLimitOrder("AAPL", 1, OrderSide.BUY, OrderTimeInForce.DAY,
			            201.30, true);

			    System.out.println("\n\nNew AAPL Order:");
			    System.out.println("\t" + aaplLimitOrder.toString().replace(",", ",\n\t"));
			} catch (AlpacaAPIRequestException e) {
			    e.printStackTrace();
			}

			// Create watchlist
			try {
			    Watchlist dayTradeWatchlist = alpacaAPI.createWatchlist("Day Trade", "AAPL");

			    System.out.println("\n\nDay Trade Watchlist:");
			    System.out.println("\t" + dayTradeWatchlist.toString().replace(",", ",\n\t"));
			} catch (AlpacaAPIRequestException e) {
			    e.printStackTrace();
			}

			// Get bars
			try {
			    ZonedDateTime start = ZonedDateTime.of(2019, 11, 18, 0, 0, 0, 0, ZoneId.of("America/Chicago"));
			    ZonedDateTime end = ZonedDateTime.of(2019, 11, 22, 23, 59, 0, 0, ZoneId.of("America/Chicago"));

			    Map<String, ArrayList<Bar>> bars = alpacaAPI.getBars(BarsTimeFrame.ONE_DAY, "AAPL", null, start, end,
			            null, null);

			    System.out.println("\n\nBars response:");
			    for (Bar bar : bars.get("AAPL")) {
			        System.out.println("\t==========");
			        System.out.println("\tUnix Time " + ZonedDateTime.ofInstant(Instant.ofEpochSecond(bar.getT()),
			                ZoneOffset.UTC));
			        System.out.println("\tOpen: $" + bar.getO());
			        System.out.println("\tHigh: $" + bar.getH());
			        System.out.println("\tLow: $" + bar.getL());
			        System.out.println("\tClose: $" + bar.getC());
			        System.out.println("\tVolume: " + bar.getV());
			    }
			} catch (AlpacaAPIRequestException e) {
			    e.printStackTrace();
			}

			// Keep the Alpaca websocket stream open for 5 seconds
			try {
			    Thread.sleep(5000);
			} catch (InterruptedException e) {
			    e.printStackTrace();
			}
		} catch (Exception e) { e.printStackTrace(); }
	}
}
