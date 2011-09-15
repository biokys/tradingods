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
import java.util.List;

import org.apache.log4j.Logger;

import com.dukascopy.api.*;

import cz.tradingods.common.IndicatorHelper;
import cz.tradingods.common.MailHelper;
import cz.tradingods.common.PropertyHelper;
import cz.tradingods.signaler.ValueContainer;

public class VladoStrategy extends MyStrategy {
	private IEngine engine = null;
	private IIndicators indicators = null;
	private IHistory history;
	private int tagCounter = 0;
	private IConsole console;
	
	private static Logger log = Logger.getLogger(VladoStrategy.class);

	
	private int emaSlowParam = PropertyHelper.getCustomParam("vladostrategy", "emaslow");
	private int emaFastParam = PropertyHelper.getCustomParam("vladostrategy", "emafast");
	private int cciParam = PropertyHelper.getCustomParam("vladostrategy", "cci");
	private int macdAParam = PropertyHelper.getCustomParams("vladostrategy", "macd")[0];
	private int macdBParam = PropertyHelper.getCustomParams("vladostrategy", "macd")[1];
	private int macdCParam = PropertyHelper.getCustomParams("vladostrategy", "macd")[2];
	private List<Period> periods = PropertyHelper.getTimeframes("vladostrategy");

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

	public void onBar(Instrument instrument, Period actualPeriod, IBar askBar, IBar bidBar) throws JFException {
		for (Period period : periods) {
			if (!period.equals(actualPeriod)) 
				continue;
			
			IBar prevBar_1 = history.getBar(instrument, period, OfferSide.BID, 1);
			double[] emaFast = indicators.ema(instrument, period, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, emaFastParam, Filter.NO_FILTER, 2, prevBar_1.getTime(), 0);
			double[] emaSlow = indicators.ema(instrument, period, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, emaSlowParam, Filter.NO_FILTER, 2, prevBar_1.getTime(), 0);
			double[] macd = indicators.macd(instrument, period, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, macdAParam, macdBParam, macdCParam, 0);
			double cci = indicators.cci(instrument, period, OfferSide.BID, cciParam, 0);

			// otestuji zda doslo k prekrizeni indikatoru
			int result = IndicatorHelper.testForCrossover(emaFast, emaSlow);
			// pokud doslo k prekrizeni
			if (result != IndicatorHelper.CROSSOVER_NONE) {
				// pokud dojde k prekrizeni ze spoda nahoru a macd je > 0 a cci je > 0
				if (result == IndicatorHelper.CROSSOVER_DOWN_UP && macd[0] > 0 && cci > 0) {
					// pak vygeneruji zpravu
					String s = getMessage(instrument, bidBar, period, IndicatorHelper.CROSSOVER_DOWN_UP, cci);
					log.info(s);
					MailHelper.sendEmail(s);
				}
				// analogicky
				if (result == IndicatorHelper.CROSSOVER_UP_DOWN && macd[0] < 0 && cci < 0) {
					String s = getMessage(instrument, bidBar, period, IndicatorHelper.CROSSOVER_UP_DOWN, cci);
					log.info(s);
					MailHelper.sendEmail(s);
				}
				// zapamatuji si cas prekrizeni
				ValueContainer.putCrossoverDate(period, instrument, new Date(bidBar.getTime()));
				// nastavim typ prekrizeni
				ValueContainer.putLastCrossoverType(period, instrument, result);
			}

			// pokud nedoslo k prekrizeni
			if (result == IndicatorHelper.CROSSOVER_NONE) {
				// a je nastavena posledni doba prekrizeni
				if (ValueContainer.getCrossoverDate(period, instrument) != null) {
					// a byly splneny ostatni podminky prekrizeni
					if (ValueContainer.getLastCrossoverType(period, instrument) == IndicatorHelper.CROSSOVER_DOWN_UP && macd[0] > 0 && cci > 0) {
						String s = getMessage(instrument, bidBar, period, IndicatorHelper.CROSSOVER_DOWN_UP, cci);
						log.info(s);
						MailHelper.sendEmail(s);
						ValueContainer.putCrossoverDate(period, instrument, null);
						ValueContainer.putLastCrossoverType(period, instrument, IndicatorHelper.CROSSOVER_NONE);
					}

					if (ValueContainer.getLastCrossoverType(period, instrument) == IndicatorHelper.CROSSOVER_UP_DOWN && macd[0] < 0 && cci < 0) {
						String s = getMessage(instrument, bidBar, period, IndicatorHelper.CROSSOVER_UP_DOWN, cci);
						log.info(s);
						MailHelper.sendEmail(s);
						ValueContainer.putCrossoverDate(period, instrument, null);
						ValueContainer.putLastCrossoverType(period, instrument, IndicatorHelper.CROSSOVER_NONE);
					}
				}
			}
		}

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

	public String getMessage(Instrument instrument, IBar bar, Period period, int crossOverType,double cci) {
		String nowStr = sdf.format(new Date(bar.getTime()));
		String typeStr = IndicatorHelper.getCrossoverTypeAsString(crossOverType);
		String crossDate = "";
		String bid = getRoundedNumber(bar.getClose(), 5);

		if (ValueContainer.getCrossoverDate(period, instrument) != null)
			crossDate = sdf.format(ValueContainer.getCrossoverDate(period, instrument)) + " ";

		return nowStr + " " + instrument.name() + " " + getTimeFrame(period) + " | " + "EMAs " + typeStr + " " + crossDate + "(bid:" + bid + ",cci:" + getRoundedNumber(cci, 2) + ")";
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
		System.out.println("cci: " + cciParam + ", emaslow: " + emaSlowParam + ", emafast: " + emaFastParam + ", macd: " + macdAParam + ", " + macdBParam + ", " + macdCParam);
	}

	@Override
	public void onAccount(IAccount arg0) throws JFException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMessage(IMessage arg0) throws JFException {
		// TODO Auto-generated method stub
		
	}
}