package com.financialanalysis.data;

import lombok.Data;

import java.util.List;

@Data
public class DetailedSymbol {
    private final String symbol;
    private final String symbolId;
    private final String prevDayClosePrice;
    private final String highPrice52;
    private final String lowPrice52;
    private final String averageVol3Months;
    private final String averageVol20Days;
    private final String outstandingShares;
    private final String eps;
    private final String pe;
    private final String dividend;
    private final String yield;
    private final String exDate;
    private final String marketCap;
    private final String tradeUnit;
    private final String optionType;
    private final String optionDurationType;
    private final String optionRoot;
    private final OptionContract optionContractDeliverables;
    private final String optionExerciseType;
    private final String listingExchange;
    private final String description;
    private final String securityType;
    private final String optionExpiryDate;
    private final String dividendDate;
    private final String optionStrikePrice;
    private final String isTradable;
    private final String isQuotable;
    private final String hasOptions;
    private final String currency;
    private final List<Ticks> minTicks;
    private final String industrySector;
    private final String industryGroup;
    private final String industrySubgroup;

    public Symbol convertToSymbol() {
        return new Symbol(
                symbol,
                symbolId,
                description,
                securityType,
                isTradable,
                isQuotable,
                currency
        );
    }
}
