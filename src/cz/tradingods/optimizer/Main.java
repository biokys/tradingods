/*
 * Copyright (c) 2009 Dukascopy (Suisse) SA. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * -Redistribution of source code must retain the above copyright notice, this
 *  list of conditions and the following disclaimer.
 * 
 * -Redistribution in binary form must reproduce the above copyright notice, 
 *  this list of conditions and the following disclaimer in the documentation
 *  and/or other materials provided with the distribution.
 * 
 * Neither the name of Dukascopy (Suisse) SA or the names of contributors may 
 * be used to endorse or promote products derived from this software without 
 * specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL 
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING
 * ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. DUKASCOPY (SUISSE) SA ("DUKASCOPY")
 * AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE
 * AS A RESULT OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL DUKASCOPY OR ITS LICENSORS BE LIABLE FOR ANY LOST 
 * REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, 
 * INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY 
 * OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, 
 * EVEN IF DUKASCOPY HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 */
package cz.tradingods.optimizer;

import com.dukascopy.api.system.ISystemListener;
import com.dukascopy.api.system.IClient;
import com.dukascopy.api.system.ClientFactory;
import com.dukascopy.api.system.ITesterClient;
import com.dukascopy.api.system.TesterFactory;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.OfferSide;
import com.dukascopy.api.Period;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.TextArea;
import java.awt.Toolkit;
import java.awt.peer.ComponentPeer;
import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * This small program demonstrates how to initialize Dukascopy client and start a strategy
 */
