package cz.tradingods.signaler.strategies;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.log4j.Logger;

import com.dukascopy.api.*;
import com.dukascopy.api.IEngine.OrderCommand;

import cz.tradingods.common.IndicatorHelper;
import cz.tradingods.common.MailHelper;
import cz.tradingods.common.PropertyHelper;
import cz.tradingods.signaler.Main;
import cz.tradingods.signaler.ValueContainer;

public class VladoStrategy extends MyStrategy {
	private IEngine engine = null;
	private IIndicators indicators = null;
	private IHistory history;
	private int tagCounter = 0;
	private IConsole console;

	private static Logger log = Logger.getLogger(VladoStrategy.class);


	private int profitTargetParam = PropertyHelper.getCustomParam("vladostrategy", "pt");
	private int stopLostParam = PropertyHelper.getCustomParam("vladostrategy", "sl");
	private int emaSlowParam = PropertyHelper.getCustomParam("vladostrategy", "emaslow");
	private int emaFastParam = PropertyHelper.getCustomParam("vladostrategy", "emafast");
	private int cciParam = PropertyHelper.getCustomParam("vladostrategy", "cci");
	private int macdAParam = PropertyHelper.getCustomParam("vladostrategy", "macd_1");
	private int macdBParam = PropertyHelper.getCustomParam("vladostrategy", "macd_2");
	private int macdCParam = PropertyHelper.getCustomParam("vladostrategy", "macd_3");
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

	public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
			if (!periods.contains(period)) 
				return;
			
			IBar nowBar = history.getBar(instrument, period, OfferSide.BID, 0);
			IBar prevBar = history.getBar(instrument, period, OfferSide.BID, 1);
			double[] emaFast = indicators.ema(instrument, period, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, emaFastParam, Filter.WEEKENDS, 2, nowBar.getTime(), 0);
			double[] emaSlow = indicators.ema(instrument, period, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, emaSlowParam, Filter.WEEKENDS, 2, nowBar.getTime(), 0);
			double[] emaFastPrev = indicators.ema(instrument, period, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, emaFastParam, Filter.WEEKENDS, 2, prevBar.getTime(), 0);
			double[] emaSlowPrev = indicators.ema(instrument, period, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, emaSlowParam, Filter.WEEKENDS, 2, prevBar.getTime(), 0);
			
			emaFast[1] = emaFastPrev[0];
			emaSlow[1] = emaSlowPrev[0];
			
			double[][] macd = indicators.macd(instrument, period, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, macdAParam, macdBParam, macdCParam, Filter.WEEKENDS, 2, nowBar.getTime(), 0);
			double cci[] = indicators.cci(instrument, period, OfferSide.BID, cciParam, Filter.WEEKENDS, 2, nowBar.getTime(), 0);

			//log.info("instr: " + instrument.name() + ", period: " + period.name() + ", bidBar time: " + new Date(bidBar.getTime()) + ", bid: " + bidBar.getClose() + ", cci[0]: " + cci[0] + ", macd[0]: " + macd[0][0]);
			//log.info("instr: " + instrument.name() + ", period: " + period.name() + ", bidBar time: " + new Date(bidBar.getTime()) + ", bid: " + bidBar.getClose() + ", emafast[0]: " + emaFast[0] + ", emafast[1]: " + emaFast[1]);
			//log.info("instr: " + instrument.name() + ", period: " + period.name() + ", askBar time: " + new Date(askBar.getTime()) + ", ask: " + askBar.getClose() + ", bidBar time: " + new Date(bidBar.getTime()) + ", bid: " + bidBar.getClose() + ", emaslow: " + emaSlow[0] + ", emafast: " + emaFast[0] + ", cci: " + cci + ", macd: " + macd[0]);
			
