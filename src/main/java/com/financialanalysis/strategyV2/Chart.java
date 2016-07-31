package com.financialanalysis.strategyV2;

import com.financialanalysis.data.Account;
import com.financialanalysis.data.StockFA;
import com.financialanalysis.graphing.StockChart;

public abstract class Chart {
    public abstract StockChart getChart(StockFA stock, Account account);
}