public class Main {
	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);


	//url of the DEMO jnlp
	private static String jnlpUrl = PropertyHelper.getFxApiJNLP();
	//user name
	private static String userName = PropertyHelper.getFxApiUsername();
	//password
	private static String password = PropertyHelper.getFxApiPassword();


	public static void main(String[] args) throws Exception {
		//get the instance of the IClient interface
		//final IClient client = ClientFactory.getDefaultInstance();
		final ITesterClient client = TesterFactory.getDefaultInstance();

		//set the listener that will receive system events
		client.setSystemListener(new ISystemListener() {
			private int lightReconnects = 3;

			@Override
			public void onStart(long processId) {
				LOGGER.info("Strategy started: " + processId);

			}

			@Override
			public void onStop(long processId) {
				LOGGER.info("Strategy stopped: " + processId);
				Integer[] params = map.get(processId);
				String s = params[0] + "_" + params[1] + "_" + params[2] + "_" + params[3] + "_" + params[4] + "_" + params[5];
				File reportFile;
				if (optimize) {
					reportFile = new File("C:\\trading2\\optimize_" + processId + "__" + s + ".html");
				} else {
					reportFile = new File("C:\\trading2\\test_" + processId + "__" + s + ".html");	
				}
				try {
					client.createReport(processId, reportFile);
				} catch (Exception e) {
					LOGGER.error(e.getMessage(), e);
				}


				if (client.getStartedStrategies().size() == 0) {
					FileHelper.getOptimizedParamsForCertainDays();
					System.exit(0);
				}
			}

			@Override
			public void onConnect() {
				LOGGER.info("Connected");
				lightReconnects = 3;
			}

			@Override
			public void onDisconnect() {
				LOGGER.warn("Disconnected");
				if (lightReconnects > 0) {
					client.reconnect();
					--lightReconnects;
				} else {
					try {
						//sleep for 10 seconds before attempting to reconnect
						Thread.sleep(10000);
					} catch (InterruptedException e) {
						//ignore
					}
					try {
						client.connect(jnlpUrl, userName, password);
					} catch (Exception e) {
						LOGGER.error(e.getMessage(), e);
					}
				}
			}
		});

		LOGGER.info("Connecting...");
		//connect to the server using jnlp, user name and password
		client.connect(jnlpUrl, userName, password);
		//wait for it to connect
		int i = 10; //wait max ten seconds
		while (i > 0 && !client.isConnected()) {
			Thread.sleep(1000);
			i--;
		}
		if (!client.isConnected()) {
			LOGGER.error("Failed to connect Dukascopy servers");
			System.exit(1);
		}



		//subscribe to the instruments
		Set<Instrument> instruments = new HashSet<Instrument>();
		List<String> list = PropertyHelper.getInstruments();
		for (String string : list) {
			Instrument ins = Instrument.fromString(string);
			instruments.add(ins);
		}

		LOGGER.info("Subscribing instruments...");
		client.setSubscribedInstruments(instruments);

		LOGGER.info("Downloading data");
		Future<?> future = client.downloadData(null);
		//wait for downloading to complete
		future.get();

		//start the strategy
		LOGGER.info("Starting strategy");

		if (optimize) {
			for (Date[] fromto : FileHelper.getDays(2011, 6)) {
				runStrategies(client, fromto[0], fromto[1]);
				//break;
			}
		} else {
			Map<Date, Values> mapValues = FileHelper.getValuesForBackTest();
			Iterator<Date> iterator = mapValues.keySet().iterator();
			while(iterator.hasNext()) {
				Date d = iterator.next();
				Values v = mapValues.get(d);
				StrategyWatcher sw = new StrategyWatcher();
				sw.setParams(v.cci, v.ema1, v.ema2, new int[] {v.macdA, v.macdB, v.macdC});
				sw.setFromToDate(new Date[] {d, d});
				dateList.add(d);
				listStrategy.add(sw);

			}

			while (!listStrategy.isEmpty()) {
				if (client.getStartedStrategies().size() < 20) {
					StrategyWatcher s = listStrategy.remove(0);
					Date d = dateList.remove(0);
					client.setDataInterval(Period.TICK, OfferSide.ASK, ITesterClient.InterpolationMethod.OPEN_TICK, d.getTime(), d.getTime() + FileHelper.INTERVAL_IN_MS);
					map.put(client.startStrategy(s), s.getParams());
				}
			}
		}



		//now it's running
	}

	public static List<Date> dateList = new ArrayList<Date>();
	
	public static final boolean optimize = false;

	public static void runStrategies(ITesterClient client, Date fromdate, Date todate) {
		client.setDataInterval(Period.TICK, OfferSide.ASK, ITesterClient.InterpolationMethod.OPEN_TICK, fromdate.getTime(), todate.getTime());
		StrategyWatcher sw;
		for (int cciLoop = cci[0];cciLoop < cci[2]; cciLoop+=cci[1]) {
			for (int ema1Loop = ema1[0];ema1Loop < ema1[2]; ema1Loop+=ema1[1]) {
				for (int ema2Loop = ema2[0];ema2Loop < ema2[2]; ema2Loop+=ema2[1]) {
					for (int macdALoop = macdA[0];macdALoop < macdA[2]; macdALoop+=macdA[1]) {
						for (int macdBLoop = macdB[0];macdBLoop < macdB[2]; macdBLoop+=macdB[1]) {
							for (int macdCLoop = macdC[0];macdCLoop < macdC[2]; macdCLoop+=macdC[1]) {
								sw = new StrategyWatcher();
								sw.setParams(cciLoop, ema1Loop, ema2Loop, new int[] {macdALoop, macdBLoop, macdCLoop});
								sw.setFromToDate(new Date[] {fromdate, todate});
								listStrategy.add(sw);
							}	
						}	
					}
				}	
			}	
		}

		while (!listStrategy.isEmpty()) {
			if (client.getStartedStrategies().size() < 20) {
				StrategyWatcher s = listStrategy.remove(0);
				map.put(client.startStrategy(s), s.getParams());
			}
		}



	}



	public static int[] cci = new int[] {10, 5, 20};
	public static int[] ema1 = new int[] {8, 5, 15};
	public static int[] ema2 = new int[] {15, 10, 30};
	public static int[] macdA = new int[] {5, 3, 6};
	public static int[] macdB = new int[] {25, 5, 28};
	public static int[] macdC = new int[] {5, 3, 6};

	static List<StrategyWatcher> listStrategy = new ArrayList<StrategyWatcher>();
	static Map<Long, Integer[]> map = new HashMap<Long, Integer[]>();

}
