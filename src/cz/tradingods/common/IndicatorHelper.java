package cz.tradingods.common;

public class IndicatorHelper {

	public static final int CROSSOVER_UP_DOWN = -1;
	public static final int CROSSOVER_DOWN_UP = 1;
	public static final int CROSSOVER_NONE = 0;
	
	/**
	 * @param indicator1
	 * @param indicator2
	 * @return 0 = nic, -1 = UP->DOWN, 1 = DOWN->UP
	 */
	public static int testForCrossover(double[] indicator1, double[] indicator2) {
		int result = CROSSOVER_NONE;
		// UP -> DOWN : ind1 -> ind2
		if ((indicator1[1] < indicator1[0]) && (indicator1[1] < indicator2[1]) && (indicator1[0] >= indicator2[0])) {
			return CROSSOVER_UP_DOWN;
		}
		// DOWN -> UP : ind1 -> ind2
		if ((indicator1[1] > indicator1[0]) && (indicator1[1] > indicator2[1]) && (indicator1[0] <= indicator2[0])) {
			return CROSSOVER_DOWN_UP;
		}

		return result;
	}
	
	public static String getCrossoverTypeAsString(int crossOverType) {
		String typeStr;
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
		return typeStr;
	}
}
