package com.financialanalysis.questrade.response;

import lombok.Data;

@Data
public class Candle {
    private final String start;
    private final String end;
    private final String low;
    private final String high;
    private final String open;
    private final String close;
    private final String volume;
}
