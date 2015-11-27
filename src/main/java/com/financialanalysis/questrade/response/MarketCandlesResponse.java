package com.financialanalysis.questrade.response;

import lombok.Data;

import java.util.List;

@Data
public class MarketCandlesResponse {
    private final List<Candle> candles;
}
