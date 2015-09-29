package com.financialanalysis.updater;

import com.financialanalysis.data.StockFA;
import com.financialanalysis.data.StockPrice;
import com.google.common.collect.Lists;
import lombok.SneakyThrows;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class StockPuller {
    public static DateTime DEFAULT_START_DATE = new DateTime("2014-01-01", DateTimeZone.forID("America/Toronto"));

    public StockFA getStock(String symbol) {
        return getStock(symbol, DEFAULT_START_DATE, new DateTime());
    }

    public StockFA getStock(String symbol, DateTime from, DateTime to) {
        try {
            Stock stock = pullStockFromYahoo(symbol, from, to);
            StockFA stockFA = convertToStockFA(stock);
            return stockFA;
        } catch (Exception e) {
            return null;
        }
    }

    private Stock pullStockFromYahoo(String symbol, DateTime from, DateTime to) throws Exception {
        Calendar calendarFrom = from.toCalendar(Locale.getDefault());
        Calendar calendarTo = to.toCalendar(Locale.getDefault());
        Stock stock = YahooFinance.get(symbol, calendarFrom, calendarTo, Interval.DAILY);
        stock.setHistory(Lists.reverse(stock.getHistory())); // Reverse order of prices to follow chrono order
        return stock;
    }

    @SneakyThrows
    private StockFA convertToStockFA(Stock stock) {
        List<StockPrice> stockPrices = new ArrayList<>();
        for(HistoricalQuote q : stock.getHistory()) {
            DateTime date = new DateTime(q.getDate().getTimeInMillis(), DateTimeZone.forID(q.getDate().getTimeZone().getID()));
            StockPrice sp = new StockPrice(
                    date, q.getOpen().doubleValue(),
                    q.getLow().doubleValue(),
                    q.getHigh().doubleValue(),
                    q.getClose().doubleValue(),
                    q.getVolume()
            );
            stockPrices.add(sp);
        }
        StockFA stockFA = new StockFA(
                stock.getSymbol(),
                stock.getName(),
                stock.getCurrency(),
                stock.getStockExchange(),
                stockPrices
        );
        return stockFA;
    }
}
