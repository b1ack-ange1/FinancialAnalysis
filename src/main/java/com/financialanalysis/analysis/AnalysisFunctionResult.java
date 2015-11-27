package com.financialanalysis.analysis;

import lombok.Data;

@Data
public class AnalysisFunctionResult {
    int beginIndex;
    int endIndex;

    // Macd
    double[] macd;
    double[] macdSignal;
    double[] macdHist;

    // Bollinger bands
    double[] bbMid;
    double[] bbHigh;
    double[] bbLow;

    //Ema
    double[] ema;

    //Adx
    double[] dmPlus;
    double[] dmMinus;
    double[] adx;

    //Zigzag
    double[] zigzag;

    //PVO
    double[] pvo;
    double[] pvoSignal;
    double[] pvoHist;
}
