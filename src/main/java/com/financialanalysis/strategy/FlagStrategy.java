package com.financialanalysis.strategy;

import com.financialanalysis.data.Action;
import com.financialanalysis.data.StockFA;
import com.financialanalysis.data.StockPrice;
import com.financialanalysis.data.Symbol;
import com.financialanalysis.data.Trend;
import com.financialanalysis.graphing.StockChart;
import com.financialanalysis.graphing.Line;
import com.financialanalysis.graphing.Point;
import com.financialanalysis.data.Account;
import com.google.common.collect.Lists;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.financialanalysis.analysis.AnalysisBaseFunctions.findTrend;
import static com.financialanalysis.analysis.AnalysisBaseFunctions.max;
import static com.financialanalysis.analysis.AnalysisBaseFunctions.min;
import static com.financialanalysis.analysis.AnalysisBaseFunctions.sma;
import static com.financialanalysis.analysis.AnalysisTools.getClosingPrices;
import static com.financialanalysis.analysis.AnalysisTools.getDates;
import static com.financialanalysis.analysis.AnalysisTools.getHighPrices;
import static com.financialanalysis.analysis.AnalysisTools.getLowPrices;
import static com.financialanalysis.analysis.AnalysisTools.getOpenPrices;
import static com.financialanalysis.analysis.AnalysisTools.getValidStockPrices;
import static com.financialanalysis.analysis.AnalysisTools.getVolume;
import static com.financialanalysis.analysis.AnalysisTools.round;
import static com.financialanalysis.workflow.Main.*;

@Log4j
public class FlagStrategy extends AbstractStrategy {
    private static final int MIN_FLAG_TOP_LEN = 5;
    private static final int MAX_FLAG_TOP_LEN = 30;
    private static final int MIN_FLAG_POLE_LEN = 5;
    private static final int MAX_FLAG_POEL_LEN = 60;

    private static final int FLAG_LOOK_AHEAD_DAYS = 8;
    private static final double PERCENTAGE_MAX_TARGET = 0.65;

    private static final int MIN_DATA_POINTS = 100;

    double longTrendSlopeThreshold = 0.0;
    double longTrendRSquareThreshold = 0.8;
    double trendBotSlopeThresholdLower = -1.0;
    double trendBotSlopeThresholdHigher = 0.0;
    double trendTopSlopeThresholdLower = -1.0;
    double trendTopSlopeThresholdHigher = 0.0;
    double trendRSquareThreshold = 0.8;
    double topBotSlopeDifferenceThreshold = 0.03;
    int numTopDataPoints = 3;
    int numBotDataPoints = 2;

    private double[] closingPrices;
    private double[] openPrices;
    private double[] lowPrices;
    private double[] highPrices;
    private double[] volume;
    private double[] pvo;
    private double[] pvoSignal;
    private double[] sma;
    private List<StockPrice> validStockPrice;
    private List<DateTime> dates;

