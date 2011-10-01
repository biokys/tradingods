package cz.tradingods.common;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TimeZone;

import org.apache.log4j.Logger;

import com.dukascopy.api.Period;
import com.dukascopy.api.Unit;

public class PropertyHelper {

	private static Logger log = Logger.getLogger(PropertyHelper.class);

	private static final String BUNDLE = "config";
	
	private static final String BUNDLE_OPTIMIZE = "config_optimizer";

	private static boolean isOptimization = isOptimizerEnabled();

	private static Map<String, String> mapParams = new HashMap<String, String>();
	private static Map<Long, Map<String, String>> mapStrategyNumber = new HashMap<Long, Map<String,String>>();
	private static Map<String, Map<Long, Map<String, String>>> mapStrategy = new HashMap<String, Map<Long, Map<String,String>>>();
	
	private static Date startDate = new Date();

	public static void setOptimizationParams(String strategy, long strategyId, String... values) {
		try {
			for (String value : values) {
				String[] parsed = value.split("\\=");
				mapParams.put(parsed[0], parsed[1]);
			}
			mapStrategyNumber.put(strategyId, mapParams);
			mapStrategy.put(strategy, mapStrategyNumber);
			log.warn("Switching to internal properties for optimization");
		} catch (Exception e) {};
	}

	public static List<String> getInstruments() {
		List<String> list = new ArrayList<String>();
		String[] instruments =  getStringProperty("instruments").split("\\|");
		for (String string : instruments) {
			string = string.substring(0, 3) + "/" +  string.substring(3);
			list.add(string);
		}
		return list;
	}

	public static String getFxApiJNLP() {
		return getStringProperty("api.jnlp");
	}

	public static String getFxApiUsername() {
		return getStringProperty("api.username");
	}

	public static String getFxApiPassword() {
		return getStringProperty("api.password");
	}

	public static String[] getToEmails() {
		return getStringProperty("to.emails").split("\\|");
	}

	public static String getFromEmail() {
		return getStringProperty("from.email");
	}

	public static String getSmtpHostName() {
		return getStringProperty("smtp.hostname");
	}

	public static String getSmtpUsername() {
		return getStringProperty("smtp.username");
	}

	public static String getSmtpPassword() {
		return getStringProperty("smtp.password");
	}

	public static List<Period> getTimeframes(String strategyName) {
		List<Period> periods = new ArrayList<Period>();
		String[] timeframes = getStringProperty(strategyName + ".timeframes").split("\\|");
		for (String string : timeframes) {
			String[] parts = string.split("\\-");
			int num = new Integer(parts[0]).intValue();
			String period = parts[1];
			Period p = Period.createCustomPeriod(Unit.valueOf(period), num);
			periods.add(p);
		}
		return periods;
	}

	public static boolean onHistoricalData() {
		return getBooleanProperty("historical.backtest");
	}
	
	public static boolean isTradingEnabled() {
		return getBooleanProperty("trading");
	}
	
	public static boolean isOptimizerEnabled() {
		return getBooleanProperty("use.optimizer");
	}

	private static SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy") {{setTimeZone(TimeZone.getTimeZone("GMT"));}};;
	private static Date getDate(String str) throws ParseException {
		return sdf.parse(str);
	}
	public static Date[] getHistoricalDataInterval() {
		String strFrom = getStringProperty("historical.backtest.interval.from");
		String strTo = getStringProperty("historical.backtest.interval.to");
		Date[] dates = new Date[2];
		try {
			dates[0] = getDate(strFrom);
			dates[1] = getDate(strTo);
		} catch (ParseException e) {
			log.error("Backtest interval parsing error!");
			System.exit(0);
		}
		return dates;
	}
	
	public static int[] getHistoricalDataHourInterval() {
		String str = getStringProperty("historical.backtest.hour.interval");
		String[] strHours = str.split("\\-");
		int[] result = new int[2];
		result[0] = new Integer(strHours[0]).intValue();
		result[1] = new Integer(strHours[1]).intValue();
		return result;
	}
	
	public static String[] getActiveStrategies() {
		String s = getStringProperty("active.strategies");
		String[] splitString = s.split("\\|");
		return splitString;
	}

	public static int getConfigParam(String strategyName, long strategyId, String paramName) {
		if (isOptimization) {
			 Map<Long,Map<String,String>> mapS = mapStrategy.get(strategyName);
			Map<String, String> map = mapS.get(strategyId);
			return new Integer(map.get(paramName)).intValue();
		} else
			return getIntProperty(strategyName + "." + paramName);
	}
	
	public static List<String> getAllOptimizationParamsForStrategy(String strategyName) {
		List<String> list = new ArrayList<String>();
		ResourceBundle resources = ResourceBundle.getBundle(BUNDLE_OPTIMIZE);
		Enumeration<String> keys = resources.getKeys();
		while (keys.hasMoreElements()) {
			String s = keys.nextElement();
			if (s.startsWith(strategyName))
				list.add(s);
		}
		return list;
	}
	
	public static String getStrategyClassName(String strategyName) {
			return getStringProperty(strategyName + ".class");
	}

	public static boolean sendEmails() {
		return getBooleanProperty("send.email");
	}

	private static String getStringProperty(String key) {
		ResourceBundle resources = ResourceBundle.getBundle(BUNDLE);
		return resources.getString(key);
	}

	private static boolean getBooleanProperty(String key) {
		ResourceBundle resources = ResourceBundle.getBundle(BUNDLE);
		return new Boolean(resources.getString(key)).booleanValue();
	}

	private static int getIntProperty(String key) {
		ResourceBundle resources = ResourceBundle.getBundle(BUNDLE);
		return new Integer(resources.getString(key)).intValue();
	}

	/**
	 * Vraci pole integeru pro zadany klic - pracuje nad konfigurakem pro Optimizer
	 * @param key
	 * @return
	 */
	public static Integer[] getIntPropertyForOptimizer(String key) {
		ResourceBundle resources = ResourceBundle.getBundle(BUNDLE_OPTIMIZE);
		String value = resources.getString(key);
		String[] values = value.split("\\,");
		Integer[] result = new Integer[values.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = new Integer(values[i]).intValue();
		}
		return result;
	}
	
	public static String getReportDir() {
		String s = getStringProperty("report.dir");
		s = s + "/" + startDate.getTime();
		new File(s).mkdir();
		return s;
	}
}

