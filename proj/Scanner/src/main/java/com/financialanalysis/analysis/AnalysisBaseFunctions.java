package com.financialanalysis.analysis;

import com.financialanalysis.data.Trend;
import com.financialanalysis.graphing.Point;
import com.google.common.collect.Lists;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnalysisBaseFunctions {
    private static final int NUM_DAYS_WITHOUT_CHANGE_ALLOWED = 5;
    private static final int NUM_PERIODS_WITHOUT_CHANGE_ALLOWED = 2;
    private static final int MIN_MONTHS = 6;
    private static final int DAYS_PER_MONTH = 20;

    public static double[] stdDev(double[] input, int period) {
        double[] stdDevArr = new double[input.length];
        double[] devSquared = new double[input.length];

        for(int i = 0; i< input.length; i++){

            int counter = 0;
            double mean = 0;
            // Average the input of previous period of intervals
            for(int j = i; j >= 0 && i - j < period - 1; j--) {
                mean += input[j];
                counter++;
            }
            mean /= counter;

            // square the deviation
            double dev = input[i] - mean;
            devSquared[i] = Math.pow(dev, 2);

            int devCounter = 0;
            double devMean = 0;
            // Average the deviation over previous period of intervals
            for(int j = i; j >= 0 && i - j < period - 1; j--) {
                devMean += devSquared[j];
                devCounter++;
            }

            devMean /= devCounter;
            stdDevArr[i] = Math.sqrt(devMean);
        }
        return stdDevArr;
    }

    /**
     * http://www.iexplain.org/ema-how-to-calculate/
     *
     * EMA = Price(t) * k + EMA(y) * (1 – k)
     * t = today, y = yesterday, N = number of days in EMA, k = 2/(N+1)
     */
    public static double[] ema(double[] input, int period) {
        double[] ema = new double[input.length];
        double k = 2.0/(period + 1);

        double ave = 0;
        for(int i = 0; i < period; i++) {
            ema[i] = 0;
            ave += input[i];
        }
        ave /= period;

        ema[period] = ave;
//        ema[0] = input[0]; // Seed the first value
        for(int i = period+1; i < input.length; i++) {
            ema[i] = (input[i] * k) + (ema[i-1] * (1 - k));
        }

        return ema;
    }

    public static double[] sma(double[] input, int period) {
        double[] sma = new double[input.length];

        // Seed the first value
        sma[0] = input[0];
        for(int i = 1; i < input.length; i++) {
            // Compute the average of the previous period indices
            int counter = 0;
            double ave = 0;
            for(int j = i; j >= 0 && j > i - period; j--) {
                ave += input[j];
                counter++;
            }
            ave /= counter;

            sma[i] = ave;
        }

        return sma;
    }

    /**
     * Returns double array where each value contains the highest value for the previous period indices
     */
    public static double[] highest(double[] input, int period) {
        double[] highest = new double[input.length];
        //Seed the first value
        highest[0] = input[0];

        for(int i = 1; i < input.length; i++) {
            double maxInLookBackPeriod = 0.0;
            for(int j = i; j >= 0 && j > i - period; j--) {
                if(input[j] > maxInLookBackPeriod) maxInLookBackPeriod = input[j];
            }
            highest[i] = maxInLookBackPeriod;
        }

        return highest;
    }

    /**
     * Returns double array where each value contains the lowest value for the previous period indices
     */
    public static double[] lowest(double[] input, int period) {
        double[] lowest = new double[input.length];
        //Seed the first value
        lowest[0] = input[0];

        for(int i = 1; i < input.length; i++) {
            double lowestInLookBackPeriod = 1000000000.0;
            for(int j = i; j >= 0 && j > i - period; j--) {
                if(input[j] < lowestInLookBackPeriod) lowestInLookBackPeriod = input[j];
            }
            lowest[i] = lowestInLookBackPeriod;
        }

        return lowest;
    }

    public static double[] averageTrueRange(double[] highPrices, double[] lowPrices, int period) {
        double[] atr = new double[highPrices.length];

        for(int i = 0; i < highPrices.length; i++) {
            int counter = 0;
            double ave = 0;
            for(int j = i; j >= 0 && j > i - period; j--) {
                ave += highPrices[i] - lowPrices[i];
                counter++;
            }
            atr[i] = ave/counter;
        }

        return atr;
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
     * Will look back starting at startIdx, period intervals. If it find an extrma, add it to the
     * SimpleRegression that constitutes the trend
     *
     * points: min or max extrma as determined by min or max
     */
    public static Trend findTrend(Map<Integer, Point> points, int startIdx, int period, List<DateTime> dates) {
        SimpleRegression sr = new SimpleRegression();
        List<Point> pointsInTrend = new ArrayList<>();

        for(int i = startIdx; i >= 0 && i >= startIdx - period ; i--) {
            if(points.containsKey(i)) {
                sr.addData(points.get(i).getX(), points.get(i).getY());
                pointsInTrend.add(points.get(i));
            }
        }

        List<Point> pointsCorrectOrder = Lists.reverse(pointsInTrend);
        DateTime start = new DateTime();
        DateTime end  = new DateTime();
        if(!pointsCorrectOrder.isEmpty()) {
            start = dates.get((int) pointsCorrectOrder.get(0).getX());
            end = dates.get((int) pointsCorrectOrder.get(pointsCorrectOrder.size() - 1).getX());
        }

        Trend trend = new Trend(sr, pointsCorrectOrder, start, end);
        return trend;
    }

    public static boolean tradingActivityIsOk(double[] input) {
        if(input.length < MIN_MONTHS * DAYS_PER_MONTH) {
            return false;
        }

        int countDays = 0;
        int countPeriod = 0;
        for(int i = 1; i < input.length; i++) {
            double prev = input[i-1];
            double cur = input[i];

            if(Math.abs(prev - cur) < 0.00003) {
                countDays++;
            }

            if(countDays >= NUM_DAYS_WITHOUT_CHANGE_ALLOWED) {
                countPeriod++;
                countDays = 0;
            }

            if(countPeriod >+ NUM_PERIODS_WITHOUT_CHANGE_ALLOWED) {
                return false;
            }
        }

        return true;
    }
}