    @Override
    @SneakyThrows
    public StrategyOutput runStrategy(StrategyInput input) {
        StockFA stock = input.getStock();
        Symbol symbol = input.getStock().getSymbol();

        validStockPrice = getValidStockPrices(stock.getHistory(), input.getStartDate(), input.getEndDate());
        if(validStockPrice.isEmpty() || validStockPrice.size() < MIN_DATA_POINTS) {
            return new StrategyOutput(symbol, Account.createDefaultAccount(), new ArrayList<>(), "Flag");
        }

        closingPrices = getClosingPrices(validStockPrice);
        openPrices = getOpenPrices(validStockPrice);
        lowPrices = getLowPrices(validStockPrice);
        highPrices = getHighPrices(validStockPrice);
        volume = getVolume(validStockPrice);
        dates = getDates(validStockPrice);
        sma = sma(closingPrices, 100);

        if(runStrategies) {
            // Find flag for most recent day
            Optional<Flag> flag = findFlagForDay(closingPrices.length - 1, stock);
            if(flag.isPresent()) {
                log.info(symbol.getSymbol() + " has a flag.");
                return new StrategyOutput(symbol, Account.createDefaultAccount(), Lists.newArrayList(flag.get().getFlagStockChart()), "Flag");
            }
        }

        if(backtest && !runStrategies) {
            // Find the flags
            List<Flag> flagPatterns = findFlagPatterns(stock);

            // Given these flags, buy and sell on them
            Account runAccount = determineLongPositions(flagPatterns, stock);

            // If this stock generated a buy signal, then lets report it
            if(runAccount.getActivity().size() > 0) {
                List<StockChart> flagCharts = flagPatterns.stream().map(f -> f.getFlagStockChart()).collect(Collectors.toList());
                log.info(stock.getSymbol().getSymbol() + " found " + runAccount.getActivity().size() + " transactions.");

                return new StrategyOutput(symbol, runAccount, flagCharts, "Flag");
            }
        }

        return new StrategyOutput(symbol, Account.createDefaultAccount(), new ArrayList<>(), "Flag");
    }

    @SneakyThrows
    private List<Flag> findFlagPatterns(StockFA stock) {
        List<Flag> flagPatterns = Lists.newArrayList();
        for(int i = closingPrices.length - 1; i >= 0 && i >= MIN_DATA_POINTS; i--) {
            Optional<Flag> flag = findFlagForDay(i, stock);
            if(flag.isPresent()) {
                flagPatterns.add(flag.get());
            }
        }
        // Reverse the list so it's in chronological order
        return Lists.reverse(flagPatterns);
    }

