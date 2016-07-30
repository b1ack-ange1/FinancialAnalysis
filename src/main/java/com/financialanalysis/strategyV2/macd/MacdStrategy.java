package com.financialanalysis.strategyV2.macd;

import com.financialanalysis.strategyV2.Strategy;
import com.google.inject.Inject;

public class MacdStrategy extends Strategy {
    @Inject
    public MacdStrategy(MacdEntry entry, MacdExit exit, MacdChart chart) {
        super(entry, exit, chart);
    }
}
