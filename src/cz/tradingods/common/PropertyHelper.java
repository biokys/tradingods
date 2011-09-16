package cz.tradingods.common;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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

	private static boolean isOptimization = false;

	private static Map<String, String> mapParams = new HashMap<String, String>();

	public static void setOptimizationParams(String... values) {
		try {
			for (String value : values) {
				String[] parsed = value.split("\\=");
				mapParams.put(parsed[0], parsed[1]);
			}
			isOptimization = true;
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
			/*if (!(Period.isPeriodCompliant(p))) {
				log.warn(p.name() + " neni povoleny TF. Povolene jsou: TICK, TEN_SECS, ONE_MIN, FIVE_MINS, TEN_MINS, FIFTEEN_MINS, THIRTY_MINS, ONE_HOUR, FOUR_HOURS, DAILY, WEEKLY, MONTHLY");
				continue;
			}*/
			periods.add(p);
		}
		return periods;
	}

	public static boolean onHistoricalData() {
		return getBooleanProperty("historical.backtest");
	}

	private static SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm") {{setTimeZone(TimeZone.getTimeZone("GMT"));}};;
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

	public static int[] getCustomParams(String strategyName, String paramName) {
		String[] splitString;
		if (isOptimization) {
			splitString = mapParams.get(paramName).split("\\,");
		} else {
			String s = getStringProperty(strategyName + "." + paramName);
			splitString = s.split("\\,");
		}
		int[] result = new int[splitString.length];
		for (int i = 0;i< splitString.length;i++) {
			result[i] = new Integer(splitString[i]).intValue();
		}
		return result;
	}

	public static int getCustomParam(String strategyName, String paramName) {
		if (isOptimization)
			return new Integer(mapParams.get(paramName)).intValue();
		else
			return getIntProperty(strategyName + "." + paramName);
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

}

