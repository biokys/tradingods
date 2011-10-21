package cz.tradingods.common;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dukascopy.api.Instrument;
import com.dukascopy.api.Period;

public class ValueContainer {

	private static Map<Integer, Map<Integer, Date>> dateMap;

	private static Map<Integer, Map<Integer, Integer>> typeMap;
	
	private static class DataWithDate {
		
		Period period;
		Instrument instrument;
		Date object;
		
		public DataWithDate(Period period, Instrument instrument, Date object) {
			this.period = period;
			this.instrument = instrument;
			this.object = object;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (((DataWithDate)obj).period == this.period && ((DataWithDate)obj).instrument == this.instrument)
				return true;
			return false;
		}
	}
	
	private static class DataWithInteger {
		
		Period period;
		Instrument instrument;
		int object;
		
		public DataWithInteger(Period period, Instrument instrument, int object) {
			this.period = period;
			this.instrument = instrument;
			this.object = object;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (((DataWithInteger)obj).period == this.period && ((DataWithInteger)obj).instrument == this.instrument)
				return true;
			return false;
		}
	}

	private List<DataWithDate> listDate;
	private List<DataWithInteger> listInteger;

	public ValueContainer() {
		listDate = new ArrayList<ValueContainer.DataWithDate>();
		listInteger = new ArrayList<ValueContainer.DataWithInteger>();
		dateMap = new HashMap<Integer, Map<Integer,Date>>();
		typeMap = new HashMap<Integer, Map<Integer,Integer>>();
	}
	
	private void insertDate(DataWithDate data) {
		for (DataWithDate d : listDate) {
			if (d.equals(data))  {
				d.object = data.object;
				return;
			}
		}
		listDate.add(data);
	}
	
	private void insertInteger(DataWithInteger data) {
		for (DataWithInteger d : listInteger) {
			if (d.equals(data))  {
				d.object = data.object;
				return;
			}
		}
		listInteger.add(data);
	}
	
	private Date getDate(Period period, Instrument instrument) {
		DataWithDate d = new DataWithDate(period, instrument, null);
		for (DataWithDate data : listDate) {
			if (d.equals(data))
				return data.object;
		}
		return null;
	}

	private int getInteger(Period period, Instrument instrument) {
		DataWithInteger d = new DataWithInteger(period, instrument, IndicatorHelper.CROSSOVER_NONE);
		for (DataWithInteger data : listInteger) {
			if (d.equals(data))
				return data.object;
		}
		return 0;
	}
	
	public void putCrossoverDate(Period period, Instrument instrument, Date date) {
		insertDate(new DataWithDate(period, instrument, date));
		 /*Map<Integer, Date> md = new HashMap<Integer, Date>(); 
		 md.put(instrument.ordinal(), date);
		dateMap.put(period.ordinal(), md);*/
	}

	public Date getCrossoverDate(Period period, Instrument instrument) {
		return getDate(period, instrument);
		/*if (dateMap == null)
			return null;
		Map<Integer, Date> m = dateMap.get(period.ordinal());
		if (m == null)
			return null;
		return m.get(instrument.ordinal());*/
	}

	public void putLastCrossoverType(Period period, Instrument instrument, int type) {
		insertInteger(new DataWithInteger(period, instrument, type));
		/*Map<Integer, Integer> mi = new HashMap<Integer, Integer>();
		mi.put(instrument.ordinal(), type);
		typeMap.put(period.ordinal(), mi);*/
	}

	public int getLastCrossoverType(Period period, Instrument instrument) {
		return getInteger(period, instrument);
		/*if (typeMap == null)
			return 0;
		Map<Integer, Integer> m = typeMap.get(period.ordinal());
		if (m == null)
			return 0;
		return m.get(instrument.ordinal());*/
	}

}