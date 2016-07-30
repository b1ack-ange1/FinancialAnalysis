package com.financialanalysis.strategyV2;

import com.financialanalysis.data.StockPrice;

import java.util.List;

public abstract class Entry {
    public abstract EntryDecision getEntryDecision(List<StockPrice> stockPrices);
}
