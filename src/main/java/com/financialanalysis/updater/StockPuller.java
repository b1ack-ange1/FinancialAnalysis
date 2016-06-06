package com.financialanalysis.updater;

import com.financialanalysis.common.DateTimeUtils;
import com.financialanalysis.data.StockFA;
import com.financialanalysis.data.StockPrice;
import com.financialanalysis.data.Symbol;
import com.financialanalysis.questrade.HistoricDataGranularity;
import com.financialanalysis.questrade.QuestradeImpl;
import com.financialanalysis.questrade.response.Candle;
import com.financialanalysis.questrade.response.MarketCandlesResponse;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Log4j
public class StockPuller {
    private final QuestradeImpl questrade;

    public static DateTime DEFAULT_START_DATE = new DateTime("2008-01-01", DateTimeZone.forID("America/Toronto")).withTimeAtStartOfDay();

    @Inject
    public StockPuller(QuestradeImpl questradeImpl) {
        this.questrade = questradeImpl;
    }

    public StockFA getStock(Symbol symbol) throws Exception {
        return getStock(symbol, DEFAULT_START_DATE, DateTimeUtils.getToday());
    }

    public StockFA getStock(Symbol symbol, DateTime from, DateTime to) throws Exception {
        return pullStock(symbol, from, to);
    }

    private StockFA pullStock(Symbol symbol, DateTime from, DateTime to) throws Exception {
        try {
            // Try pulling from Yahoo first as it can handle more traffic
            StockFA stockFA = pullStockFromYahoo(symbol, from, to);
            log.info("Pulled " + symbol.getSymbol() + " from Yahoo");
            return stockFA;
        } catch (Exception e) {
            // Do nothing
        }

        try {
            // Try pulling from Questrade
            StockFA stockFA = pullStockFromQuestrade(symbol, from, to);
            log.info("Pulled " + symbol.getSymbol() + " from Questrade.");
            return stockFA;
        } catch (Exception e) {
            log.error("Pulling " + symbol.getSymbol() + " from Questrade failed.", e);
        }

        // If we've reached here, then we have not pulled it yet
        log.error("Failed to pull " + symbol.getSymbol());
        throw new Exception("Failed to pull " + symbol.getSymbol());
    }

    private StockFA pullStockFromQuestrade(Symbol symbol, DateTime from, DateTime to) throws IOException {
        MarketCandlesResponse response = questrade.getMarketCandles(symbol, from, to, HistoricDataGranularity.OneDay);
        List<Candle> candles = response.getCandles();
        List<StockPrice> stockPrices = candles.stream().map(
                c -> new StockPrice(
                        new DateTime(c.getStart()),
                        Double.parseDouble(c.getOpen()),
                        Double.parseDouble(c.getLow()),
                        Double.parseDouble(c.getHigh()),
                        Double.parseDouble(c.getClose()),
                        Double.parseDouble(c.getVolume())
                )).collect(Collectors.toList());

        return new StockFA(symbol, stockPrices);
    }

    private StockFA pullStockFromYahoo(Symbol symbol, DateTime from, DateTime to) throws IOException {
        Calendar calendarFrom = from.toCalendar(Locale.getDefault());
        Calendar calendarTo = to.toCalendar(Locale.getDefault());
        Stock stock = YahooFinance.get(symbol.getSymbol(), calendarFrom, calendarTo, Interval.DAILY);
        stock.setHistory(Lists.reverse(getHistory(stock))); // Reverse order of prices to follow chrono order
        return convertToStockFA(symbol, stock);
    }

    @SneakyThrows
    private List<HistoricalQuote> getHistory(Stock stock) {
        return stock.getHistory();
    }

    @SneakyThrows
    private StockFA convertToStockFA(Symbol symbol, Stock stock) {
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
        StockFA stockFA = new StockFA(symbol, stockPrices);
        return stockFA;
    }
}
