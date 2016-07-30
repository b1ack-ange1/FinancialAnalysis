package com.financialanalysis.strategyV2;

import com.beust.jcommander.internal.Lists;
import com.financialanalysis.data.Account;
import com.financialanalysis.data.StockFA;
import com.financialanalysis.data.StockPrice;
import com.financialanalysis.graphing.StockChart;
import com.financialanalysis.strategy.StrategyOutput;
import lombok.extern.log4j.Log4j;
import org.joda.time.DateTime;

import java.util.List;

import static com.financialanalysis.analysis.AnalysisTools.getClosingPrices;
import static com.financialanalysis.analysis.AnalysisTools.getDates;

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

    public StrategyOutput run(StrategyInput input) {
        StockFA stock = input.getStock();
        if(stock.getHistory().size() < MIN_DATA_POINTS) {
            return new StrategyOutput(stock.getSymbol(), Account.createDefaultAccount(), Lists.newArrayList(), "S");
        }

        Account account = Account.createDefaultAccount();
        double[] closingPrices = getClosingPrices(stock.getHistory());
        List<DateTime> dates = getDates(stock.getHistory());

        boolean bought = false;
        for(int i = MIN_DATA_POINTS; i < stock.getHistory().size(); i++) {
            List<StockPrice> upToDay = stock.getHistory().subList(0, i+1);

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

        return new StrategyOutput(stock.getSymbol(), account, chart.getCharts(stock, account), "S");
    }
}
