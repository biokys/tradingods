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
package cz.tradingods.signaler;

import com.dukascopy.api.system.ISystemListener;
import com.dukascopy.api.system.IClient;
import com.dukascopy.api.system.ClientFactory;
import com.dukascopy.api.system.ITesterClient;
import com.dukascopy.api.system.TesterFactory;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.OfferSide;
import com.dukascopy.api.Period;

import cz.tradingods.common.PropertyHelper;
import cz.tradingods.optimizer.FileHelper;
import cz.tradingods.signaler.strategies.VladoStrategy;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * This small program demonstrates how to initialize Dukascopy client and start a strategy
 */
public class Main {
	private static final Logger log = LoggerFactory.getLogger(Main.class);

	public static final boolean HISTORICAL_DATA = PropertyHelper.onHistoricalData();
	public static final boolean TRADING_ENABLED = true;
	private static final Date[] HISTORICAL_DATA_INTERVAL = PropertyHelper.getHistoricalDataInterval();
	
	//url of the DEMO jnlp
	private static String jnlpUrl = PropertyHelper.getFxApiJNLP();
	//user name
	private static String userName = PropertyHelper.getFxApiUsername();
	//password
	private static String password = PropertyHelper.getFxApiPassword();


	public static void main(String[] args) throws Exception {
		log.info("Signaler started on " + new Date().toString());
		final IClient client;

		if (!HISTORICAL_DATA) {
			client = ClientFactory.getDefaultInstance();
		} else {
			client = TesterFactory.getDefaultInstance();
		}
		PropertyHelper.setOptimizationParams("emaslow=13", "emafast=3", "cci=13", "macd=10,23,10", "pt=80", "sl=30");
		//set the listener that will receive system events
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
					((ITesterClient)client).createReport(processId, new File("report.html"));
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (client.getStartedStrategies().size() == 0) {
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



		//subscribe to the instruments
		Set<Instrument> instruments = new HashSet<Instrument>();
		List<String> list = PropertyHelper.getInstruments();
		for (String string : list) {
			Instrument ins = Instrument.fromString(string);
			instruments.add(ins);
		}

		log.info("Subscribing instruments...");
		client.setSubscribedInstruments(instruments);
		log.info("Downloading data");

		if (HISTORICAL_DATA) {
			((ITesterClient)client).setDataInterval(Period.TICK, OfferSide.ASK, ITesterClient.InterpolationMethod.OPEN_TICK, HISTORICAL_DATA_INTERVAL[0].getTime(), HISTORICAL_DATA_INTERVAL[1].getTime());
			Future<?> future = ((ITesterClient)client).downloadData(null);
			future.get();
		}


		//start the strategy
		log.info("Starting strategy");
		client.startStrategy(new VladoStrategy());
		//now it's running
	}


}
