package com.financialanalysis.reports;

import com.financialanalysis.data.Symbol;
import com.financialanalysis.graphing.StockChart;
import com.financialanalysis.store.ReportStore;
import com.financialanalysis.strategy.AbstractStrategy;
import com.financialanalysis.strategy.StrategyOutput;
import com.financialanalysis.workflow.StrategyRunner;
import com.google.inject.Inject;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class Reporter {
    private final StrategyRunner strategyRunner; //TODO: This needs to be a singleton
    private final ReportStore reportStore;

    @Inject
    public Reporter(StrategyRunner strategyRunner, ReportStore reportStore) {
        this.strategyRunner = strategyRunner;
        this.reportStore = reportStore;
    }

    public void generateReports() {
        DateTime currentDate = DateTime.now(DateTimeZone.forID("America/Toronto"));
        Map<AbstractStrategy, List<StrategyOutput>> allResults = strategyRunner.getAllResults();
        Set<AbstractStrategy> strategies = allResults.keySet();

        for(AbstractStrategy strategy : strategies) {
            List<StrategyOutput> outputs = allResults.get(strategy);
            if(outputs != null && !outputs.isEmpty()) {

                for(StrategyOutput output : outputs) {
                    Set<Symbol> symbols = output.getCharts().keySet();
                    if(symbols != null && !symbols.isEmpty()) {

                        for(Symbol symbol : symbols) {
                            List<StockChart> charts = output.getCharts().get(symbol);
//                            for(StockChart chart : charts) {
//                                chart.render();
//                            }
                            reportStore.save(symbol, charts, currentDate);
                        }
                    }
                }
            }
        }
    }
}
