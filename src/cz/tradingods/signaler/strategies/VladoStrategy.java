package cz.tradingods.signaler.strategies;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.log4j.Logger;

import com.dukascopy.api.*;
import com.dukascopy.api.IEngine.OrderCommand;

import cz.tradingods.Main;
import cz.tradingods.common.Helper;
import cz.tradingods.common.IndicatorHelper;
import cz.tradingods.common.MailHelper;
import cz.tradingods.common.PropertyHelper;
import cz.tradingods.common.ValueContainer;

public class VladoStrategy extends MyStrategy {
	private IEngine engine = null;
	private IIndicators indicators = null;
	private IContext context;
	private IHistory history;
	private int tagCounter = 0;
	private IConsole console;

	private static Logger log = Logger.getLogger(VladoStrategy.class);


	private int profitTargetParam;
	private int stopLostParam;
	private int emaSlowParam;
	private int emaFastParam;
	private int cciParam;
	private int macdAParam;
	private int macdBParam;
	private int macdCParam;

	double startingBalance;

	Calendar c = Calendar.getInstance();
	
	private ValueContainer valueContainer;
	

	public void onStart(IContext context) throws JFException {
		valueContainer = new ValueContainer();
		this.context = context;
		engine = context.getEngine();
		indicators = context.getIndicators();
		this.console = context.getConsole();
		this.history = context.getHistory();
		startingBalance = context.getAccount().getBalance();
		console.getOut().println("Started");
	}

	public void onStop() throws JFException {
		for (IOrder order : engine.getOrders()) {
			order.close();
		}
		profit = context.getAccount().getEquity() - startingBalance;
		super.onStop();
		console.getOut().println("Stopped");
	}

	public void onTick(Instrument instrument, ITick tick) throws JFException {
	}
	
	@SuppressWarnings("deprecation")
	public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
		if (!periods.contains(period))
			return;
		
		if (!context.getSubscribedInstruments().contains(instrument))
			return;
		
		//log.info("instr: " + instrument.name() + ", period: " + period.name() + ", bidBar time: " + new Date(bidBar.getTime()) + ", bid: " + bidBar.getClose());
		
