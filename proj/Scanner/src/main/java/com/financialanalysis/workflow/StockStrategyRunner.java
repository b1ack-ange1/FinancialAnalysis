package com.financialanalysis.workflow;

import com.financialanalysis.data.Account;
import com.financialanalysis.data.StockFA;
import com.financialanalysis.strategy.AMVStrategy;
import com.financialanalysis.strategy.BollingerBandsVolatilityStrategy;
import com.financialanalysis.strategy.SRStrategy;
import com.financialanalysis.strategy.StrategyOutput;
import com.financialanalysis.strategy.MACDStrategyOld;
import com.financialanalysis.strategy.MACDStrategy;
import com.financialanalysis.strategy.StrategyInput;
import com.financialanalysis.strategy.SqueezeStrategy;
import com.financialanalysis.strategy.FlagStrategy;
import com.google.inject.Inject;
import lombok.Setter;
import lombok.extern.log4j.Log4j;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.List;

import static com.financialanalysis.workflow.Analysis.addOutput;

@Log4j
public class StockStrategyRunner implements Runnable{
    private final Account account;
    private final MACDStrategyOld macdStrategyOld;
    private final MACDStrategy macdStrategy;
    private final SqueezeStrategy squeezeIndicator;
    private final BollingerBandsVolatilityStrategy bbVolIndicator;
    private final FlagStrategy triangleIndicator;
    private final SRStrategy srStrategy;
    private final AMVStrategy amvStrategy;

    private final int MONTHS = 24;

    @Setter private List<StockFA> stockFAs;

    @Inject
    public StockStrategyRunner(Account account) {
        this.account = account;
        this.macdStrategyOld = new MACDStrategyOld(account);
        this.macdStrategy = new MACDStrategy(account);
        this.squeezeIndicator = new SqueezeStrategy(account);
        this.bbVolIndicator = new BollingerBandsVolatilityStrategy(account);
        this.triangleIndicator = new FlagStrategy(account);
        this.srStrategy = new SRStrategy(account);
        this.amvStrategy = new AMVStrategy(account);
    }

    @Override
    public void run() {
        List<StrategyOutput> results = runStrategies();
        addOutput(results);
    }

    public List<StrategyOutput> runStrategies() {
        List<StrategyOutput> results = new ArrayList<>();
        DateTime startDate = DateTime.now(DateTimeZone.forID("America/Toronto"))
                .minusMonths(MONTHS)
                .withHourOfDay(0)
                .withMinuteOfHour(0);
        DateTime endDate = DateTime.now(DateTimeZone.forID("America/Toronto"))
                .withHourOfDay(0)
                .withMinuteOfHour(0);
        StrategyInput input = new StrategyInput(stockFAs, startDate, endDate, null);

        StrategyOutput triOutput = triangleIndicator.runStrategy(input);
        results.add(triOutput);

        return results;
    }


}
