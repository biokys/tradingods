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
package cz.tradingods.signaler.strategies;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.mail.MessagingException;

import com.dukascopy.api.*;

import cz.tradingods.common.IndicatorHelper;
import cz.tradingods.common.MailHelper;
import cz.tradingods.common.PropertyHelper;

public class VladoStrategy extends MyStrategy {
	private IEngine engine = null;
	private IIndicators indicators = null;
	private IHistory history;
	private int tagCounter = 0;
	private IConsole console;

	private int ema1Param = 3;
	private int ema2Param = 13;
	private int cciParam = 14;
	private int macdAParam = 5;
	private int macdBParam = 35;
	private int macdCParam = 5;

	private Period firstTF = PropertyHelper.getTimeframes().get(0);
	private Period secondTF = PropertyHelper.getTimeframes().get(1);

	double[][] emaFast_1 = new double[Instrument.values().length][];
	double[][] emaSlow_1 = new double[Instrument.values().length][];
	double[][] macd_1 = new double[Instrument.values().length][];
	double[] cci_1 = new double[Instrument.values().length];
	int lastCrossoverType_1 = IndicatorHelper.CROSSOVER_NONE;

	double[][] ema13_2 = new double[Instrument.values().length][];
	double[][] ema3_2 = new double[Instrument.values().length][];
	double[][] macd_2 = new double[Instrument.values().length][];
	double[] cci_2 = new double[Instrument.values().length];
	int lastCrossoverType_2 = IndicatorHelper.CROSSOVER_NONE;

	

	public static Date[] crossover1hour = new Date[Instrument.values().length];
	public static Date[] crossover2hour = new Date[Instrument.values().length];

	public void setParams(int cci, int ema1, int ema2, int[] macd) {
		this.cciParam = cci;
		this.ema1Param = ema1;
		this.ema2Param = ema2;
		this.macdAParam = macd[0];
		this.macdBParam = macd[1];
		this.macdCParam = macd[2];
	}
	
	public Integer[] getParams() {
		Integer[] params = new Integer[6];
		params[0] = cciParam;
		params[1] = ema1Param;
		params[2] = ema2Param;
		params[3] =  macdAParam;
		params[4] = macdBParam;
		params[5] = macdCParam;
		return params;
	}

	public void onStart(IContext context) throws JFException {
		engine = context.getEngine();
		indicators = context.getIndicators();
		this.console = context.getConsole();
		this.history = context.getHistory();

		console.getOut().println("Started");
	}

	public void onStop() throws JFException {
		for (IOrder order : engine.getOrders()) {
			order.close();
		}
		console.getOut().println("Stopped");
	}

	public void onTick(Instrument instrument, ITick tick) throws JFException {
	}