    private Optional<Flag> findFlagForDay(int dayIndex, StockFA stock) {
        int i = dayIndex;

        // Determine the flag top first
        FlagTop flagTop = findBestFlagTop(i, stock.getSymbol());
        Trend topTrend = flagTop.getTopTrend();
        Trend botTrend = flagTop.getBotTrend();
        int flagTopLength = flagTop.getFlagTopLength();

        // If we don't have an RSquared value, then skip
        if(Double.isNaN(topTrend.getSimpleRegression().getRSquare())|| Double.isNaN(botTrend.getSimpleRegression().getRSquare())) {
            Optional.empty();
        }

        // Build the flag top lines
        SimpleRegression lows = botTrend.getSimpleRegression();
        SimpleRegression highs = topTrend.getSimpleRegression();
        Line lowsTrendLine = new Line(lows);
        Line highsTrendLine = new Line(highs);

        int endOfFlagPole = i - flagTopLength;
        FlagPole flagPole = findBestFlagPole(endOfFlagPole);
        SimpleRegression flagPoleSR = flagPole.getFlagPole();

        // If we don't have an RSquared value, then skip
        if(Double.isNaN(flagPoleSR.getRSquare())) {
            Optional.empty();
        }

        // Build the pole line
        int flagPoleLength = flagPole.getFlagPoleLength();
        int startOfFlagPole = endOfFlagPole - flagPoleLength;
        Line flagPoleLine = new Line(flagPoleSR);

        // Build the project price line, should continue with same slope and distance as flag pole
        Line projectedPriceLine = new Line(lowsTrendLine.getPointForX(i), flagPoleLine.getSlope());

        // If the projectPriceLine is never above buy/sell line, then ignore because no chance for profits
        boolean profit = projectedPriceLine.getYForX(i + flagPoleLength) > highsTrendLine.getYForX(i);

        // If the magnitude of the slope of the flagTop is greater than flagPole, skip
        if(Math.abs(topTrend.getSimpleRegression().getSlope()) > Math.abs(flagPoleSR.getSlope())) {
            Optional.empty();
        }

        // Ensure conditions for long trend are met
        boolean accuracyLong = flagPoleSR.getRSquare() > longTrendRSquareThreshold;
        boolean longSlope = longTrendSlopeThreshold < flagPoleSR.getSlope();
        boolean longTrend = accuracyLong && longSlope;

        // Ensure conditions for pattern are met
        boolean numTopData = highs.getN() >= numTopDataPoints;
        boolean numBotData = lows.getN() >= numBotDataPoints;
        boolean numData = numBotData && numTopData;

        boolean topSlope = trendTopSlopeThresholdLower < highs.getSlope() && highs.getSlope() < trendTopSlopeThresholdHigher;
        boolean botSlope = trendBotSlopeThresholdLower < lows.getSlope() && lows.getSlope() < trendBotSlopeThresholdHigher;
        boolean slopeDifference = Math.abs(lows.getSlope() - highs.getSlope()) < topBotSlopeDifferenceThreshold;
        boolean trendSlope = topSlope && botSlope && slopeDifference;
        boolean accuracy = lows.getRSquare() > trendRSquareThreshold && highs.getRSquare() > trendRSquareThreshold;

        // Make sure the flagTop doesn't retrace more than 65% of the flagPole
        double flagPoleLowest = flagPoleLine.getYForX((double) startOfFlagPole);
        double flagPoleHighest = flagPoleLine.getYForX((double) endOfFlagPole);
        double flagPoleHeight = flagPoleHighest - flagPoleLowest;
        double minThresholdForLowestFlagTop = flagPoleLowest + (flagPoleHeight * 0.65);
        double flagTopLowest = lowsTrendLine.getYForX((double) i);
        boolean flagLowest = minThresholdForLowestFlagTop < flagTopLowest;

        boolean patternTrend = accuracy && trendSlope && numData && flagLowest;

        // Make sure there have been movement in the closing for the last 3 days
        boolean sufficientMovement = determineIfSufficientMovement(i, startOfFlagPole, stock.getSymbol());

        if(patternTrend && longTrend && profit && sufficientMovement) {
            String info = String.format("%s_%s", stock.getSymbol(), dates.get(i).toString().split("T")[0]);
            StockChart stockChart = new StockChart("Flag_" + info);
            stockChart.setYAxis("Price");
            stockChart.setXAxis("Date");
            stockChart.addCandles(validStockPrice);
            stockChart.addVolume(dates, volume);
            stockChart.addXYLine(
                    dates,
                    lowsTrendLine.generateXValues(i - flagTopLength, i),
                    lowsTrendLine.generateYValues(i - flagTopLength, i),
                    "Bot-" + i + "-" + flagTopLength + "-[" + round(lowsTrendLine.getSlope(), 2) + "]"
            );
            stockChart.addXYLine(
                    dates,
                    highsTrendLine.generateXValues(i - flagTopLength, i),
                    highsTrendLine.generateYValues(i - flagTopLength, i),
                    "Top-" + i + "-" + flagTopLength + "-[" + round(highsTrendLine.getSlope(), 2) + "]"
            );
            stockChart.addXYLine(
                    dates,
                    flagPoleLine.generateXValues(startOfFlagPole, endOfFlagPole),
                    flagPoleLine.generateYValues(startOfFlagPole, endOfFlagPole),
                    "Pole-" + i + "-" + flagPoleLength + "-" + round(flagPoleLine.getSlope(), 2)
            );
            stockChart.addXYLine(
                    dates,
                    projectedPriceLine.generateXValues(i, i + flagPoleLength),
                    projectedPriceLine.generateYValues(i, i + flagPoleLength),
                    "Projected-" + i + "-" + flagPoleLength
            );
            stockChart.addVerticalLine(dates.get(i), "T ", null);
            stockChart.addHorizontalLine(highsTrendLine.getYForX(i), "Buy/Sell");

            Flag flag = new Flag(
                    flagTop,
                    flagPole,
                    projectedPriceLine.getYForX(i + flagPoleLength),
                    highsTrendLine.getYForX(i),
                    stockChart,
                    projectedPriceLine
            );

            return Optional.of(flag);
        }

        return Optional.empty();
    }

