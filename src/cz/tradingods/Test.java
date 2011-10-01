package cz.tradingods;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import cz.tradingods.common.HelpClass;
import cz.tradingods.common.Helper;

public class Test {

	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		 List<Map<String, Integer>>  list = Helper.getParametersCombination("vladostrategy");
		 list.clear();
	}
}
