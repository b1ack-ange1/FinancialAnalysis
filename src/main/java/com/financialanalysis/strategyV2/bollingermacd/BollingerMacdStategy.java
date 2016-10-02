package com.financialanalysis.strategyV2.bollingermacd;

import com.financialanalysis.strategyV2.Strategy;
import com.google.inject.Inject;

public class BollingerMacdStategy extends Strategy {
    @Inject
    public BollingerMacdStategy(BollingerMacdEntry entry, BollingerMacdExit exit, BollingerMacdChart chart) {
        super(entry, exit, chart);
    }
}
