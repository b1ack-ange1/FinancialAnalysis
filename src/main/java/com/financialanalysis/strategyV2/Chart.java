package com.financialanalysis.strategyV2;

import com.financialanalysis.data.Account;
import com.financialanalysis.data.StockFA;
import com.financialanalysis.graphing.StockChart;

import java.util.List;

public abstract class Chart {
    public abstract List<StockChart> getCharts(StockFA stock, Account account);
}
