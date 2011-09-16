package cz.tradingods.optimizer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileHelper {

	public static final long INTERVAL_IN_MS = 86400000;

	public static List<Date[]> getDays(int year) { 
		return getDays(year, -1);
	}
	
	public static List<Date[]> getDays(int year, int month) { 
		List<Date[]> list = new ArrayList<Date[]>();
		Date[] fromto;
		Calendar c = Calendar.getInstance();
		c.clear();
		c.set(year, month, 0);
		int days;
		if (month >= 0)
			 days = c.getActualMaximum(Calendar.DAY_OF_MONTH);
		else
			days = c.getActualMaximum(Calendar.DAY_OF_YEAR);
		for (int i = 0;i < days;i++) {
			c.add(Calendar.DAY_OF_YEAR, 1);
			if (c.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY || c.get(Calendar.DAY_OF_WEEK) == Calendar.WEDNESDAY) {
				Date from = c.getTime();
				Date to = new Date(from.getTime() + (INTERVAL_IN_MS * 2));
				fromto = new Date[2];
				fromto[0] = from;
				fromto[1] = to;
				list.add(fromto);
			}
		}
		return list;
	}

	public static final String DIR = "C:\\trading";

	public static void appendOptimizationToFile(Date[] fromto, Values v, double profit, boolean append) {
		String filename = DIR + "\\" + fromto[0].getTime() + "_" + fromto[1].getTime() + ".txt";
		try { 
			BufferedWriter out = new BufferedWriter(new FileWriter(filename, append)); 
			out.write(profit + "," + v.cci + "," + v.ema1 + "," + v.ema2 + "," + v.macdA + "," + v.macdB + "," + v.macdC + "\n"); 
			out.close(); 
			} catch (IOException e) { 
				e.printStackTrace();
			} 
	}

	public static void getOptimizedParamsForCertainDays() {
		File[] files = new File(DIR).listFiles();
		for (File file : files) {
			if (!file.isFile())
				continue;
			double maxProfit = Double.MIN_VALUE;
			Values v = new Values();
			try{
				FileInputStream fstream = new FileInputStream(file);
				DataInputStream in = new DataInputStream(fstream);
				BufferedReader br = new BufferedReader(new InputStreamReader(in));
				String strLine;
				while ((strLine = br.readLine()) != null)   {
					String[] strings = strLine.split(",");
					double profit = new Double(strings[0]).doubleValue();
					if (profit > maxProfit) {
						maxProfit = profit;
						v.cci = new Integer(strings[1]).intValue();
						v.ema1 = new Integer(strings[2]).intValue();
						v.ema2 = new Integer(strings[3]).intValue();
						v.macdA = new Integer(strings[4]).intValue();
						v.macdB = new Integer(strings[5]).intValue();
						v.macdC = new Integer(strings[6]).intValue();
					}
				}
				
				String[] filename = file.getName().split("\\.")[0].split("_");
				Date[] fromto = new Date[2];
				fromto[0] = new Date(new Long(filename[0]).longValue());
				fromto[1] = new Date(new Long(filename[1]).longValue());
				appendOptimizationToFile(fromto, v, maxProfit, false);
				in.close();
			}catch (Exception e){
				System.err.println("Error: " + e.getMessage());
			}
		}
	}
	
	public static Map<Date, Values> getValuesForBackTest() {
		Map<Date, Values> map = new HashMap<Date, Values>();
		Values v;
		File[] files = new File(DIR).listFiles();
		for (File file : files) {
			if (!file.isFile())
				continue;

			String todateStr = file.getName().split("\\.")[0].split("_")[1];
			Date todate = new Date(new Long(todateStr).longValue());

			try {
				FileInputStream fstream = new FileInputStream(file);
				DataInputStream in = new DataInputStream(fstream);
				BufferedReader br = new BufferedReader(new InputStreamReader(in));
				String strLine;
				while ((strLine = br.readLine()) != null)   {
					String[] strings = strLine.split(",");
					v = new Values();
					v.cci = new Integer(strings[1]).intValue();
					v.ema1 = new Integer(strings[2]).intValue();
					v.ema2 = new Integer(strings[3]).intValue();
					v.macdA = new Integer(strings[4]).intValue();
					v.macdB = new Integer(strings[5]).intValue();
					v.macdC = new Integer(strings[6]).intValue();
					map.put(todate, v);
				}
			} catch (Exception e){
				System.err.println("Error: " + e.getMessage());
			}
		}
		return map;
	}
	
}
