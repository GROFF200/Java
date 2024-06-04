package stock.util;

import java.util.Comparator;

public class SortByPrice implements Comparator<String> {

	@Override
	public int compare(String a, String b) {
		try {
		     String info1[] = a.split("\\^");
		     String price1 = info1[0];
		     String info2[] = b.split("\\^");
		     String price2 = info2[0];
		     float fPrice1 = Float.parseFloat(price1);
		     float fPrice2 = Float.parseFloat(price2);
		     float fResult = fPrice1 - fPrice2;
		     return new Float(fResult).intValue();
		} catch (Exception e) { e.printStackTrace(); }
		return 0;
	}

}