	public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
		// 1. TF
		if (period == firstTF) {
			IBar prevBar_1 = history.getBar(instrument, firstTF, OfferSide.BID, 1);
			emaFast_1[instrument.ordinal()] = indicators.ema(instrument, firstTF, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, ema2Param, Filter.NO_FILTER, 2, prevBar_1.getTime(), 0);
			emaSlow_1[instrument.ordinal()] = indicators.ema(instrument, firstTF, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, ema1Param, Filter.NO_FILTER, 2, prevBar_1.getTime(), 0);
			macd_1[instrument.ordinal()] = indicators.macd(instrument, firstTF, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, macdAParam, macdBParam, macdCParam, 0);
			cci_1[instrument.ordinal()] = indicators.cci(instrument, firstTF, OfferSide.BID, cciParam, 0);

			String s = "NOTHING GENERATED - FAIL?"; 

			// otestuji zda doslo k prekrizeni indikatoru
			int result = IndicatorHelper.testForCrossover(emaSlow_1[instrument.ordinal()], emaFast_1[instrument.ordinal()]);
			// pokud doslo k prekrizeni
			if (result != IndicatorHelper.CROSSOVER_NONE) {
				// nastavim typ posledniho prekrizeni na aktualni
				lastCrossoverType_1 = result;
				// pokud dojde k prekrizeni ze spoda nahoru a macd je > 0 a cci je > 0
				if (result == IndicatorHelper.CROSSOVER_DOWN_UP && macd_1[instrument.ordinal()][0] > 0 && cci_1[instrument.ordinal()] > 0) {
					// pak vygeneruji zpravu
					s = getMessage(instrument, bidBar, firstTF, IndicatorHelper.CROSSOVER_DOWN_UP, 1);
				}
				// analogicky
				if (result == IndicatorHelper.CROSSOVER_UP_DOWN && macd_1[instrument.ordinal()][0] < 0 && cci_1[instrument.ordinal()] < 0) {
					s = getMessage(instrument, bidBar, firstTF, IndicatorHelper.CROSSOVER_UP_DOWN, 1);
				}
				// vypisu do konzole
				System.out.println(s);
				// poslu email
				MailHelper.sendEmail(s);
				// zapamatuji si cas prekrizeni
				crossover1hour[instrument.ordinal()] = new Date(bidBar.getTime());
			}

			// pokud nedoslo k prekrizeni
			if (result == IndicatorHelper.CROSSOVER_NONE) {
				// a je nastavena posledni doba prekrizeni
				if (crossover1hour[instrument.ordinal()] != null) {
					// a byly splneny ostatni podminky prekrizeni
					if (macd_1[instrument.ordinal()][0] > 0 && cci_1[instrument.ordinal()] > 0) {
						s = getMessage(instrument, bidBar, firstTF, IndicatorHelper.CROSSOVER_DOWN_UP, 1);
						System.out.println(s);
						MailHelper.sendEmail(s);
						crossover1hour[instrument.ordinal()] = null;
						lastCrossoverType_1 = IndicatorHelper.CROSSOVER_NONE;
					}

					if (macd_1[instrument.ordinal()][0] < 0 && cci_1[instrument.ordinal()] < 0) {
						s = getMessage(instrument, bidBar, firstTF, IndicatorHelper.CROSSOVER_UP_DOWN, 1);
						System.out.println(s);
						MailHelper.sendEmail(s);
						crossover1hour[instrument.ordinal()] = null;
						lastCrossoverType_1 = IndicatorHelper.CROSSOVER_NONE;
					}
				}
			}
		}





