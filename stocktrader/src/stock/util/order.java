package stock.util;

public class order {

	private String clientOrderId;
	
	public String getClientOrderId() {
		return clientOrderId;
	}

	public void setClientOrderId(String clientOrderId) {
		this.clientOrderId = clientOrderId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	private String status;
	
	private String timeInMs;

	public String getTimeInMs() {
		return timeInMs;
	}

	public void setTimeInMs(String timeInMs) {
		this.timeInMs = timeInMs;
	}
	
	public String symbol;

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}
}
