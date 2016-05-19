package com.financialanalysis.strategy;

import com.financialanalysis.data.Account;
import com.financialanalysis.data.Symbol;
import com.financialanalysis.graphing.StockChart;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class StrategyOutput {
    private final Account account;
    private final List<StockChart> charts;
    private final String strategyName;
}
