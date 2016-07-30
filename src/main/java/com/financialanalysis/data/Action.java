package com.financialanalysis.data;

import lombok.Data;
import org.joda.time.DateTime;

@Data
public class Action {
    private final String action;
    private final double amount;
    private final double shares;
    private final double price;
    private final DateTime date;
    private final Symbol symbol;
    private final double weight;

    public Action clone() {
        return new Action(action, amount, shares, price, date, symbol, weight);
    }
}
