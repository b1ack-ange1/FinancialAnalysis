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
    private final String symbol;

    public Action(String action, double amount, double shares, double price, DateTime date, String symbol) {
        this.action = action;
        this.amount = amount;
        this.shares = shares;
        this.price = price;
        this.date = date;
        this.symbol = symbol;
    }

    public Action clone() {
        return new Action(action, amount, shares, price, date, symbol);
    }
}
