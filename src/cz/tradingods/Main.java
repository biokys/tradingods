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
package cz.tradingods;

import com.dukascopy.api.system.ISystemListener;
import com.dukascopy.api.system.IClient;
import com.dukascopy.api.system.ClientFactory;
import com.dukascopy.api.system.ITesterClient;
import com.dukascopy.api.system.TesterFactory;
import com.dukascopy.api.IStrategy;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.OfferSide;
import com.dukascopy.api.Period;

import cz.tradingods.common.HelpClass;
import cz.tradingods.common.Helper;
import cz.tradingods.common.PropertyHelper;
import cz.tradingods.signaler.strategies.MyStrategy;
import cz.tradingods.signaler.strategies.VladoStrategy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class Main {
	private static final Logger log = LoggerFactory.getLogger(Main.class);

	public static final boolean HISTORICAL_DATA = PropertyHelper.onHistoricalData();
	public static final boolean TRADING_ENABLED = PropertyHelper.isTradingEnabled();
	public static final boolean OPTIMIZER_ENABLED = PropertyHelper.isOptimizerEnabled();
	private static final Date[] HISTORICAL_DATA_INTERVAL = PropertyHelper.getHistoricalDataInterval();

	//url of the DEMO jnlp
	private static String jnlpUrl = PropertyHelper.getFxApiJNLP();
	//user name
	private static String userName = PropertyHelper.getFxApiUsername();
	//password
	private static String password = PropertyHelper.getFxApiPassword();

	private static List<MyStrategy> strategyPool = new ArrayList<MyStrategy>();

	private static Map<Long, HelpClass> bindIdMap = new HashMap<Long, HelpClass>();


	public static void main(String[] args) throws Exception {
		log.info("Started on " + new Date().toString());
		final IClient client;

		if (!HISTORICAL_DATA) {
			client = ClientFactory.getDefaultInstance();
		} else {
			client = TesterFactory.getDefaultInstance();
		}

		client.setSystemListener(new ISystemListener() {
			private int lightReconnects = 3;

			@Override
			public void onStart(long processId) {
				log.info("Strategy started: " + processId);
			}


			@Override
			public void onStop(long processId) {
				log.info("Strategy stopped: " + processId);
				try {
					if (client instanceof ITesterClient) {
						if (TRADING_ENABLED) {
							HelpClass h = bindIdMap.get(processId);
							new File(PropertyHelper.getReportDir() + "/" + h.strategyName).mkdir();
							File f;
							if (OPTIMIZER_ENABLED) {
								f = new File(PropertyHelper.getReportDir() + "/" + h.strategyName + "/" + processId + "-optimize-" + h.readableParams + ".html");
							} else {
								f = new File(PropertyHelper.getReportDir() + "/" + h.strategyName + "/" + processId + "-backtest.html");
							}
							((ITesterClient)client).createReport(processId, f);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (client.getStartedStrategies().size() == 0) {
					if (OPTIMIZER_ENABLED) {
						for (String strategyName : PropertyHelper.getActiveStrategies()) {
							Helper.findMaxProfitParams(strategyName);
						}

						for (String strategyName : PropertyHelper.getActiveStrategies()) {
							Map<Date, HelpClass> m = Helper.getValuesForBackTest(strategyName);
							Helper.createFinalFile(strategyName, m);
						}
					}
					System.exit(0);
				}
			}

			@Override
			public void onConnect() {
				log.info("Connected");
				lightReconnects = 3;
			}

			@Override
			public void onDisconnect() {
				log.warn("Disconnected");
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
						log.error(e.getMessage(), e);
					}
				}
			}
		});

		log.info("Connecting...");
		//connect to the server using jnlp, user name and password
		client.connect(jnlpUrl, userName, password);
		//wait for it to connect
		int i = 10; //wait max ten seconds
		while (i > 0 && !client.isConnected()) {
			Thread.sleep(1000);
			i--;
		}
		if (!client.isConnected()) {
			log.error("Failed to connect Dukascopy servers");
			System.exit(1);
		}

		Set<Instrument> instruments = new HashSet<Instrument>();
		List<String> list = PropertyHelper.getInstruments();
		for (String string : list) {
			Instrument ins = Instrument.fromString(string);
			instruments.add(ins);
		}

		log.info("Subscribing instruments...");
		client.setSubscribedInstruments(instruments);

		if (HISTORICAL_DATA) {
			Future<?> future = ((ITesterClient)client).downloadData(null);
			future.get();
		}

		// pouze signaler
		if (!OPTIMIZER_ENABLED) {
			if (HISTORICAL_DATA) {
				log.info("Downloading data");
				((ITesterClient)client).setDataInterval(Period.TICK, OfferSide.BID, ITesterClient.InterpolationMethod.OPEN_TICK, HISTORICAL_DATA_INTERVAL[0].getTime(), HISTORICAL_DATA_INTERVAL[1].getTime());
			}
			String[] strategies = PropertyHelper.getActiveStrategies();
			for (String strategy : strategies) {
				String clazz = PropertyHelper.getStrategyClassName(strategy);
				MyStrategy strategyInstance = (MyStrategy)Class.forName(clazz).newInstance();
				// nejdrive nastavime jmeno strategie
				strategyInstance.setStrategyName(strategy);
				// pak nechame instanci aby si natahla parametry
				strategyInstance.setParams();
				log.info("Starting signaler,  strategy " + strategy);
				long l = client.startStrategy(strategyInstance);
				HelpClass h = new HelpClass();
				h.readableParams = strategyInstance.readableParams;
				h.strategyName = strategyInstance.getStrategyName();
				bindIdMap.put(l, h);
			}
		} else {
			// optimizer
			if (!HISTORICAL_DATA) {
				log.warn("you have to enable historical.backtest");
				System.exit(0);
			}

			if (!TRADING_ENABLED) {
				log.warn("you have to enable trading");
				System.exit(0);
			}
			// nacteme strategie
			String[] strategies = PropertyHelper.getActiveStrategies();
			// nacteme datumy od kdy do kdy testovat
			Date[] dInterval = PropertyHelper.getHistoricalDataInterval();
			// vygenerujeme jednotlive casove useky (PO, UT) a (ST, CT)
			List<Date[]> dates = Helper.getDays(dInterval[0], dInterval[1]);
			// prochazime po jednotlivych casovych usecich
			for (Date[] date : dates) {
				log.info("Downloading data for date " + date[0] + " - " + date[1]);
				((ITesterClient)client).setDataInterval(Period.TICK, OfferSide.BID, ITesterClient.InterpolationMethod.OPEN_TICK, date[0].getTime(), date[1].getTime());

				// prochazime pres vsechny registrovane strategie
				for (String strategyName : strategies) {
					// zjistime nazev tridy abychom ji mohli instanciovat
					String clazz = PropertyHelper.getStrategyClassName(strategyName);
					// najdeme vsechny kombinace parametru pro strategii "strategy", podle config_optimizer.properties
					List<Map<String, Integer>> combinationMap = Helper.getParametersCombination(strategyName);
					// budeme prochazet polozky mapy, nastavovat dle nich parametry a spoustet jednotlive strategie
					for (Map<String, Integer> map : combinationMap) {
						// vytvorime pole Stringu velke jako mapa
						String[] params = new String[map.size()];
						String readableParams = "";
						int var=0;
						Iterator<String> it = map.keySet().iterator();
						while(it.hasNext()) {
							String key = it.next();
							Integer param = map.get(key);
							// postupne sestavujeme pole parametru ex. "emaslow=13", "emafast=3", "cci=13", "macd=10,23,10", "pt=80", "sl=30"
							readableParams+= (params[var++] = key + "=" + param) + ",";
						}
						readableParams = readableParams.substring(0, readableParams.length() - 1);
						// vytvorim si instanci strategie podle nazvu tridy
						MyStrategy strategyInstance = (MyStrategy)Class.forName(clazz).newInstance();
						// teto instanci poslu jeji jmeno a dostanu zpatky jeji unikatni id
						long strategyId = strategyInstance.setStrategyName(strategyName);
						// nastavim hodnoty parametru pres nazev strategie a jeji unikatni id
						PropertyHelper.setOptimizationParams(strategyName, strategyId, params);
						// nakonec ve vytvorene instanci nechame nasetovat parametry
						strategyInstance.setParams();
						strategyInstance.readableParams = readableParams;
						strategyInstance.dates = date;
						log.info("Starting optimizer,  strategy " + strategyName);
						log.info("params --> " + readableParams);
						// pridame instanci do poolu instanci
						strategyPool.add(strategyInstance);
					}
				}

				while (!strategyPool.isEmpty()) {
					if (client.getStartedStrategies().size() < 20) {
						MyStrategy s = strategyPool.remove(0);
						long l = client.startStrategy(s);
						HelpClass h = new HelpClass();
						h.readableParams = s.readableParams;
						h.strategyName = s.getStrategyName();
						bindIdMap.put(l, h);
					}
				}
			}
		}
	}

}


