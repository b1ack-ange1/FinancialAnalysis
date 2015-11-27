package com.financialanalysis.strategy;

import com.financialanalysis.data.StockFA;
import lombok.Data;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Map;

@Data
public class StrategyInput {
    private final List<StockFA> stocks;
    private final DateTime startDate;
    private final DateTime endDate;
    private final Map<String, String> params;
}
