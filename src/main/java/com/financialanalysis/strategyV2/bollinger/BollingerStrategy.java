package com.financialanalysis.strategyV2.bollinger;

import com.financialanalysis.strategyV2.Strategy;
import com.google.inject.Inject;

public class BollingerStrategy extends Strategy {
    @Inject
    public BollingerStrategy(BollingerEntry entry, BollingerExit exit, BollingerChart chart) {
        super(entry, exit, chart);
    }
}
