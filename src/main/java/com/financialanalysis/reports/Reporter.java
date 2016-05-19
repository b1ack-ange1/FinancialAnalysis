package com.financialanalysis.reports;

import com.financialanalysis.store.ReportStore;
import com.financialanalysis.strategy.StrategyOutput;
import com.financialanalysis.workflow.StrategyRunner;
import com.google.inject.Inject;
import lombok.extern.log4j.Log4j;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.List;

@Log4j
public class Reporter {
    private final StrategyRunner strategyRunner;
    private final ReportStore reportStore;

    @Inject
    public Reporter(StrategyRunner strategyRunner, ReportStore reportStore) {
        this.strategyRunner = strategyRunner;
        this.reportStore = reportStore;
    }

    public void generateReports(List<StrategyOutput> allResults) {
        DateTime currentDate = DateTime.now(DateTimeZone.forID("America/Toronto"));

        allResults.forEach(output -> {
            if(!output.getAccount().getActivity().isEmpty()) {
                log.info(output.getAccount().getSummary());
                output.getCharts().forEach(chart -> chart.render());
//            reportStore.save(r.getCharts(), currentDate);
            }
        });
    }
}
