package com.financialanalysis.data;

import com.financialanalysis.strategy.AbstractStrategy;
import lombok.Data;
import org.joda.time.DateTime;

@Data
public class Signal {
    private final String longOrShort;
    private final double price;
    private final DateTime date;

    public static final String LONG = "long";
    public static final String SHORT = "short";

    public Signal(String longOrShort, double price, DateTime date) {
        this.longOrShort = longOrShort;
        this.price = price;
        this.date = date;
    }

    public boolean isLong() {
        return longOrShort.equalsIgnoreCase(LONG);
    }

    public boolean isShort() {
        return longOrShort.equalsIgnoreCase(SHORT);
    }

    @Override
    public String toString() {
        return String.format("(%s) %8.2f %s", longOrShort, price, date.toString().split("T")[0]);
    }
}
