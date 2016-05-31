package com.financialanalysis.analysis;

import lombok.extern.log4j.Log4j;

@Log4j
public class AnalysisBaseFunctions {

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
    // This may not be thread safe
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
}






