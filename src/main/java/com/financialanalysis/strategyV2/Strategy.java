package com.financialanalysis.strategyV2;

import com.beust.jcommander.internal.Lists;
import com.financialanalysis.data.Account;
import com.financialanalysis.data.StockFA;
import com.financialanalysis.data.StockPrice;
import com.financialanalysis.strategy.StrategyOutput;
import lombok.extern.log4j.Log4j;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Optional;

import static com.financialanalysis.analysis.AnalysisTools.getClosingPrices;
import static com.financialanalysis.analysis.AnalysisTools.getDates;
import static com.financialanalysis.analysis.AnalysisTools.getHighPrices;
import static com.financialanalysis.analysis.AnalysisTools.getLowPrices;

@Log4j
public abstract class Strategy {
    private final Entry entry;
    private final Exit exit;
    private final Chart chart;

    private static final int MIN_DATA_POINTS = 55;

    public Strategy(Entry entry, Exit exit, Chart chart) {
        this.entry = entry;
        this.exit = exit;
        this.chart = chart;
    }

    public StrategyOutputV2 run(StrategyInput input) {
        StockFA stock = input.getStock();
        if(stock.getHistory().size() < MIN_DATA_POINTS) {
            return new StrategyOutputV2(stock.getSymbol(), Account.createDefaultAccount(), Optional.empty(), "S");
        }

        Account account = Account.createDefaultAccount();
        double[] closingPrices = getClosingPrices(stock.getHistory());
        double[] lowPrices = getLowPrices(stock.getHistory());
        double[] highPrices = getHighPrices(stock.getHistory());
        List<DateTime> dates = getDates(stock.getHistory());

        boolean bought = false;
        for(int i = MIN_DATA_POINTS; i < stock.getHistory().size(); i++) {
            List<StockPrice> upToDay = stock.getHistory().subList(0, i+1);
            if(!haveSufficientMovement(upToDay)) {
                continue;
            }

            EntryDecision decision = entry.getEntryDecision(upToDay);

            if(!bought && decision.isEntry()) {
                account.buyAll(closingPrices[i], dates.get(i), stock.getSymbol(), decision.getWeight());
                bought = true;
            }else if(bought && exit.shouldExit(upToDay)) {
                account.sellAll(closingPrices[i], dates.get(i), stock.getSymbol());
                bought = false;
            }
        }

        if(!account.getActivity().isEmpty()) {
            log.info(stock.getSymbol() + " has activity");
        }

        return new StrategyOutputV2(stock.getSymbol(), account, Optional.of(chart.getChart(stock, account)), "S");
    }

    /**
     * Look back until the start of the flag pole on a rolling 3 day window
     * 1) Closing prices must not be all the same
     * 2) Low and High prices must be different
     */
    private boolean haveSufficientMovement(List<StockPrice> upToDay) {
        double[] closingPrices = getClosingPrices(upToDay);
        double[] lowPrices = getLowPrices(upToDay);
        double[] highPrices = getHighPrices(upToDay);

        int startIndex = closingPrices.length - 1;
        int inSufficientMovementCount1 = 0;
        int inSufficientMovementCount2 = 0;
        for(int i = startIndex; i > startIndex - 5; i--) {
            boolean inSufficientMovement1 = closingPrices[i] == closingPrices[i- 1];
            boolean inSufficientMovement2 = lowPrices[i] == highPrices[i];

            if(inSufficientMovement1) {
                inSufficientMovementCount1++;
            }

            if(inSufficientMovement2) {
                inSufficientMovementCount2++;
            }
        }

        if(inSufficientMovementCount1 > 2) {
            return false;
        }

        if(inSufficientMovementCount2 > 2) {
            return false;
        }
        return true;
    }
}