		Date d = new Date(bidBar.getTime());
		c.setTime(d);
		if (c.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
			return;
		if (hourFrom >= 0 && hourTo >=0)
			if (d.getHours() < hourFrom || d.getHours() > hourTo)
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
		
		//log.info("instr: " + instrument.name() + ", period: " + period.name() + ", bidBar time: " + new Date(bidBar.getTime()) + ", bid: " + bidBar.getClose() + ", emaslow: " + emaSlow[0] + ", emafast: " + emaFast[0] + ", cci: " + cci[0] + ", macd: " + macd[0][0]);

		//log.info("instr: " + instrument.name() + ", period: " + period.name() + ", bidBar time: " + new Date(bidBar.getTime()) + ", bid: " + bidBar.getClose() + ", cci[0]: " + cci[0] + ", macd[0]: " + macd[0][0]);
		//log.info("instr: " + instrument.name() + ", period: " + period.name() + ", bidBar time: " + new Date(bidBar.getTime()) + ", bid: " + bidBar.getClose() + ", emafast[0]: " + emaFast[0] + ", emafast[1]: " + emaFast[1]);
		//log.info("instr: " + instrument.name() + ", period: " + period.name() + ", askBar time: " + new Date(askBar.getTime()) + ", ask: " + askBar.getClose() + ", bidBar time: " + new Date(bidBar.getTime()) + ", bid: " + bidBar.getClose() + ", emaslow: " + emaSlow[0] + ", emafast: " + emaFast[0] + ", cci: " + cci + ", macd: " + macd[0]);

		// otestuji zda doslo k prekrizeni indikatoru
		int result = IndicatorHelper.testForCrossover(emaFast, emaSlow);
		// pokud doslo k prekrizeni
		if (result != IndicatorHelper.CROSSOVER_NONE) {
			// vymazeme hodnoty minuleho prekrizeni
			valueContainer.putCrossoverDate(period, instrument, null);
			valueContainer.putLastCrossoverType(period, instrument, IndicatorHelper.CROSSOVER_NONE);
			// pokud dojde k prekrizeni ze spoda nahoru a macd je > 0 a cci je > 0
			if (result == IndicatorHelper.CROSSOVER_DOWN_UP && macd[0][0] > 0 && cci[0] > 0) {
				// pak vygeneruji zpravu
				String s = getMessage(instrument, bidBar, period, IndicatorHelper.CROSSOVER_DOWN_UP, cci[0], macd[0]);
				proccessMessage(s);
				enterPosition(instrument, OrderCommand.BUY, bidBar);
				valueContainer.putCrossoverDate(period, instrument, null);
				valueContainer.putLastCrossoverType(period, instrument, IndicatorHelper.CROSSOVER_NONE);
			} else if (result == IndicatorHelper.CROSSOVER_UP_DOWN && macd[0][0] < 0 && cci[0] < 0) {
				String s = getMessage(instrument, bidBar, period, IndicatorHelper.CROSSOVER_UP_DOWN, cci[0],  macd[0]);
				proccessMessage(s);
				enterPosition(instrument, OrderCommand.SELL, bidBar);
				valueContainer.putCrossoverDate(period, instrument, null);
				valueContainer.putLastCrossoverType(period, instrument, IndicatorHelper.CROSSOVER_NONE);
			} else {
				// zapamatuji si cas prekrizeni
				valueContainer.putCrossoverDate(period, instrument, new Date(bidBar.getTime()));
				// nastavim typ prekrizeni
				valueContainer.putLastCrossoverType(period, instrument, result);
			}
		} else {
			// a pokud nedoslo k prekrizeni a je nastavena posledni doba prekrizeni
			if (valueContainer.getCrossoverDate(period, instrument) != null) {
				// a byly splneny ostatni podminky prekrizeni
				if (valueContainer.getLastCrossoverType(period, instrument) == IndicatorHelper.CROSSOVER_DOWN_UP && macd[0][0] > 0 && cci[0] > 0) {
					String s = getMessage(instrument, bidBar, period, IndicatorHelper.CROSSOVER_DOWN_UP, cci[0], macd[0]);
					proccessMessage(s);
					enterPosition(instrument, OrderCommand.BUY, bidBar);
					valueContainer.putCrossoverDate(period, instrument, null);
					valueContainer.putLastCrossoverType(period, instrument, IndicatorHelper.CROSSOVER_NONE);
				}

				if (valueContainer.getLastCrossoverType(period, instrument) == IndicatorHelper.CROSSOVER_UP_DOWN && macd[0][0] < 0 && cci[0] < 0) {
					String s = getMessage(instrument, bidBar, period, IndicatorHelper.CROSSOVER_UP_DOWN, cci[0], macd[0]);
					proccessMessage(s);
					enterPosition(instrument, OrderCommand.SELL, bidBar);
					valueContainer.putCrossoverDate(period, instrument, null);
					valueContainer.putLastCrossoverType(period, instrument, IndicatorHelper.CROSSOVER_NONE);
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

		if (valueContainer.getCrossoverDate(period, instrument) != null)
			crossDate = sdf.format(valueContainer.getCrossoverDate(period, instrument)) + " ";

		return "SN: " + strategyName + " " + nowStr + " " + instrument.name() + " " + getTimeFrame(period) + " | " + "EMAs " + typeStr + " " + crossDate + "(bid:" + bid + ",cci:" + getRoundedNumber(cci, 2) /* + ",  macd:" + macd[0] + "," + macd[1] + "," + macd[2]*/ + ")";
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
	}

	@Override
	public void onMessage(IMessage arg0) throws JFException {
	}

	@Override
	public void setParams(String strategyName, long strategyId) {
		profitTargetParam = PropertyHelper.getConfigParam(strategyName, strategyId, "pt");
		stopLostParam = PropertyHelper.getConfigParam(strategyName, strategyId, "sl");
		emaSlowParam = PropertyHelper.getConfigParam(strategyName, strategyId, "emaslow");
		emaFastParam = PropertyHelper.getConfigParam(strategyName, strategyId, "emafast");
		cciParam = PropertyHelper.getConfigParam(strategyName, strategyId, "cci");
		macdAParam = PropertyHelper.getConfigParam(strategyName, strategyId, "macd_1");
		macdBParam = PropertyHelper.getConfigParam(strategyName, strategyId, "macd_2");
		macdCParam = PropertyHelper.getConfigParam(strategyName, strategyId, "macd_3");
	}
}