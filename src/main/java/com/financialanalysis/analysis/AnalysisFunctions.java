package com.financialanalysis.analysis;

import com.financialanalysis.graphing.Line;
import com.financialanalysis.graphing.Point;
import lombok.extern.log4j.Log4j;

import java.util.ArrayList;
import java.util.List;

import static com.financialanalysis.analysis.AnalysisBaseFunctions.ema;
import static com.financialanalysis.analysis.AnalysisBaseFunctions.sma;
import static com.financialanalysis.analysis.AnalysisBaseFunctions.stdDev;
import static com.financialanalysis.analysis.AnalysisTools.add;
import static com.financialanalysis.analysis.AnalysisTools.div;
import static com.financialanalysis.analysis.AnalysisTools.mult;
import static com.financialanalysis.analysis.AnalysisTools.sub;

@Log4j
public class AnalysisFunctions {
    /**
     * MACD Line: (fast EMA - slow EMA)
     * Signal Line: signal EMA of MACD Line
     * MACD Histogram: MACD Line - Signal Line
     */
    public static AnalysisFunctionResult macd(double[] prices, int fastPeriod, int slowPeriod, int signalPeriod) {
        double[] fastEma = ema(prices, fastPeriod);
        double[] slowEma = ema(prices, slowPeriod);

        double[] macd = sub(fastEma, slowEma);
        double[] macdSignal = sma(macd, signalPeriod);
        double[] macdHist = sub(macd, macdSignal);

        //Zero out the first slowPeriod + signalPeriod
        for(int i = 0; i < slowPeriod + signalPeriod; i++) {
            macd[i] = 0;
            macdSignal[i] = 0;
            macdHist[i] = 0;
        }

        AnalysisFunctionResult results = new AnalysisFunctionResult();
        results.setBeginIndex(slowPeriod + signalPeriod);
        results.setMacdHist(macdHist);
        results.setMacd(macd);
        results.setMacdSignal(macdSignal);
        return results;
    }

    public static AnalysisFunctionResult bollingerBands(double[] input, int period) {
        double[] bbMid = sma(input, period);

        double[] stdDev = stdDev(input, period);
        double[] stdDev2 = mult(stdDev, 2);

        double[] bbHigh = add(bbMid, stdDev2);
        double[] bbLow = sub(bbMid, stdDev2);

        AnalysisFunctionResult results = new AnalysisFunctionResult();
        results.setBbHigh(bbHigh);
        results.setBbLow(bbLow);
        results.setBbMid(bbMid);
        return results;
    }

    /**
     * ADX, first 40 intervals are invalid
     */
    public static AnalysisFunctionResult adx(double[] low, double[] high, double[] close, int period) {
        double[] highSubLow = sub(high, low);

        double[] highSubPrevClose = new double[close.length];
        highSubPrevClose[0] = 0;
        double[] lowSubPrevClose = new double[close.length];
        lowSubPrevClose[0] = 0;
        double[] highSubPrevHigh = new double[close.length];
        highSubPrevHigh[0] = 0;
        double[] prevHighSubHigh = new double[close.length];
        prevHighSubHigh[0] = 0;
        double[] lowSubPrevLow = new double[close.length];
        lowSubPrevLow[0] = 0;
        double[] prevLowSubLow = new double[close.length];
        prevLowSubLow[0] = 0;

        for(int i = 1; i < close.length; i++) {
            highSubPrevClose[i] = high[i] - close[i-1];
            lowSubPrevClose[i] = low[i] - close[i-1];
            highSubPrevHigh[i] = high[i] - high[i-1];
            prevHighSubHigh[i] = high[i-1] - high[i];
            lowSubPrevLow[i] = low[i] - low[i-1];
            prevLowSubLow[i] = low[i-1] - low[i];
        }

        double[] trueRange = new double[close.length];
        double[] dmPlus = new double[close.length];
        double[] dmMinus = new double[close.length];

        for(int i = 0; i < close.length; i++) {
            trueRange[i] = Math.max( Math.max(highSubLow[i], Math.abs(highSubPrevClose[i])), Math.abs(lowSubPrevClose[i]) );
            dmPlus[i] = highSubPrevHigh[i] > prevLowSubLow[i] ? Math.max(highSubPrevHigh[i], 0.0) : 0.0;
            dmMinus[i] = prevLowSubLow[i] > highSubPrevHigh[i] ? Math.max(prevLowSubLow[i], 0.0) : 0.0;
        }

        double[] smoothTR = new double[close.length];
        smoothTR[0] = trueRange[0];
        double[] smoothDMPlus = new double[close.length];
        smoothDMPlus[0] = dmPlus[0];
        double[] smoothDMMinus = new double[close.length];
        smoothDMMinus[0] = dmMinus[0];

        // Seed the values for Wilder smoothing as per
        // http://stockcharts.com/school/doku.php?id=chart_school:technical_indicators:average_directional_index_adx
        for(int i = 1; i < period; i++) {
            smoothTR[i] = trueRange[i] + smoothTR[i-1];
            smoothDMPlus[i] = dmPlus[i] + smoothDMPlus[i-1];
            smoothDMMinus[i] = dmMinus[i] + smoothDMMinus[i-1];
        }

        for(int i = period; i < close.length; i++) {
            smoothTR[i] = smoothTR[i-1] - (smoothTR[i-1]/period) + trueRange[i];
            smoothDMPlus[i] = smoothDMPlus[i-1] - (smoothDMPlus[i-1]/period) + dmPlus[i];
            smoothDMMinus[i] = smoothDMMinus[i-1] - (smoothDMMinus[i-1]/period) + dmMinus[i];
        }

        double[] finalDIPlus = new double[close.length];
        double[] finalDIMinus = new double[close.length];
        double[] DX = new double[close.length];

        for(int i = 0; i < close.length; i++) {
            finalDIPlus[i] = smoothDMPlus[i] / smoothTR[i] * 100.0;
            finalDIMinus[i] = smoothDMMinus[i] / smoothTR[i] * 100.0;
            DX[i] = Math.abs(finalDIPlus[i] - finalDIMinus[i]) / (finalDIPlus[i] + finalDIMinus[i]) * 100.0;
        }

        double[] ADX = sma(DX, period);

        //Zero out the first slowPeriod + signalPeriod
        for(int i = 0; i < period; i++) {
            ADX[i] = 0;
            finalDIPlus[i] = 0;
            finalDIMinus[i] = 0;
        }

        AnalysisFunctionResult result = new AnalysisFunctionResult();
        result.setBeginIndex(period);
        result.setDmMinus(finalDIMinus);
        result.setDmPlus(finalDIPlus);
        result.setAdx(ADX);
        return result;
    }

