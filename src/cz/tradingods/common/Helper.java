package cz.tradingods.common;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;



public class Helper {

	public static final long INTERVAL_IN_MS = 86400000;

	private static Logger log = Logger.getLogger(Helper.class);

	public static List<Date[]> getDays(int year) { 
		return getDays(year, -1);
	}

	public static List<Date[]> getDays(Date from, Date to) { 
		List<Date[]> list = new ArrayList<Date[]>();
		Date[] fromto;
		Calendar c = Calendar.getInstance();
		c.setTime(from);
		Calendar cTo = Calendar.getInstance();
		cTo.setTime(to);
		fromto = new Date[2];
		fromto[0] = c.getTime();
		fromto[1] = cTo.getTime();
		list.add(fromto);
		return list;
		/*while (c.before(cTo)) {
			if (c.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY || c.get(Calendar.DAY_OF_WEEK) == Calendar.WEDNESDAY) {
				Date lFrom = c.getTime();
				Calendar lCal = Calendar.getInstance();
				lCal.setTime(lFrom);
				lCal.add(Calendar.DAY_OF_YEAR, 2);
				Date lTo = lCal.getTime();
				fromto = new Date[2];
				fromto[0] = lFrom;
				fromto[1] = lTo;
				list.add(fromto);
			}
			c.add(Calendar.DAY_OF_YEAR, 1);
		}
		return list;*/
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
				c.add(Calendar.DAY_OF_YEAR, 2);
				Date to = c.getTime();
				//Date to = new Date(from.getTime() + (INTERVAL_IN_MS * 2));
				fromto = new Date[2];
				fromto[0] = from;
				fromto[1] = to;
				list.add(fromto);
			}
		}
		return list;
	}

	public static void createFinalFile(String strategyName, Map<Date, HelpClass> m) {
		new File(PropertyHelper.getReportDir() + "/" + strategyName).mkdir();
		String filename = PropertyHelper.getReportDir() + "/" + strategyName+ "/final";
		try { 
			BufferedWriter out = new BufferedWriter(new FileWriter(filename, true));
			Iterator<Date> it = m.keySet().iterator();
			while (it.hasNext()) {
				Date d = it.next();
				HelpClass h = m.get(d);
				out.write( d.getTime() + ";" + h.readableParams + "\n");				
			}

			out.close(); 
		} catch (IOException e) { 
			e.printStackTrace();
		}
	}

	private static void createFile(Date[] fromto, String strategyName, String readableParams, double profit, String ext) {
		new File(PropertyHelper.getReportDir() + "/" + strategyName).mkdir();
		String filename = PropertyHelper.getReportDir() + "/" + strategyName+ "/" + fromto[0].getTime() + "_" + fromto[1].getTime() + "." + ext;
		try { 
			BufferedWriter out = new BufferedWriter(new FileWriter(filename, true)); 
			out.write(profit + ";" + readableParams + "\n"); 
			out.close(); 
		} catch (IOException e) { 
			e.printStackTrace();
		}
	}

	public static void appendOptimizationToFile(Date[] fromto, String strategyName, String readableParams, double profit) {
		createFile(fromto, strategyName, readableParams, profit, "proc");
	}

	public static void finalOptimizationToFile(Date[] fromto, String strategyName, String readableParams, double profit) {
		createFile(fromto, strategyName, readableParams, profit, "optimize");
	}

	/**
	 * prohleda jednotlive proc soubory a najde tu nejvic profitabilni strategii a tu pak ulozi do .optimize souboru
	 * @param strategyName
	 */
	public static void findMaxProfitParams(String strategyName) {
		File[] files = new File(PropertyHelper.getReportDir() + "/" + strategyName).listFiles(new ProcFileFilter("proc"));
		for (File file : files) {
			if (!file.isFile())
				continue;
			double maxProfit = Double.NEGATIVE_INFINITY;
			String readableParams = null;
			try{
				FileInputStream fstream = new FileInputStream(file);
				DataInputStream in = new DataInputStream(fstream);
				BufferedReader br = new BufferedReader(new InputStreamReader(in));
				String strLine;
				while ((strLine = br.readLine()) != null)   {
					String[] strings = strLine.split(";");
					double profit = new Double(strings[0]).doubleValue();
					if (profit > maxProfit) {
						maxProfit = profit;
						readableParams = strings[1];
					}
				}

				String[] filename = file.getName().split("\\.")[0].split("_");
				Date[] fromto = new Date[2];
				fromto[0] = new Date(new Long(filename[0]).longValue());
				fromto[1] = new Date(new Long(filename[1]).longValue());
				finalOptimizationToFile(fromto, strategyName, readableParams, maxProfit);
				in.close();
			}catch (Exception e){
				System.err.println("Error: " + e.getMessage());
			}
		}
	}

	public static Map<Date, HelpClass> getValuesForBackTest(String strategyName) {
		Map<Date, HelpClass> map = new HashMap<Date, HelpClass>();
		File[] files = new File(PropertyHelper.getReportDir() + "/" + strategyName).listFiles(new ProcFileFilter("optimize"));
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
					String[] strings = strLine.split("\\;");
					HelpClass h = new HelpClass();
					h.readableParams = strings[1];
					h.strategyName = strategyName;
					map.put(todate, h);
				}
			} catch (Exception e){
				System.err.println("Error: " + e.getMessage());
			}
		}
		return map;
	}

	private static Integer[] getAllIntegers(Integer[] input) {
		if (input.length != 3) {
			log.warn("bad number of params! allowed are 3! exiting..." );
			System.exit(0);
		}
		if (input[1] < 0) {
			log.warn("middle parameter must be greater or equal to 0!");
			System.exit(0);
		}
		if (input[0] >= input[2]) {
			log.warn("first parameter must be less than third parameter!");
			System.exit(0);
		}
		if (input[1] == 0)
			return new Integer[]  {input[0]};
		
		List<Integer> list = new ArrayList<Integer>();
		for (int i=input[0];i<=input[2];i+=input[1])  {
			list.add(i);
		}
		Integer[] l = new Integer[list.size()];
		l =  list.toArray(l);
		return l;
	}

	/**
	 * Vraci seznam vsech kombinaci pro zadanou strategy
	 * @param strategyName jmeno strategie
	 * @return  {key=cci, value=1;key=ema, value=13}
	 * 			{key=cci, value=2;key=ema, value=13}
	 * 			{key=cci, value=1;key=ema, value=14}
	 * 			atd... 
	 * 
	 */
	public static List<Map<String, Integer>> getParametersCombination(String strategyName) {
		Map<String, Integer> map;
		List<Map<String, Integer>> list = new ArrayList<Map<String,Integer>>();
		// nactu z konfigu seznam vsech klicu
		List<String> keys = PropertyHelper.getAllOptimizationParamsForStrategy(strategyName);
		// naplnim si mapu konfiguracnich dat ex. {key=cci,value=3,4,5},{key=macd_1, value=10,2,20}
		Set[] sets = new Set[keys.size()];
		//sets[0] = ImmutableSet.of(keys);
		int loop = 0;
		for (String string : keys) {
			Integer[] values = PropertyHelper.getIntPropertyForOptimizer(string);
			Integer[] ilist = getAllIntegers(values);
			sets[loop++] = ImmutableSet.of(ilist);
		}
		List<String> reducedKeys = new ArrayList<String>();
		for (int i=0;i<keys.size();i++) {
			String s = keys.get(i);
			s = s.split("\\.")[1];
			reducedKeys.add(s);
		}

		Set s = Sets.cartesianProduct(sets);
		System.out.println(s.size());
		Iterator it = s.iterator();
		loop = 0;
		while (it.hasNext()) {
			ImmutableList il = (ImmutableList)it.next();
			Object[] o = il.toArray();
			map = new  HashMap<String, Integer>();
			for (int i=0;i<reducedKeys.size();i++) {
				map.put(reducedKeys.get(i), (Integer)o[i]);
			}
			list.add(map);
		}
		return list;
		/*map = new  HashMap<String, Integer>();
		map.put("pt", 40);
		map.put("sl", 30);
		map.put("emaslow", 15);
		map.put("emafast", 5);
		map.put("cci", 13);
		map.put("macd_1", 5);
		map.put("macd_2", 40);
		map.put("macd_3", 5);
		list.add(map);

		map = new  HashMap<String, Integer>();
		map.put("pt", 50);
		map.put("sl", 20);
		map.put("emaslow", 13);
		map.put("emafast", 3);
		map.put("cci", 16);
		map.put("macd_1", 5);
		map.put("macd_2", 35);
		map.put("macd_3", 5);
		list.add(map);*/
	}

	public static void prepareFileSystem() {
		deleteDirectory(new File(PropertyHelper.getReportDir()));
		new File(PropertyHelper.getReportDir()).mkdir();
	}

	private static boolean deleteDirectory(File path) {
		if( path.exists() ) {
			File[] files = path.listFiles();
			for(int i=0; i<files.length; i++) {
				if(files[i].isDirectory()) {
					deleteDirectory(files[i]);
				}
				else {
					files[i].delete();
				}
			}
		}
		return( path.delete() );
	}

	private static class ProcFileFilter implements FileFilter {

		private String ext;

		public ProcFileFilter(String ext) {
			this.ext = ext;
		}
		public boolean accept(File file) {
			if (file.getName().toLowerCase().endsWith(ext))
				return true;
			else
				return false;
		}
	}

}
