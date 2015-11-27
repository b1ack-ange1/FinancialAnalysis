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






    /****************/

//    private static final Core core = new Core();
//
//    public double[] smaTA_LIB(double[] prices, int period) {
//        return smaTA_LIB(prices, period, 0, prices.length - 1);
//    }
//
//    public static double[] smaTA_LIB(double[] prices, int period, int startIdx, int endIdx) {
//        double inReal[] = prices;
//        int optInTimePeriod = period;
//        MInteger outBegIdx = new MInteger();
//        MInteger outNbElement = new MInteger();
//        double outReal[] = new double[inReal.length];
//
//        RetCode code = core.sma(startIdx, endIdx, inReal, optInTimePeriod, outBegIdx, outNbElement, outReal);
//        if(code != RetCode.Success) {
//            throwException("sma failed");
//        }
//
//        return outReal;
//    }
//
//    public static double[] stdDevTA_LIB(double[] prices, int lookBackLength) {
//        double[] out = new double[prices.length];
//        MInteger begin = new MInteger();
//        MInteger length = new MInteger();
//
//        RetCode code = core.stdDev(0, prices.length - 1, prices, lookBackLength, 0, begin, length, out);
//        if(code != RetCode.Success) {
//            throwException("stdDev failed");
//        }
//
//        return out;
//    }
//
//    public static double[] adxTA_LIB(double[] highPrice, double[] lowPrice, double[] closePrice, int period, int startIdx, int endIdx) {
//        double inHigh[] = highPrice;
//        double inLow[] = lowPrice;
//        double inClose[] = closePrice;
//        int optInTimePeriod = period;
//        MInteger outBegIdx = new MInteger();
//        MInteger outNbElement = new MInteger();
//        double outReal[] = new double[inHigh.length];
//
//        RetCode code = core.adx(startIdx, endIdx, inHigh, inLow, inClose, optInTimePeriod, outBegIdx, outNbElement, outReal);
//        if(code != RetCode.Success) {
//            throwException("adx failed");
//        }
//
//        return outReal;
//    }
//
//    public static AnalysisFunctionResult emaTA_LIB(double[] prices, int period) {
//        return emaTA_LIB(prices, period, 0, prices.length - 1);
//    }
//
//    public static AnalysisFunctionResult emaTA_LIB(double[] prices, int period, int startIdx, int endIdx) {
//        double inReal[] = prices;
//        int optInTimePeriod = period;
//        MInteger outBegIdx = new MInteger();
//        MInteger outNbElement = new MInteger();
//        double outReal[] = new double[inReal.length];
//
//        RetCode code = ema(startIdx, endIdx, inReal, optInTimePeriod, outBegIdx, outNbElement, outReal);
//        if(code != RetCode.Success) {
//            throwException("ema failed");
//        }
//
//        AnalysisFunctionResult results = new AnalysisFunctionResult();
//        results.setEma(outReal);
//        results.setBeginIndex(outBegIdx.value);
//        results.setEndIndex(outNbElement.value);
//
//        return results;
//    }
//    /**************/
//    public static RetCode ema(int startIdx, int endIdx, double[] inReal, int optInTimePeriod, MInteger outBegIdx, MInteger outNBElement, double[] outReal) {
//        if(startIdx < 0) {
//            return RetCode.OutOfRangeStartIndex;
//        } else if(endIdx >= 0 && endIdx >= startIdx) {
//            if(optInTimePeriod == -2147483648) {
//                optInTimePeriod = 30;
//            } else if(optInTimePeriod < 2 || optInTimePeriod > 100000) {
//                return RetCode.BadParam;
//            }
//
//            return TA_INT_EMA(startIdx, endIdx, inReal, optInTimePeriod, 2.0D / (double)(optInTimePeriod + 1), outBegIdx, outNBElement, outReal);
//        } else {
//            return RetCode.OutOfRangeEndIndex;
//        }
//    }
//
//    public static RetCode TA_INT_EMA(int startIdx, int endIdx, double[] inReal, int optInTimePeriod, double optInK_1, MInteger outBegIdx, MInteger outNBElement, double[] outReal) {
//        int lookbackTotal = optInTimePeriod - 1;
//        if(startIdx < lookbackTotal) {
//            startIdx = lookbackTotal;
//        }
//
//        if(startIdx > endIdx) {
//            outBegIdx.value = 0;
//            outNBElement.value = 0;
//            return RetCode.Success;
//        } else {
//            outBegIdx.value = startIdx;
//            double prevMA;
//            int today;
//
//            today = startIdx - lookbackTotal;
//            int i = optInTimePeriod;
//
//            double tempReal;
//            for(tempReal = 0.0D; i-- > 0; tempReal += inReal[today++]) {
//                ;
//            }
//
//            prevMA = tempReal / (double)optInTimePeriod;
//
//            while(today <= startIdx) {
//                prevMA += (inReal[today++] - prevMA) * optInK_1;
//            }
//
//            outReal[0] = prevMA;
//
//            int outIdx;
//            today = 1;
//            for(outIdx = 1; today <= endIdx; outReal[outIdx++] = prevMA) {
//                prevMA += (inReal[today++] - prevMA) * optInK_1;
//            }
//
//            outNBElement.value = outIdx;
//            return RetCode.Success;
//        }
//    }
//
//    public static RetCode macd(int startIdx, int endIdx, double[] inReal, int optInFastPeriod, int optInSlowPeriod, int optInSignalPeriod, MInteger outBegIdx, MInteger outNBElement, double[] outMACD, double[] outMACDSignal, double[] outMACDHist) {
//        if(startIdx < 0) {
//            return RetCode.OutOfRangeStartIndex;
//        } else if(endIdx >= 0 && endIdx >= startIdx) {
//            if(optInFastPeriod == -2147483648) {
//                optInFastPeriod = 12;
//            } else if(optInFastPeriod < 2 || optInFastPeriod > 100000) {
//                return RetCode.BadParam;
//            }
//
//            if(optInSlowPeriod == -2147483648) {
//                optInSlowPeriod = 26;
//            } else if(optInSlowPeriod < 2 || optInSlowPeriod > 100000) {
//                return RetCode.BadParam;
//            }
//
//            if(optInSignalPeriod == -2147483648) {
//                optInSignalPeriod = 9;
//            } else if(optInSignalPeriod < 1 || optInSignalPeriod > 100000) {
//                return RetCode.BadParam;
//            }
//
//            return TA_INT_MACD(startIdx, endIdx, inReal, optInFastPeriod, optInSlowPeriod, optInSignalPeriod, outBegIdx, outNBElement, outMACD, outMACDSignal, outMACDHist);
//        } else {
//            return RetCode.OutOfRangeEndIndex;
//        }
//    }
//    public static RetCode TA_INT_MACD(int startIdx, int endIdx, double[] inReal, int optInFastPeriod, int optInSlowPeriod, int optInSignalPeriod_2, MInteger outBegIdx, MInteger outNBElement, double[] outMACD, double[] outMACDSignal, double[] outMACDHist) {
//        MInteger outBegIdx1 = new MInteger();
//        MInteger outNbElement1 = new MInteger();
//        MInteger outBegIdx2 = new MInteger();
//        MInteger outNbElement2 = new MInteger();
//        int tempInteger;
//        if(optInSlowPeriod < optInFastPeriod) {
//            tempInteger = optInSlowPeriod;
//            optInSlowPeriod = optInFastPeriod;
//            optInFastPeriod = tempInteger;
//        }
//
//        double k1;
//        if(optInSlowPeriod != 0) {
//            k1 = 2.0D / (double)(optInSlowPeriod + 1);
//        } else {
//            optInSlowPeriod = 26;
//            k1 = 0.075D;
//        }
//
//        double k2;
//        if(optInFastPeriod != 0) {
//            k2 = 2.0D / (double)(optInFastPeriod + 1);
//        } else {
//            optInFastPeriod = 12;
//            k2 = 0.15D;
//        }
//
//        int lookbackSignal = optInSignalPeriod_2-1;
//        int lookbackTotal = lookbackSignal + optInSlowPeriod-1;
//        if(startIdx < lookbackTotal) {
//            startIdx = lookbackTotal;
//        }
//
//        if(startIdx > endIdx) {
//            outBegIdx.value = 0;
//            outNBElement.value = 0;
//            return RetCode.Success;
//        } else {
//            tempInteger = endIdx - startIdx + 1 + lookbackTotal; //lookbackSignal;
//            double[] fastEMABuffer = new double[tempInteger];
//            double[] slowEMABuffer = new double[tempInteger];
//            tempInteger = startIdx - lookbackSignal;
//            RetCode retCode = TA_INT_EMA(tempInteger, endIdx, inReal, optInSlowPeriod, k1, outBegIdx1, outNbElement1, slowEMABuffer);
//            if(retCode != RetCode.Success) {
//                outBegIdx.value = 0;
//                outNBElement.value = 0;
//                return retCode;
//            } else {
//                retCode = TA_INT_EMA(tempInteger, endIdx, inReal, optInFastPeriod, k2, outBegIdx2, outNbElement2, fastEMABuffer);
//                if(retCode != RetCode.Success) {
//                    outBegIdx.value = 0;
//                    outNBElement.value = 0;
//                    return retCode;
//                } else if(outBegIdx1.value == tempInteger && outBegIdx2.value == tempInteger && outNbElement1.value == outNbElement2.value && outNbElement1.value == endIdx - startIdx + 1 + lookbackTotal/*lookbackSignal*/) {
//                    int i;
//                    for(i = 0; i < outNbElement1.value; ++i) {
//                        fastEMABuffer[i] -= slowEMABuffer[i];
//                    }
//
//                    System.arraycopy(fastEMABuffer, lookbackSignal, outMACD, 0, endIdx - startIdx + 1);
//                    retCode = TA_INT_EMA(0, outNbElement1.value - 1, (double[])fastEMABuffer, optInSignalPeriod_2, 2.0D / (double)(optInSignalPeriod_2 + 1), outBegIdx2, outNbElement2, outMACDSignal);
//                    if(retCode != RetCode.Success) {
//                        outBegIdx.value = 0;
//                        outNBElement.value = 0;
//                        return retCode;
//                    } else {
//                        for(i = 0; i < outNbElement2.value; ++i) {
//                            outMACDHist[i] = outMACD[i] - outMACDSignal[i];
//                        }
//
//                        outBegIdx.value = startIdx;
//                        outNBElement.value = outNbElement2.value;
//                        return RetCode.Success;
//                    }
//                } else {
//                    outBegIdx.value = 0;
//                    outNBElement.value = 0;
//                    return RetCode.InternalError;
//                }
//            }
//        }
//    }
//    /**************/
//
//
//
//    public static AnalysisFunctionResult macdTA_LIB(double[] prices,
//                                 int fastPeriod,
//                                 int slowPeriod,
//                                 int signalPeriod,
//                                 int startIdx,
//                                 int endIdx,
//                                 double outMACD[],
//                                 double outMACDSignal[],
//                                 double outMACDHist[]) {
//        double inReal[] = prices;
//        MInteger outBegIdx = new MInteger();
//        MInteger outNbElement = new MInteger();
//        RetCode code = core.macd(
//                startIdx,
//                endIdx,
//                inReal,
//                fastPeriod,
//                slowPeriod,
//                signalPeriod,
//                outBegIdx,
//                outNbElement,
//                outMACD,
//                outMACDSignal,
//                outMACDHist
//        );
//        if(code != RetCode.Success) {
//            throwException("macdTA_LIB failed");
//        }
//
//        AnalysisFunctionResult results = new AnalysisFunctionResult();
//        results.setBeginIndex(outBegIdx.value);
//        results.setEndIndex(outNbElement.value);
//
//        return results;
//    }
//
//    public static AnalysisFunctionResult macdTA_LIB(double[] prices,
//                                              int fastPeriod,
//                                              int slowPeriod,
//                                              int signalPeriod,
//                                              int startIdx,
//                                              int endIdx) {
//        double outMACD[] = new double[prices.length];
//        double outMACDSignal[] = new double[prices.length];
//        double outMACDHist[] = new double[prices.length];
//
//        double inReal[] = prices;
//        MInteger outBegIdx = new MInteger();
//        MInteger outNbElement = new MInteger();
//        RetCode code = core.macd(
//                startIdx,
//                endIdx,
//                inReal,
//                fastPeriod,
//                slowPeriod,
//                signalPeriod,
//                outBegIdx,
//                outNbElement,
//                outMACD,
//                outMACDSignal,
//                outMACDHist
//        );
//        if(code != RetCode.Success) {
//            throwException("macdTA_LIB failed");
//        }
//
//        AnalysisFunctionResult results = new AnalysisFunctionResult();
//        results.setBeginIndex(outBegIdx.value);
//        results.setEndIndex(outNbElement.value);
//        results.setMacd(outMACD);
//        results.setMacdSignal(outMACDSignal);
//        results.setMacdHist(outMACDHist);
//
//        return results;
//    }
//
//    public static int stochTA_LIB(double[] highPrice,
//                     double[] lowPrice,
//                     double[] closePrice,
//                     int fastK,
//                     int slowK,
//                     int slowD,
//                     int startIdx,
//                     int endIdx,
//                     double outSlowK[],
//                     double outSlowD[]) {
//        double inHigh[] = highPrice;
//        double inLow[] = lowPrice;
//        double inClose[] = closePrice;
//        MInteger outBegIdx = new MInteger();
//        MInteger outNbElement = new MInteger();
//
//        RetCode code = core.stoch(
//                startIdx,
//                endIdx,
//                inHigh,
//                inLow,
//                inClose,
//                fastK,
//                slowK,
//                MAType.Sma,
//                slowD,
//                MAType.Sma,
//                outBegIdx,
//                outNbElement,
//                outSlowK,
//                outSlowD
//        );
//        if (code != RetCode.Success) {
//            throwException("stoch failed");
//        }
//        return outNbElement.value;
//    }


}
