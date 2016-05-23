package com.financialanalysis.reports;

import com.financialanalysis.data.Account;
import com.financialanalysis.strategy.StrategyOutput;
import lombok.extern.log4j.Log4j;

import java.util.List;
import java.util.stream.Collectors;

@Log4j
public class Reporter {

    public List<Report> generateReports(List<StrategyOutput> allResults) {
        return allResults.stream().map(s -> s.getCharts()).flatMap(c -> c.stream()).map(c -> new Report(c)).collect(Collectors.toList());
    }

    public String generateIndividualAccountSummary(List<StrategyOutput> allResults) {
        StringBuffer buf = new StringBuffer();

        allResults.forEach(output -> {
            String info = output.getAccount().getSummary();
            buf.append(info + "\n");
            log.info(info);
        });

        return buf.toString();
    }

    public String generateAverageAccountSummary(List<StrategyOutput> allResults) {
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
        if(num == 0) return "";

        StringBuffer buf = new StringBuffer();
        String a = String.format("Positive Return:                %.2f%%", posGain / num * 100);
        String b = String.format("Gain/Loss Ave:                  %.2f%%", gain/num);
        String c = String.format("Gain/Loss NOT -2%% to 2%% Ave:    %.2f%%", gainNotZero/numNotZero);
        String d = String.format("Number of meaningful trades:    %.2f%% %d/%d ", numNotZero * 100.0 / num, (int) numNotZero, (int) num);
        buf.append("\n" + a + "\n" + b + "\n" + c + "\n" + d + "\n");
        log.info(buf.toString());

        return buf.toString();
    }

    public void generateSmallSummary(List<StrategyOutput> allResults) {

    }
}