		// 2. TF
		/*if (period == secondTF) {
			IBar prevBar_2 = history.getBar(instrument, secondTF, OfferSide.BID, 1);
			ema13_2[instrument.ordinal()] = indicators.ema(instrument, secondTF, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, 13, Filter.NO_FILTER, 2, prevBar_2.getTime(), 0);
			ema3_2[instrument.ordinal()] = indicators.ema(instrument, secondTF, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, 3, Filter.NO_FILTER, 2, prevBar_2.getTime(), 0);
			macd_2[instrument.ordinal()] = indicators.macd(instrument, secondTF, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, 5, 35, 5, 0);
			cci_2[instrument.ordinal()] = indicators.cci(instrument, secondTF, OfferSide.BID, 14, 0);

			String s; 

			int result = testForCrossover(ema3_2[instrument.ordinal()], ema13_2[instrument.ordinal()]);
			if (result != CROSSOVER_NONE) {
				lastCrossoverType_2 = result;
				if (result == CROSSOVER_DOWN_UP && macd_2[instrument.ordinal()][0] > 0 && cci_2[instrument.ordinal()] > 0) {
					s = getMessage(instrument, bidBar, secondTF, CROSSOVER_DOWN_UP, 1);
					System.out.println(s);
					sendEmail(s);
				}
				if (result == CROSSOVER_UP_DOWN && macd_2[instrument.ordinal()][0] < 0 && cci_2[instrument.ordinal()] < 0) {
					s = getMessage(instrument, bidBar, secondTF, CROSSOVER_UP_DOWN, 1);
					System.out.println(s);
					sendEmail(s);
				}

				crossover2hour[instrument.ordinal()] = new Date(bidBar.getTime());

			}

			if (result == CROSSOVER_NONE) {
				if (crossover2hour[instrument.ordinal()] != null) {
					if (lastCrossoverType_2 == CROSSOVER_DOWN_UP && macd_2[instrument.ordinal()][0] > 0 && cci_2[instrument.ordinal()] > 0) {
						s = getMessage(instrument, bidBar, secondTF, CROSSOVER_DOWN_UP, 1);
						System.out.println(s);
						sendEmail(s);
						crossover2hour[instrument.ordinal()] = null;
						lastCrossoverType_2 = CROSSOVER_NONE;
					}

					if (lastCrossoverType_2 == CROSSOVER_UP_DOWN && macd_2[instrument.ordinal()][0] < 0 && cci_2[instrument.ordinal()] < 0) {
						s = getMessage(instrument, bidBar, secondTF, CROSSOVER_UP_DOWN, 1);
						System.out.println(s);
						sendEmail(s);
						crossover2hour[instrument.ordinal()] = null;
						lastCrossoverType_2 = CROSSOVER_NONE;
					}
				}
			}
		}*/

	}


	public void onMessage(IMessage message) throws JFException {
	}

	public void onAccount(IAccount account) throws JFException {
	}

	protected int positionsTotal(Instrument instrument) throws JFException {
		int counter = 0;
		for (IOrder order : engine.getOrders(instrument)) {
			if (order.getState() == IOrder.State.FILLED) {
				counter++;
			}
		}
		return counter;
	}

	protected String getLabel(Instrument instrument) {
		String label = instrument.name();
		label = label.substring(0, 2) + label.substring(3, 5);
		label = label + (tagCounter++);
		label = label.toLowerCase();
		return label;
	}

	String dateFormat = "dd.MM HH:mm";
	SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);

	public String getMessage(Instrument instrument, IBar bar, Period period, int crossOverType, int timeframeNum) {
		String msg = "";
		String typeStr = "";
		String nowStr = sdf.format(new Date(bar.getTime()));

		String crossDate = "";
		String bid = getRoundedNumber(bar.getClose(), 5);
		double cci = 0;

		switch (timeframeNum) {
		case 1:
			if (crossover1hour[instrument.ordinal()] != null)
				crossDate = sdf.format(crossover1hour[instrument.ordinal()]) + " ";
			cci = cci_1[instrument.ordinal()];
			break;
		case 2:
			if (crossover2hour[instrument.ordinal()] != null)
				crossDate = sdf.format(crossover2hour[instrument.ordinal()]) + " ";
			cci = cci_2[instrument.ordinal()];
			break;
		}

		switch (crossOverType) {
		case IndicatorHelper.CROSSOVER_UP_DOWN:
			typeStr = "SELL";
			break;
		case IndicatorHelper.CROSSOVER_DOWN_UP:
			typeStr = "BUY";
			break;
		case IndicatorHelper.CROSSOVER_NONE:
		default:
			typeStr = "NONE";
			break;
		}
		msg = nowStr + " " + instrument.name() + " " + getTimeFrame(period) + " | " + "EMAs " + typeStr + " " + crossDate + "(bid:" + bid + ",cci:" + getRoundedNumber(cci, 2) + ")";
		showStrategyParams();
		return msg;
	}

	private String getTimeFrame(Period period) {
		Unit u = period.getUnit();
		return period.getNumOfUnits() + u.getShortDescription().toUpperCase();
	}

	private String getRoundedNumber(double number, int order) {
		BigDecimal bd = new BigDecimal(number);
		return bd.setScale(order, BigDecimal.ROUND_HALF_UP) + "";
	}

	public void showStrategyParams() {
		System.out.println("cci: " + cciParam + ", ema1: " + ema1Param + ", ema2: " + ema2Param + ", macd: " + macdAParam + ", " + macdBParam + ", " + macdCParam);
	}
}