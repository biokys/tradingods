package cz.tradingods.optimizer;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import com.dukascopy.api.Period;
import com.dukascopy.api.Unit;

public class PropertyHelper {
	
	private static final String BUNDLE = "config";

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
	
	public static List<Period> getTimeframes() {
		List<Period> periods = new ArrayList<Period>();
		String[] timeframes = getStringProperty("timeframes").split("\\|");
		for (String string : timeframes) {
			String[] parts = string.split("\\-");
			int num = new Integer(parts[0]).intValue();
			String period = parts[1];
			periods.add(Period.createCustomPeriod(Unit.valueOf(period), num));
		}
		return periods;
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
	
}

