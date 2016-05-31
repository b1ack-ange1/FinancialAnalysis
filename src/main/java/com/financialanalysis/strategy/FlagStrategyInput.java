package com.financialanalysis.strategy;

import com.financialanalysis.data.StockFA;
import lombok.Data;
import org.joda.time.DateTime;

@Data
public class FlagStrategyInput {
    private final StockFA stock;
    private final FlagConfig config;
    private final DateTime startDate;
    private final DateTime endDate;
}
