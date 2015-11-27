package com.financialanalysis.analysis;

import com.financialanalysis.data.StockPrice;
import com.financialanalysis.data.Symbol;
import org.joda.time.DateTime;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

public class AnalysisTools {
    private static Random random = new Random();

    public static double[] getClosingPrices(List<StockPrice> quotes) {
        return quotes.stream().map(q -> q.getClose()).mapToDouble(q -> q).toArray();
    }

    public static double[] getHighPrices(List<StockPrice> quotes) {
        return quotes.stream().map(q -> q.getHigh()).mapToDouble(q -> q).toArray();
    }

    public static double[] getLowPrices(List<StockPrice> quotes) {
        return quotes.stream().map(q -> q.getLow()).mapToDouble(q -> q).toArray();
    }

    public static double[] getOpenPrices(List<StockPrice> quotes) {
        return quotes.stream().map(q -> q.getOpen()).mapToDouble(q -> q).toArray();
    }

    public static double[] getVolume(List<StockPrice> quotes) {
        return quotes.stream().map(q -> q.getVolume()).mapToDouble(q -> q).toArray();
    }

    public static List<DateTime> getDates(List<StockPrice> quotes) {
        return quotes.stream().map(q -> q.getDate()).collect(Collectors.toList());
    }

    public static double[] deepCopyArray(double[] array) {
        double[] deep = new double[array.length];
        for(int i = 0; i < array.length; i++) {
            deep[i] = array[i];
        }
        return deep;
    }

    public static List<String> deepCopyStringList(List<String> list) {
        List<String> newList = new ArrayList<>();
        newList.addAll(list);
        return newList;
    }

    public static List<Symbol> deepCopySymbolList(List<Symbol> list) {
        if(list == null) return new LinkedList<>();
        return list.stream().map(s -> s.clone()).collect(Collectors.toList());
    }

    public static double[] sub(double[] from, double[] to) {
        if(from.length != to.length) throw new RuntimeException("Arrays length not equal");
        double[] res = new double[from.length];

        for(int i = 0; i < from.length; i++) {
            res[i] = from[i] - to[i];
        }
        return res;
    }

    public static double[] add(double[] from, double[] to) {
        if(from.length != to.length) throw new RuntimeException("Arrays length not equal");
        double[] res = new double[from.length];

        for(int i = 0; i < from.length; i++) {
            res[i] = from[i] + to[i];
        }
        return res;
    }

    public static double[] mult(double[] input, double factor) {
        double[] res = new double[input.length];
        for(int i = 0; i < input.length; i++) {
            res[i] = input[i] * factor;
        }
        return res;
    }

    public static double[] div(double[] input, double factor) {
        double[] res = new double[input.length];
        for(int i = 0; i < input.length; i++) {
            res[i] = input[i] / factor;
        }
        return res;
    }

    public static double[] div(double[] from, double[] to) {
        double[] res = new double[from.length];
        for(int i = 0; i < from.length; i++) {
            res[i] = to[i] != 0.0 ? from[i] / to[i] : 1;
        }
        return res;
    }

    public static int randInt(int min, int max) {
        int randomNum = random.nextInt((max - min) + 1) + min;
        return randomNum;
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public static List<StockPrice> getValidStockPrices(List<StockPrice> allPrices, DateTime start, DateTime end) {
        List<StockPrice> valid = new ArrayList<>();
        for(StockPrice sp : allPrices) {
            DateTime curDate = sp.getDate();
            if(curDate.isAfter(start.minusDays(1)) && curDate.isBefore(end.plusDays(1))) {
                valid.add(sp);
            }
        }

        return valid;
    }
}








