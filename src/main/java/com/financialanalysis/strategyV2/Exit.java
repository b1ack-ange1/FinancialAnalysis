package com.financialanalysis.strategyV2;

import com.financialanalysis.data.StockPrice;

import java.util.List;

public abstract class Exit {
    public abstract boolean shouldExit(List<StockPrice> stockPrices);

}
