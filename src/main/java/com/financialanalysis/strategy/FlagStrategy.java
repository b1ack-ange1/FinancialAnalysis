package com.financialanalysis.strategy;

import com.financialanalysis.analysis.AnalysisBaseFunctions;
import com.financialanalysis.analysis.AnalysisFunctionResult;
import com.financialanalysis.analysis.AnalysisFunctions;
import com.financialanalysis.analysis.AnalysisTools;
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

import static com.financialanalysis.analysis.AnalysisBaseFunctions.*;
import static com.financialanalysis.analysis.AnalysisTools.*;
import static com.financialanalysis.workflow.Main.*;

@Log4j
public class FlagStrategy {
    //Not configurable
    private static final int MIN_DATA_POINTS = 100;

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
    private FlagConfig config;

    @SneakyThrows
    public StrategyOutput runStrategy(FlagStrategyInput input) {
        StockFA stock = input.getStock();
        Symbol symbol = input.getStock().getSymbol();

        validStockPrice = getValidStockPrices(stock.getHistory(), input.getStartDate(), input.getEndDate());

        if(validStockPrice.isEmpty() || validStockPrice.size() < MIN_DATA_POINTS) {
            return new StrategyOutput(symbol, Account.createDefaultAccount(), new ArrayList<>(), "Flag");
        }

        config = input.getConfig();

        closingPrices = getClosingPrices(validStockPrice);
        openPrices = getOpenPrices(validStockPrice);
        lowPrices = getLowPrices(validStockPrice);
        highPrices = getHighPrices(validStockPrice);
        volume = getVolume(validStockPrice);
        dates = getDates(validStockPrice);
        sma = sma(closingPrices, 100);

        AnalysisFunctionResult pvoResult = AnalysisFunctions.pvo(
                volume,
                config.getDefaultFastPeriod(),
                config.getDefaultSlowPeriod(),
                config.getDefaultSignalPeriod()
        );
        pvo = pvoResult.getPvo();
        pvoSignal = pvoResult.getPvoSignal();

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
        boolean accuracyLong = flagPoleSR.getRSquare() > config.getFlagPoleRSquareThreshold();
        boolean longSlope = config.getFlagPoleSlopeThreshold() < flagPoleSR.getSlope();
        boolean longTrend = accuracyLong && longSlope;

        // Ensure conditions for pattern are met
        boolean numTopData = highs.getN() >= config.getNumTopDataPoints();
        boolean numBotData = lows.getN() >= config.getNumBotDataPoints();
        boolean numData = numBotData && numTopData;

        boolean topSlope = config.getTrendTopSlopeThresholdLower() < highs.getSlope() && highs.getSlope() < config.getTrendTopSlopeThresholdHigher();
        boolean botSlope = config.getTrendBotSlopeThresholdLower() < lows.getSlope() && lows.getSlope() < config.getTrendBotSlopeThresholdHigher();
        boolean slopeDifference = Math.abs(lows.getSlope() - highs.getSlope()) < config.getTopBotSlopeDifferenceThreshold();
        boolean trendSlope = topSlope && botSlope && slopeDifference;
        boolean accuracy = lows.getRSquare() > config.getTrendRSquareThreshold() && highs.getRSquare() > config.getTrendRSquareThreshold();

        // Make sure the flagTop doesn't retrace more than 65% of the flagPole
        double flagPoleLowest = flagPoleLine.getYForX((double) startOfFlagPole);
        double flagPoleHighest = flagPoleLine.getYForX((double) endOfFlagPole);
        double flagPoleHeight = flagPoleHighest - flagPoleLowest;
        double minThresholdForLowestFlagTop = flagPoleLowest + (flagPoleHeight * 0.65);
        double flagTopLowest = lowsTrendLine.getYForX((double) i);
        boolean flagLowest = minThresholdForLowestFlagTop < flagTopLowest;

        boolean patternTrend = accuracy && trendSlope && numData && flagLowest;

        // Make sure there have been movement in the closing for the last 3 days
        boolean sufficientMovement = determineIfSufficientMovement(i, startOfFlagPole);

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
    private boolean determineIfSufficientMovement(int startIndex, int startOfFlagPole) {
        int inSufficientMovementCount1 = 0;
        int inSufficientMovementCount2 = 0;
        for(int i = startIndex; i > startOfFlagPole; i--) {
            boolean inSufficientMovement1 = closingPrices[i] == closingPrices[i- 1];
            boolean inSufficientMovement2 = lowPrices[i] == highPrices[i];

            if(inSufficientMovement1) {
                inSufficientMovementCount1++;
            }

            if(inSufficientMovement2) {
                inSufficientMovementCount2++;
            }
        }

        if(inSufficientMovementCount1 > config.getMaxInsufficientMovementCloseToPrevClose()) {
            return false;
        }

        if(inSufficientMovementCount2 > config.getMaxInsufficientMovementHighToLowCount()) {
            return false;
        }
        return true;
    }

    private Account determineLongPositions(List<Flag> flags, StockFA stock) {
        Account runAccount = Account.createDefaultAccount();
        runAccount.setSymbol(stock.getSymbol());
        for(Flag flag : flags) {
            DateTime triggerDay = flag.getTriggerDateTime();
            int triggerIndex = dates.indexOf(triggerDay);
            double buySellPrice = flag.getBuySellPrice();
            double maxTargetPrice = flag.getMaxTargetPrice();
            double targetSellPrice = lowPrices[triggerIndex] + (config.getPercentageMaxTarget() * (maxTargetPrice - lowPrices[triggerIndex]));
            double lastBuyPrice = -1.0;
            boolean bought = false;

            for (int i = triggerIndex + 1; i < closingPrices.length; i++) {
                // Check if we are still within period where it is okay to buy
                if (i <= triggerIndex + config.getFlagLookAheadDays()) {

                    double gapP = (openPrices[i] / closingPrices[i - 1]) - 1;
                    boolean gapIsOkay = false;
                    if (gapP <= config.getAllowableGapUp()) {
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

                // If its open below our bought price, exit immediately
                if (openPrices[i] < lastBuyPrice) {
                    runAccount.sellAll(openPrices[i], dates.get(i), stock.getSymbol());
                }

                //
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

    /**
     * Will find the best possible flagTop within a range looking backward starting at
     * startIndex
     */
    private FlagTop findBestFlagTop(int startIndex, Symbol symbol) {
        Map<Integer, Point> max = max(
                Arrays.copyOfRange(highPrices, 0, startIndex + 1),
                config.getMaxExtremaLookBackPeriod(),
                config.getMaxExtremaLookForwardPeriod()
                );

        Map<Integer, Point> min = min(
                Arrays.copyOfRange(lowPrices, 0, startIndex + 1),
                config.getMinExtremaLookBackPeriod(),
                config.getMinExtremaLookForwardPeriod()
        );

        Trend top = findTrend(max, startIndex, config.getMinFlagTopLen(), dates, symbol);
        Trend bot = findTrend(min, startIndex, config.getMinFlagTopLen(), dates, symbol);
        int flagTopLength = config.getMinFlagTopLen();
        for(int i = config.getMinFlagTopLen() + 1; i <= config.getMaxFlagTopLen(); i++) {
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
        int flagPoleLength = config.getMinFlagPoleLen();
        for(int i = config.getMinFlagPoleLen(); i <= config.getMaxFlagPoleLen(); i++) {
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

