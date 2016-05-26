package com.financialanalysis.strategy;

import com.financialanalysis.data.Account;
import com.financialanalysis.data.Symbol;
import com.financialanalysis.graphing.StockChart;
import lombok.Data;

import java.util.List;

@Data
public class StrategyOutput {
    private final Symbol symbol;
    private final Account account;
    private final List<StockChart> charts;
    private final String strategyName;

    public boolean isEmpty() {
        return charts.isEmpty();
    }
}
