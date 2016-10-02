package com.financialanalysis.analysis;

import com.beust.jcommander.internal.Lists;
import com.financialanalysis.data.StockPrice;
import com.financialanalysis.data.Symbol;
import com.financialanalysis.data.Trend;
import com.financialanalysis.graphing.Point;
import lombok.extern.log4j.Log4j;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.joda.time.DateTime;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Log4j
public class AnalysisTools {
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

    public static double ave(double[] a) {
        double sum = 0;
        for(double d : a) {
            sum += d;
        }
        return sum / a.length;
    }

    public static double min(double[] a) {
        double min = 100000000.0;
        for(double d : a) {
            if(d < min) {
                min = d;
            }
        }
        return min;
    }

    public static double max(double[] a) {
        double max = -1000000000.0;
        for(double d : a) {
            if(d > max) {
                max = d;
            }
        }
        return max;
    }

    public static double[] addConst(double[] a, double s) {
        double[] n = new double[a.length];
        for(int i = 0; i < a.length; i++) {
            n[i] = a[i] + s;
        }
        return n;
    }

    public static int randInt(int min, int max) {
        Random random = new Random();
        int randomNum = random.nextInt((max - min) + 1) + min;
        return randomNum;
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    /**
     * Given a list of stock prices, return the sublist between start and end dates.
     */
    public static List<StockPrice> getValidStockPrices(List<StockPrice> allPrices, DateTime start, DateTime end) {
        List<StockPrice> valid = Lists.newArrayList();
        for(StockPrice sp : allPrices) {
            DateTime curDate = sp.getDate();
            if(curDate.isAfter(start.minusDays(1)) && curDate.isBefore(end.plusDays(1))) {
                valid.add(sp);
            }
        }

        return valid;
    }

    public static Map<Integer, Point> max(double[] input, int lookBack, int lookForward) {
        Map<Integer, Point> points = new HashMap<>();

        for(int i = lookBack; i < input.length; i++) {
            double cur = input[i];
            boolean shouldContinue = false;

            double max = cur;
            int index = i;
            //Look back period/2
            for(int j = i-1; j >= 0 && j >= i - lookBack; j--) {
                if(input[j] > max) {
                    shouldContinue = true;
                    break;
                }
            }
            if(shouldContinue) continue;

            //Look forward period/2
            for(int j = i+1; j < input.length && j <= i + lookForward; j++) {
                if(input[j] > max) {
                    shouldContinue = true;
                    break;
                }
            }
            if(shouldContinue) continue;

            points.put(index, new Point(index, max));
        }

        return points;
    }

    public static Map<Integer, Point> min(double[] input, int lookBack, int lookForward) {
        Map<Integer, Point> points = new HashMap<>();

        for(int i = lookBack; i < input.length; i++) {
            double cur = input[i];
            boolean shouldContinue = false;

            double min = cur;
            int index = i;
            //Look back period/2
            for(int j = i-1; j >= 0 && j >= i - lookBack; j--) {
                if(input[j] < min) {
                    shouldContinue = true;
                    break;
                }
            }
            if(shouldContinue) continue;

            //Look forward period/2
            for(int j = i+1; j < input.length && j <= i + lookForward; j++) {
                if(input[j] < min) {
                    shouldContinue = true;
                    break;
                }
            }
            if(shouldContinue) continue;

            points.put(index, new Point(index, min));
        }

        return points;
    }

    /**
     * Will look back starting at startIdx, period intervals. If it find an extrema, add it to the
     * SimpleRegression that constitutes the trend
     *
     * points: local min or max extrema as determined by min or max
     */
    public static Trend findTrend(Map<Integer, Point> points, int startIdx, int period, List<DateTime> dates, Symbol symbol) {
        SimpleRegression sr = new SimpleRegression();
        List<Point> pointsInTrend = new ArrayList<>();

        for(int i = startIdx; i >= 0 && i >= startIdx - period ; i--) {
            if(points.containsKey(i)) {
                sr.addData(points.get(i).getX(), points.get(i).getY());
                pointsInTrend.add(points.get(i));
            }
        }

        List<Point> pointsCorrectOrder = com.google.common.collect.Lists.reverse(pointsInTrend);
        DateTime start = new DateTime();
        DateTime end  = new DateTime();
        if(!pointsCorrectOrder.isEmpty()) {
            if((int) pointsCorrectOrder.get(0).getX() > dates.size()) {
                log.error("Found invalid case for " + symbol.getSymbol() + "\n" + "Point: " + pointsCorrectOrder.get(0) + " :: Size:" + dates.size());
            }

            start = dates.get((int) pointsCorrectOrder.get(0).getX());
            end = dates.get((int) pointsCorrectOrder.get(pointsCorrectOrder.size() - 1).getX());
        }

        Trend trend = new Trend(sr, pointsCorrectOrder, start, end);
        return trend;
    }
}








