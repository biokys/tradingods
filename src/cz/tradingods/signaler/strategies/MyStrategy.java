package cz.tradingods.signaler.strategies;

import java.util.Date;
import java.util.List;

import com.dukascopy.api.IStrategy;
import com.dukascopy.api.JFException;
import com.dukascopy.api.Period;

import cz.tradingods.common.Helper;
import cz.tradingods.common.PropertyHelper;

public abstract class MyStrategy implements IStrategy {

	protected static long strategyId = 0;

	protected String strategyName;

	public String readableParams;

	public Date[] dates;

	protected int hourFrom;
	protected int hourTo;

	protected double profit;

	protected List<Period> periods;

	public long setStrategyName(String name) {
		this.strategyName = name;
		strategyId++;
		periods = PropertyHelper.getTimeframes(strategyName);
		if (PropertyHelper.onHistoricalData()) {
			int hours[] = PropertyHelper.getHistoricalDataHourInterval();
			hourFrom = hours[0];
			hourTo = hours[1];
		} else {
			hourFrom = -1;
			hourTo = -1;
		}
		return strategyId;
	}

	public String getStrategyName() {
		return strategyName;
	}

	public void setParams() {
		setParams(strategyName, strategyId);
	}

	public long getStrategyId() {
		return strategyId;
	}

	protected abstract void setParams(String strategyName,  long strategyId);

	@Override
	public void onStop() throws JFException {
		if (PropertyHelper.isOptimizerEnabled())
			Helper.appendOptimizationToFile(dates, strategyName, readableParams, profit);
	}

}
