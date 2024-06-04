package stock.apps;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

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
import stock.util.order;

public class tradelistener {

	private static HashMap<String, order> orderTracker = new HashMap<String, order>();

	public static order parseData(final String tradeData) {

		try {
			order orderData = new order();
			Calendar cal = Calendar.getInstance();
			String info[] = tradeData.split(",");
			String orderNumber = info[6];
			String symbol = info[15];
			String orderStatus = info[25];
			System.out.println("Order status: " + orderStatus);
			System.out.println("Order symbol: " + symbol);
			orderData.setTimeInMs(new Long(cal.getTimeInMillis()).toString());
			String orderNumberInfo[] = orderNumber.split("=");
			System.out.println("Order number: " + orderNumberInfo[1]);
			orderData.setClientOrderId(orderNumberInfo[1]);
			orderData.setSymbol(symbol);
			orderData.setStatus(orderStatus);
			return orderData;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void main(final String argv[]) {

		try {
			AlpacaAPI alpacaAPI = new AlpacaAPI();
			alpacaAPI.addAlpacaStreamListener(new AlpacaStreamListenerAdapter(AlpacaStreamMessageType.ACCOUNT_UPDATES,
					AlpacaStreamMessageType.TRADE_UPDATES) {
				@Override
				public void onStreamUpdate(AlpacaStreamMessageType streamMessageType,
						AlpacaStreamMessage streamMessage) {
					switch (streamMessageType) {
					case ACCOUNT_UPDATES:
						AccountUpdateMessage accountUpdateMessage = (AccountUpdateMessage) streamMessage;
						System.out.println("\nReceived Account Update: \n\t"
								+ accountUpdateMessage.toString().replace(",", ",\n\t"));
						break;
					case TRADE_UPDATES:
						TradeUpdateMessage tradeUpdateMessage = (TradeUpdateMessage) streamMessage;
						/*
						 * System.out.println("\nReceived Order Update: \n\t" +
						 * tradeUpdateMessage.toString().replace(",", ",\n\t"));
						 */
						order orderInfo = parseData(tradeUpdateMessage.toString());
						String status = orderInfo.getStatus();
						status = status.trim();
						if (status.contains("canceled")) {
							System.out.println("Order was canceled, removing it from tracker.");
							synchronized (orderTracker) {
								orderTracker.remove(orderInfo.getClientOrderId());
							}
						} else if (status.contains("filled")) {
							System.out.println("Order was filled, removing it from tracker.");
							synchronized (orderTracker) {
								orderTracker.remove(orderInfo.getClientOrderId());
							}
						} else {
							System.out.println("Order status is: " + status + ".  Adding to tracker.");
							synchronized (orderTracker) {
								orderTracker.put(orderInfo.getClientOrderId(), orderInfo);
							}
						}
						break;
					}
				}
			});
			while (1 == 1) {
				Thread.sleep(2000);
				System.out.println("=========   ORDER TRACKING UPDATES =========");
				// Check and see if we have any orders that haven't been filled for more than 15
				// minutes.
				// If so, send a request to cancel them.
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.MINUTE, -15);
				long timeCheck = cal.getTimeInMillis();
				Set<String> s = orderTracker.keySet();
				Iterator<String> i = s.iterator();
				while (i.hasNext()) {
					String key = null;
					synchronized (orderTracker) {
						key = i.next();
					}
					order orderInfo = orderTracker.get(key);
					System.out.println("Checking order " + orderInfo.getClientOrderId());
					String timeInMs = orderInfo.getTimeInMs();
					Calendar checkCal = Calendar.getInstance();
					checkCal.setTimeInMillis(Long.parseLong(timeInMs));
					System.out.println("Order placed at: " + checkCal.getTime().toString());
					long storedTime = Long.parseLong(timeInMs);
					long diffTime = timeCheck - storedTime;
					System.out.println("DiffTime: " + diffTime);
					if (diffTime > 0) {
						synchronized (orderTracker) {
							System.out.println("Sending request to cancel this order: " + orderInfo.getClientOrderId());
							try {
								alpacaAPI.cancelOrder(orderInfo.getClientOrderId().trim());
							} catch (AlpacaAPIRequestException ae) {
								ae.printStackTrace();
								System.out.println("Exception canceling order! Removing this one from queue.");
								try {
									synchronized (orderTracker) {
										orderTracker.remove(key);
									}
								} catch (ConcurrentModificationException ce) {
									ce.printStackTrace();
								}
							}
						}
					}
				}
				System.out.println("=============================================");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