			// otestuji zda doslo k prekrizeni indikatoru
			int result = IndicatorHelper.testForCrossover(emaFast, emaSlow);
			// pokud doslo k prekrizeni
			if (result != IndicatorHelper.CROSSOVER_NONE) {
				// vymazeme hodnoty minuleho prekrizeni
				ValueContainer.putCrossoverDate(period, instrument, null);
				ValueContainer.putLastCrossoverType(period, instrument, IndicatorHelper.CROSSOVER_NONE);
				// pokud dojde k prekrizeni ze spoda nahoru a macd je > 0 a cci je > 0
				if (result == IndicatorHelper.CROSSOVER_DOWN_UP && macd[0][0] > 0 && cci[0] > 0) {
					// pak vygeneruji zpravu
					String s = getMessage(instrument, bidBar, period, IndicatorHelper.CROSSOVER_DOWN_UP, cci[0], macd[0]);
					proccessMessage(s);
					enterPosition(instrument, OrderCommand.BUY, bidBar);
					ValueContainer.putCrossoverDate(period, instrument, null);
					ValueContainer.putLastCrossoverType(period, instrument, IndicatorHelper.CROSSOVER_NONE);
				} else if (result == IndicatorHelper.CROSSOVER_UP_DOWN && macd[0][0] < 0 && cci[0] < 0) {
					String s = getMessage(instrument, bidBar, period, IndicatorHelper.CROSSOVER_UP_DOWN, cci[0],  macd[0]);
					proccessMessage(s);
					enterPosition(instrument, OrderCommand.SELL, bidBar);
					ValueContainer.putCrossoverDate(period, instrument, null);
					ValueContainer.putLastCrossoverType(period, instrument, IndicatorHelper.CROSSOVER_NONE);
				} else {
					// zapamatuji si cas prekrizeni
					ValueContainer.putCrossoverDate(period, instrument, new Date(bidBar.getTime()));
					// nastavim typ prekrizeni
					ValueContainer.putLastCrossoverType(period, instrument, result);
				}
			} else {
				// a pokud nedoslo k prekrizeni a je nastavena posledni doba prekrizeni
				if (ValueContainer.getCrossoverDate(period, instrument) != null) {
					// a byly splneny ostatni podminky prekrizeni
					if (ValueContainer.getLastCrossoverType(period, instrument) == IndicatorHelper.CROSSOVER_DOWN_UP && macd[0][0] > 0 && cci[0] > 0) {
						String s = getMessage(instrument, bidBar, period, IndicatorHelper.CROSSOVER_DOWN_UP, cci[0], macd[0]);
						proccessMessage(s);
						enterPosition(instrument, OrderCommand.BUY, bidBar);
						ValueContainer.putCrossoverDate(period, instrument, null);
						ValueContainer.putLastCrossoverType(period, instrument, IndicatorHelper.CROSSOVER_NONE);
					}

					if (ValueContainer.getLastCrossoverType(period, instrument) == IndicatorHelper.CROSSOVER_UP_DOWN && macd[0][0] < 0 && cci[0] < 0) {
						String s = getMessage(instrument, bidBar, period, IndicatorHelper.CROSSOVER_UP_DOWN, cci[0], macd[0]);
						proccessMessage(s);
						enterPosition(instrument, OrderCommand.SELL, bidBar);
						ValueContainer.putCrossoverDate(period, instrument, null);
						ValueContainer.putLastCrossoverType(period, instrument, IndicatorHelper.CROSSOVER_NONE);
					}
			}
		}
	}

	protected String getLabel(Instrument instrument) {
		String label = instrument.name();
		label = label + (tagCounter++);
		label = label.toLowerCase();
		return label;
	}

	SimpleDateFormat sdf = new SimpleDateFormat("dd.MM HH:mm") {{setTimeZone(TimeZone.getTimeZone("GMT"));}};;
	
	public String getMessage(Instrument instrument, IBar bar, Period period, int crossOverType,double cci, double[] macd) {
		String nowStr = sdf.format(new Date(bar.getTime()));
		String typeStr = IndicatorHelper.getCrossoverTypeAsString(crossOverType);
		String crossDate = "";
		String bid = getRoundedNumber(bar.getClose(), 5);

		if (ValueContainer.getCrossoverDate(period, instrument) != null)
			crossDate = sdf.format(ValueContainer.getCrossoverDate(period, instrument)) + " ";

		return nowStr + " " + instrument.name() + " " + getTimeFrame(period) + " | " + "EMAs " + typeStr + " " + crossDate + "(bid:" + bid + ",cci:" + getRoundedNumber(cci, 2) /* + ",  macd:" + macd[0] + "," + macd[1] + "," + macd[2]*/ + ")";
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

	public void proccessMessage(String s) {
		log.info(s);
		MailHelper.sendEmail(s);
	}

	private void enterPosition(Instrument instrument, OrderCommand orderCommand, IBar bar) throws JFException {
		if (!Main.TRADING_ENABLED)
			return;
		if (engine.getOrders(instrument).size() > 0)
			return;

		double profitTargetPrice = 0;
		double stopLostPrice = 0;
		
		if (orderCommand == OrderCommand.BUY) {
			profitTargetPrice = bar.getClose() + instrument.getPipValue() * profitTargetParam;
			stopLostPrice = bar.getClose() - instrument.getPipValue() * stopLostParam;
		} else {
			profitTargetPrice = bar.getClose() - instrument.getPipValue() * profitTargetParam;
			stopLostPrice = bar.getClose() + instrument.getPipValue() * stopLostParam;
		}
		engine.submitOrder(getLabel(instrument), instrument, orderCommand, 0.001, 0, 0, stopLostPrice, profitTargetPrice);
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