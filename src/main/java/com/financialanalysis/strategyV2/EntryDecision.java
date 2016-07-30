package com.financialanalysis.strategyV2;

import lombok.Data;

@Data
public class EntryDecision {
    private final boolean entry;
    private final double weight;
}
