package cz.tradingods.signaler;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.dukascopy.api.Instrument;
import com.dukascopy.api.Period;

public class ValueContainer {

	public static class IndicatorValues {
		public double[] emaSlow;
		public double[] emaFast;
		public double[] macd;
		public double cci;

	}
	
	private static Map<Integer, Date> md = new HashMap<Integer, Date>();
	private static Map<Integer, Map<Integer, Date>> dateMap;
	
	private static Map<Integer, Integer> mi = new HashMap<Integer, Integer>();
	private static Map<Integer, Map<Integer, Integer>> typeMap;
	
	static {
		dateMap = new HashMap<Integer, Map<Integer,Date>>();
		typeMap = new HashMap<Integer, Map<Integer,Integer>>();
	}
	public void putIndicatorValues(Period period, Instrument instrument, IndicatorValues value) {
	
	}
	
	public IndicatorValues getIndicatorValues(Period period, Instrument instrument) {
		return null;
	}
	
	public static void putCrossoverDate(Period period, Instrument instrument, Date date) {
		md.put(instrument.ordinal(), date);
		dateMap.put(period.ordinal(), md);
	}
	
	public static Date getCrossoverDate(Period period, Instrument instrument) {
		if (dateMap == null)
			return null;
		Map<Integer, Date> m = dateMap.get(period.ordinal());
		if (m == null)
			return null;
		return m.get(instrument.ordinal());
	}
	
	public static void putLastCrossoverType(Period period, Instrument instrument, int type) {
		mi.put(instrument.ordinal(), type);
		typeMap.put(period.ordinal(), mi);
	}
	
	public static int getLastCrossoverType(Period period, Instrument instrument) {
		if (typeMap == null)
			return 0;
		Map<Integer, Integer> m = typeMap.get(period.ordinal());
		if (m == null)
			return 0;
		return m.get(instrument.ordinal());
	}

}