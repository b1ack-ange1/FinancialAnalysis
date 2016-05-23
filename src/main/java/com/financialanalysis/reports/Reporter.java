package com.financialanalysis.reports;

import com.financialanalysis.data.Account;
import com.financialanalysis.graphing.StockChart;
import com.financialanalysis.store.ReportStore;
import com.financialanalysis.strategy.StrategyOutput;
import com.financialanalysis.workflow.StrategyRunner;
import com.google.inject.Inject;
import lombok.extern.log4j.Log4j;

import java.util.List;
import java.util.stream.Collectors;

@Log4j
public class Reporter {
    private final StrategyRunner strategyRunner;
    private final ReportStore reportStore;

    @Inject
    public Reporter(StrategyRunner strategyRunner, ReportStore reportStore) {
        this.strategyRunner = strategyRunner;
        this.reportStore = reportStore;
    }

    public List<Report> generateReports(List<StrategyOutput> allResults) {
        return allResults.stream().map(s -> s.getCharts()).flatMap(c -> c.stream()).map(c -> new Report(c)).collect(Collectors.toList());
    }

    public void generateIndividualAccountSummary(List<StrategyOutput> allResults) {

        allResults.forEach(output -> {
            log.info(output.getAccount().getSummary());
//            output.getCharts().forEach(chart -> chart.render());
//            reportStore.save(r.getCharts(), currentDate);
        });
    }

    public void generateAverageAccountSummary(List<StrategyOutput> allResults) {
        List<Account> accounts = allResults.stream().map(StrategyOutput::getAccount).collect(Collectors.toList());
        double gainNotZero = 0;
        double numNotZero = 0;

        double gain = 0;
        double num = 0;
        double posGain = 0;
        for(Account account : accounts) {
            double gainLossP = account.getPercentageGainLoss();

            if(account.getActivity().isEmpty()) continue;
            if(gainLossP > 0) posGain++;
            gain += gainLossP;
            num++;

            if(gainLossP < -2.0 || 2.0 < gainLossP) {
                gainNotZero += gainLossP;
                numNotZero++;
            }
        }
        if(num == 0) return;
        log.info(String.format("Positive Return:                %.2f%%", posGain / num * 100));
        log.info(String.format("Gain/Loss Ave:                  %.2f%%", gain/num));
        log.info(String.format("Gain/Loss NOT -2%% to 2%% Ave:    %.2f%%", gainNotZero/numNotZero));
        log.info(String.format("Number of meaningful trades:    %.2f%% %d/%d ", numNotZero * 100.0 / num, (int) numNotZero, (int) num));

    }

    public void generateSmallSummary(List<StrategyOutput> allResults) {

    }
}