    /**
     * Look back until the start of the flag pole on a rolling 3 day window
     * 1) Closing prices must not be all the same
     * 2) Open and closing prices must be different
     * 3) ?? Volume must be greater than ??
     * @return
     */
    private boolean determineIfSufficientMovement(int startIndex, int startOfFlagPole, Symbol symbol) {
        int inSufficientMovementCount = 0;
        int maxInsufficientCount = 3;
        for(int i = startIndex; i > startOfFlagPole; i--) {
            boolean inSufficientMovement1 = closingPrices[i] == closingPrices[i- 1];
//            boolean inSufficientMovement2 = closingPrices[i] == openPrices[i];
            boolean inSufficientMovement2 = lowPrices[i] == highPrices[i];

            if(inSufficientMovement1 || inSufficientMovement2) {
                inSufficientMovementCount++;
            }

        }
        if(inSufficientMovementCount > maxInsufficientCount) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Will find the best possible flagTop within a range looking backward starting at
     * startIndex
     */
    private FlagTop findBestFlagTop(int startIndex, Symbol symbol) {
        Map<Integer, Point> max = max(Arrays.copyOfRange(highPrices, 0, startIndex + 1), 3, 3);
        Map<Integer, Point> min = min(Arrays.copyOfRange(lowPrices, 0, startIndex + 1), 3, 3);

        Trend top = findTrend(max, startIndex, MIN_FLAG_TOP_LEN, dates, symbol);
        Trend bot = findTrend(min, startIndex, MIN_FLAG_TOP_LEN, dates, symbol);
        int flagTopLength = MIN_FLAG_TOP_LEN;
        for(int i = MIN_FLAG_TOP_LEN+1; i <= MAX_FLAG_TOP_LEN; i++) {
            Trend topTmp = findTrend(max, startIndex, i, dates, symbol);
            Trend botTmp = findTrend(min, startIndex, i, dates, symbol);

            if(Double.isNaN(top.getSimpleRegression().getRSquare()) || Double.isNaN(bot.getSimpleRegression().getRSquare())) {
                top = topTmp;
                bot = botTmp;
                flagTopLength = i;
            } else if(topTmp.getSimpleRegression().getRSquare() > top.getSimpleRegression().getRSquare() &&
                    botTmp.getSimpleRegression().getRSquare() > bot.getSimpleRegression().getRSquare()) {
                top = topTmp;
                bot = botTmp;
                flagTopLength = i;
            }
        }

        FlagTop flagTop = new FlagTop(top, bot, flagTopLength, dates.get(startIndex - flagTopLength), dates.get(startIndex));
        return flagTop;
    }

    /**
     * Will find best possible flagPole within range looking backward starting at
     * startIndex
     */
    private FlagPole findBestFlagPole(int startIndex) {
        SimpleRegression pole = null;
        int flagPoleLength = MIN_FLAG_POLE_LEN;
        for(int i = MIN_FLAG_POLE_LEN; i <= MAX_FLAG_POEL_LEN; i++) {
            SimpleRegression tmp = getSR(startIndex, i);

            if(pole == null) {
                pole = tmp;
                flagPoleLength = i;
            } else if(Double.isNaN(pole.getRSquare())) {
                pole = tmp;
                flagPoleLength = i;
            } else if(tmp.getN() > pole.getN() && tmp.getRSquare() > pole.getRSquare()) {
                pole = tmp;
                flagPoleLength = i;
            }
        }

        return new FlagPole(pole, flagPoleLength, dates.get(startIndex - flagPoleLength), dates.get(startIndex));
    }

    private SimpleRegression getSR(int startIndex, int len) {
        /**
         * Gives lower return at better chance
         */
//        Map<Integer, Point> min = min(Arrays.copyOfRange(lowPrices, 0, startIndex + 1), 3, 3);
//        Trend trend = findTrend(min, startIndex, len, dates);
//        return trend.getSimpleRegression();

        /**
         * Gives better return at lower chance
         */
        SimpleRegression sr = new SimpleRegression();
        for(int i = startIndex; i >= 0 && i >= startIndex - len; i--) {
            sr.addData(i, closingPrices[i]);
        }
        return sr;
    }

    private Account determineLongPositions(List<Flag> flags, StockFA stock) {
        Account runAccount = Account.createDefaultAccount();
        runAccount.setSymbol(stock.getSymbol());
        for(Flag flag : flags) {
            DateTime triggerDay = flag.getTriggerDateTime();
            int triggerIndex = dates.indexOf(triggerDay);
            double buySellPrice = flag.getBuySellPrice();
            double maxTargetPrice = flag.getMaxTargetPrice();
            double targetSellPrice = lowPrices[triggerIndex] + (PERCENTAGE_MAX_TARGET * (maxTargetPrice - lowPrices[triggerIndex]));
            double lastBuyPrice = -1.0;
            boolean bought = false;

            for (int i = triggerIndex; i < closingPrices.length; i++) {
                // Check if we are still within period where it is okay to buy
                if (i <= triggerIndex + FLAG_LOOK_AHEAD_DAYS) {
                    // If there was more than 5% previous gap up, then don't buy
                    double gapP = (openPrices[i] / closingPrices[i - 1]) - 1;
                    boolean gapIsOkay = false;
                    if (gapP <= 0.05) {
                        gapIsOkay = true;
                    }

                    double projectedLinePrice = flag.getProjectPriceLine().getYForX((double) i);

                    // If we see a buy signal, automatically buy
                    if (closingPrices[i] > buySellPrice && closingPrices[i] > projectedLinePrice && !bought && gapIsOkay) {
                        runAccount.buyAll(closingPrices[i], dates.get(i), stock.getSymbol());
                        lastBuyPrice = closingPrices[i];
                        bought = true;
                        continue;
                    }
                }

                if (openPrices[i] < lastBuyPrice) {
                    runAccount.sellAll(openPrices[i], dates.get(i), stock.getSymbol());
                }

                if (openPrices[i] >= closingPrices[i] && closingPrices[i] < lastBuyPrice) {
                    runAccount.sellAll(lastBuyPrice, dates.get(i), stock.getSymbol());
                }

                // If there are any profitable sales, sell
                // or if the closing price drops below the projected price line, sell
                if (closingPrices[i] > targetSellPrice || closingPrices[i] < flag.getProjectPriceLine().getYForX((double) i)) {
                    runAccount.sellAll(closingPrices[i], dates.get(i), stock.getSymbol());
                    bought = false;
                }
            }

            StockChart stockChart = flag.getFlagStockChart();
            stockChart.addHorizontalLine(targetSellPrice, "Target-" + ((int) (targetSellPrice * 100.0)) / 100);
            stockChart.addXYLine(dates, sma, "100-SMA");

            if (!runAccount.getActivity().isEmpty()) {
                stockChart.setGainLoss(String.format("[%.2f%%]", runAccount.getPercentageGainLoss()));
                stockChart.setNumDays(String.format("%d", runAccount.getDayBetweenFirstAndLastAction()));

                for (Action action : runAccount.getActivity()) {
                    int actionIndex = dates.indexOf(action.getDate());
                    int price = (int) (action.getPrice() * 100.0);
                    stockChart.addVerticalLine(dates.get(actionIndex), String.format("%.2f", price / 100.0), action);
                }
            }
        }
        return runAccount;
    }

    @Data
    private class FlagTop {
        private final Trend topTrend;
        private final Trend botTrend;
        private final int flagTopLength;
        private final DateTime start;
        private final DateTime end;
    }

    @Data
    private class FlagPole {
        private final SimpleRegression flagPole;
        private final int flagPoleLength;
        private final DateTime start;
        private final DateTime end;
    }

    @Data
    private class Flag {
        private final FlagTop flagTop;
        private final FlagPole flagPole;
        private final double maxTargetPrice;
        private final double buySellPrice;
        private final StockChart flagStockChart;
        private final Line projectPriceLine;

        public DateTime getTriggerDateTime() {
            return flagTop.getEnd();
        }
    }
}

/*
        PatternParameters params = new PatternParameters();
        params.setLongTrendSlopeThreshold(0.0);
        params.setLongTrendRSquareThreshold(0.8);
        params.setTrendBotSlopeThresholdLower(-1.0);
        params.setTrendBotSlopeThresholdHigher(0.0);
        params.setTrendTopSlopeThresholdLower(-1.0);
        params.setTrendTopSlopeThresholdHigher(0.0);
        params.setTopBotSlopeDifferenceThreshold(0.03);
        params.setTrendRSquareThreshold(0.8);
        params.setNumTopDataPoints(3);
        params.setNumBotDataPoints(2);

        AnalysisFunctionResult pvoResult = pvo(volume, DEFAULT_FAST_PERIOD, DEFAULT_SLOW_PERIOD, DEFAULT_SIGNAL_PERIOD);
        pvo = pvoResult.getPvo();
        pvoSignal = pvoResult.getPvoSignal();
 */

//            // Determine the flag top first
//            FlagTop flagTop = findBestFlagTop(i, stock.getSymbol());
//            Trend topTrend = flagTop.getTopTrend();
//            Trend botTrend = flagTop.getBotTrend();
//            int flagTopLength = flagTop.getFlagTopLength();
//
//            // If we don't have an RSquared value, then skip
//            if(Double.isNaN(topTrend.getSimpleRegression().getRSquare())|| Double.isNaN(botTrend.getSimpleRegression().getRSquare())) {
//                continue;
//            }
//
//            // Build the flag top lines
//            SimpleRegression lows = botTrend.getSimpleRegression();
//            SimpleRegression highs = topTrend.getSimpleRegression();
//            Line lowsTrendLine = new Line(lows);
//            Line highsTrendLine = new Line(highs);
//
//            int endOfFlagPole = i - flagTopLength;
//            FlagPole flagPole = findBestFlagPole(endOfFlagPole);
//            SimpleRegression flagPoleSR = flagPole.getFlagPole();
//
//            // If we don't have an RSquared value, then skip
//            if(Double.isNaN(flagPoleSR.getRSquare())) {
//                continue;
//            }
//
//            // Build the pole line
//            int flagPoleLength = flagPole.getFlagPoleLength();
//            int startOfFlagPole = endOfFlagPole - flagPoleLength;
//            Line flagPoleLine = new Line(flagPoleSR);
//
//            // Build the project price line, should continue with same slope and distance as flag pole
//            Line projectedPriceLine = new Line(lowsTrendLine.getPointForX(i), flagPoleLine.getSlope());
//
//            // If the projectPriceLine is never above buy/sell line, then ignore because no chance for profits
//            boolean profit = projectedPriceLine.getYForX(i + flagPoleLength) > highsTrendLine.getYForX(i);
//
//            // If the magnitude of the slope of the flagTop is greater than flagPole, skip
//            if(Math.abs(topTrend.getSimpleRegression().getSlope()) > Math.abs(flagPoleSR.getSlope())) {
//                continue;
//            }
//
//            // Ensure conditions for long trend are met
//            boolean accuracyLong = flagPoleSR.getRSquare() > longTrendRSquareThreshold;
//            boolean longSlope = longTrendSlopeThreshold < flagPoleSR.getSlope();
//            boolean longTrend = accuracyLong && longSlope;
//
//            // Ensure conditions for pattern are met
//            boolean numTopData = highs.getN() >= numTopDataPoints;
//            boolean numBotData = lows.getN() >= numBotDataPoints;
//            boolean numData = numBotData && numTopData;
//
//            boolean topSlope = trendTopSlopeThresholdLower < highs.getSlope() && highs.getSlope() < trendTopSlopeThresholdHigher;
//            boolean botSlope = trendBotSlopeThresholdLower < lows.getSlope() && lows.getSlope() < trendBotSlopeThresholdHigher;
//            boolean slopeDifference = Math.abs(lows.getSlope() - highs.getSlope()) < topBotSlopeDifferenceThreshold;
//            boolean trendSlope = topSlope && botSlope && slopeDifference;
//            boolean accuracy = lows.getRSquare() > trendRSquareThreshold && highs.getRSquare() > trendRSquareThreshold;
//
//            // Make sure the flagTop doesn't retrace more than 65% of the flagPole
//            double flagPoleLowest = flagPoleLine.getYForX((double) startOfFlagPole);
//            double flagPoleHighest = flagPoleLine.getYForX((double) endOfFlagPole);
//            double flagPoleHeight = flagPoleHighest - flagPoleLowest;
//            double minThresholdForLowestFlagTop = flagPoleLowest + (flagPoleHeight * 0.65);
//            double flagTopLowest = lowsTrendLine.getYForX((double) i);
//            boolean flagLowest = minThresholdForLowestFlagTop < flagTopLowest;
//
//            boolean patternTrend = accuracy && trendSlope && numData && flagLowest;
//
//            // Make sure there have been movement in the closing for the last 3 days
//            boolean sufficientMovement = determineIfSufficientMovement(i, startOfFlagPole, stock.getSymbol());
//
//            if(patternTrend && longTrend && profit && sufficientMovement) {
//                String info = String.format("%s_%s", stock.getSymbol(), dates.get(i).toString().split("T")[0]);
//                StockChart stockChart = new StockChart("Flag_" + info);
//                stockChart.setYAxis("Price");
//                stockChart.setXAxis("Date");
//                stockChart.addCandles(validStockPrice);
//                stockChart.addVolume(dates, volume);
//                stockChart.addXYLine(
//                        dates,
//                        lowsTrendLine.generateXValues(i - flagTopLength, i),
//                        lowsTrendLine.generateYValues(i - flagTopLength, i),
//                        "Bot-" + i + "-" + flagTopLength + "-[" + round(lowsTrendLine.getSlope(), 2) + "]"
//                );
//                stockChart.addXYLine(
//                        dates,
//                        highsTrendLine.generateXValues(i - flagTopLength, i),
//                        highsTrendLine.generateYValues(i - flagTopLength, i),
//                        "Top-" + i + "-" + flagTopLength + "-[" + round(highsTrendLine.getSlope(), 2) + "]"
//                );
//                stockChart.addXYLine(
//                        dates,
//                        flagPoleLine.generateXValues(startOfFlagPole, endOfFlagPole),
//                        flagPoleLine.generateYValues(startOfFlagPole, endOfFlagPole),
//                        "Pole-" + i + "-" + flagPoleLength + "-" + round(flagPoleLine.getSlope(), 2)
//                );
//                stockChart.addXYLine(
//                        dates,
//                        projectedPriceLine.generateXValues(i, i + flagPoleLength),
//                        projectedPriceLine.generateYValues(i, i + flagPoleLength),
//                        "Projected-" + i + "-" + flagPoleLength
//                );
//                stockChart.addVerticalLine(dates.get(i), "T ", null);
//                stockChart.addHorizontalLine(highsTrendLine.getYForX(i), "Buy/Sell");
//
//                Flag flag = new Flag(
//                        flagTop,
//                        flagPole,
//                        projectedPriceLine.getYForX(i + flagPoleLength),
//                        highsTrendLine.getYForX(i),
//                        stockChart,
//                        projectedPriceLine
//                );
//                flagPatterns.add(flag);
//
//                i -= flagTopLength;
//            }