    /**
     *
     * @param input
     * @param threshold Percentage change allowed [0, 1]
     * @return
     */
    public static AnalysisFunctionResult zigzag(double[] input, double threshold) {
        List<Point> zzPoints = new ArrayList<>();
        // First point is always a zzPoint
        zzPoints.add(new Point(0, input[0]));

        for(int i = 1; i < input.length; i++) {
            double change = 1 - (input[i]/zzPoints.get(zzPoints.size()-1).getY());
            if(Math.abs(change) >= threshold) {
                zzPoints.add(new Point(i, input[i]));
            }
        }

        int lastIndex = zzPoints.size()-1;
        //If the last value in input is not a zzPoint, add it
        if((int) zzPoints.get(lastIndex).getX() < input.length - 1) {
            zzPoints.add(new Point(input.length - 1, input[input.length - 1]));
        }

        // Construct the zigzag lines
        double[] zigzag = new double[input.length];
        for(int i = 1; i < zzPoints.size(); i++) {
            Point prevP = zzPoints.get(i-1);
            Point curP = zzPoints.get(i);
            Line line = new Line(prevP, curP);
            double[] ys = line.generateYValues();

            int index = 0;
            for(int j = (int) prevP.getX(); j <= curP.getX(); j++) {
                zigzag[j] = ys[index];
                index++;
            }
        }

        AnalysisFunctionResult result = new AnalysisFunctionResult();
        result.setZigzag(zigzag);
        return result;
    }

    public static AnalysisFunctionResult pvo(double[] input, int fastPeriod, int slowPeriod, int signalPeriod) {
        double[] slowEma = ema(input, slowPeriod);
        double[] fastEma = ema(input, fastPeriod);

        double[] tmp1 = sub(fastEma, slowEma);
        double[] tmp2 = div(tmp1, slowEma);

        double[] pvo = mult(tmp2, 100);
        double[] signal = ema(pvo, signalPeriod);
        double[] hist = sub(pvo, signal);

        //Zero out the first slowPeriod + signalPeriod
        for(int i = 0; i < slowPeriod + signalPeriod; i++) {
            pvo[i] = 0;
            signal[i] = 0;
            hist[i] = 0;
        }

        AnalysisFunctionResult result = new AnalysisFunctionResult();
        result.setBeginIndex(slowPeriod + signalPeriod);
        result.setPvo(pvo);
        result.setPvoSignal(signal);
        result.setPvoHist(hist);
        return result;
    }

    private static void throwException(String message) {
        throw new RuntimeException(message);
    }
}
