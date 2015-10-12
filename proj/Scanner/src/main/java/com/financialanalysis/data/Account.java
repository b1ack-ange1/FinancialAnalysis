package com.financialanalysis.data;

import com.google.gson.Gson;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j;
import org.joda.time.DateTime;
import org.joda.time.Days;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Log4j
@Data
public class Account {
    private final double initialBalance;

    private double cashBalance;
    private double totalBalance;
    private double numShares;
    private double percentageGainLoss;
    private int numBuys;
    private int numSells;
    private int numTrades;private Symbol symbol;
    private List<Action> activity;

    private static int DEFAULT_COMMISSION_FEE = 7;

    public Account() {
        double cashBalance = 10000;
        this.cashBalance = cashBalance;
        this.totalBalance = cashBalance;
        this.numShares = 0;
        this.percentageGainLoss = 0;
        this.initialBalance = cashBalance;
        numBuys = 0;
        numSells = 0;
        numTrades = 0;
        activity = new ArrayList<>();
    }

    public void buyAll(double askPrice, DateTime date, Symbol symbol) {
        buy( (cashBalance - DEFAULT_COMMISSION_FEE)/askPrice, askPrice, date, symbol);
    }

    public void sellAll(double bidPrice, DateTime date, Symbol symbol) {
        sell(numShares, bidPrice, date, symbol);
    }

    public void buy(double numSharesToBuy, double askPrice, DateTime date, Symbol symbol) {
        if(numSharesToBuy > 0) {
            double cost = (numSharesToBuy * askPrice) + DEFAULT_COMMISSION_FEE;
            if(cost <= cashBalance) {

                // Buy the shares
                numShares += numSharesToBuy;
                cashBalance -= cost;
                totalBalance = cost + cashBalance;
                rebalance(askPrice);
                activity.add(new Action("buy", numSharesToBuy * askPrice, numSharesToBuy, askPrice, date, symbol));
                numBuys++;
            }
        }
    }

    public void sell(double numShareToSell, double bidPrice, DateTime date, Symbol symbol) {
        if(0 < numShareToSell && numShareToSell <= numShares) {
            double earning = (numShareToSell * bidPrice) - DEFAULT_COMMISSION_FEE;

            // Sell the shares
            numShares -= numShareToSell;
            cashBalance += earning;
            totalBalance = earning + cashBalance;
            rebalance(bidPrice);
            activity.add(new Action("sell", numShareToSell * bidPrice, numShareToSell, bidPrice, date, symbol));
            numSells++;
        }
    }

    public void rebalance(double currentPrice) {
        totalBalance = (currentPrice * numShares) + cashBalance;
        percentageGainLoss = ((totalBalance/initialBalance) - 1) * 100;
        numTrades = numBuys + numSells;
    }

    public int getDayBetweenFirstAndLastAction() {
        if(activity.isEmpty()) return 0;

        DateTime start = activity.get(0).getDate();
        DateTime end = activity.get(activity.size()-1).getDate();
        int num = Days.daysBetween(start.withTimeAtStartOfDay(), end.withTimeAtStartOfDay()).getDays();

        return num;
    }

    public static Account createDefaultAccount() {
        return new Account();
    }

    public Account copy() {
        return new Account();
    }

    public String getJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public String getSummary() {
        StringBuilder builder = new StringBuilder();
        builder.append("\n----------------------------\n");
        builder.append(String.format("Initial Balance     : %8.2f\n", initialBalance));
        builder.append(String.format("Cash Balance        : %8.2f\n", cashBalance));
        builder.append(String.format("Total Balance       : %8.2f\n", totalBalance));
        builder.append(String.format("Number Shares       : %8.2f\n", numShares));
        builder.append(String.format("Percentage Gain/Loss: %8.2f%%\n", percentageGainLoss));
        builder.append(String.format("Number Buys         : %8d\n", numBuys));
        builder.append(String.format("Number Sells        : %8d\n", numSells));
        builder.append(String.format("Number Trader       : %8d\n", numTrades));
        builder.append(String.format("Commission Paid     : %8d\n", (int)numTrades*DEFAULT_COMMISSION_FEE));
        builder.append(String.format("Symbol              : %8s\n", symbol));
        return builder.toString();
    }

    public String getAll() {
        StringBuilder builder = new StringBuilder();
        builder.append("\n----------------------------\n");
        builder.append(String.format("Initial Balance     : %8.2f\n", initialBalance));
        builder.append(String.format("Cash Balance        : %8.2f\n", cashBalance));
        builder.append(String.format("Total Balance       : %8.2f\n", totalBalance));
        builder.append(String.format("Number Shares       : %8.2f\n", numShares));
        builder.append(String.format("Percentage Gain/Loss: %8.2f%%\n", percentageGainLoss));
        builder.append(String.format("Number Buys         : %8d\n", numBuys));
        builder.append(String.format("Number Sells        : %8d\n", numSells));
        builder.append(String.format("Number Trades       : %8d\n", numTrades));
        builder.append(String.format("Commission Paid     : %8d\n", (int)numTrades*DEFAULT_COMMISSION_FEE));
        builder.append(String.format("Symbol              : %8s\n", symbol));

        builder.append("Account Summary:\n");
        List<Action> actions = cloneList(activity);
        Collections.sort(actions, (c1, c2) -> (int) (c1.getDate().isBefore(c2.getDate()) ? -1 : 1));
        for(Action action : actions) {
            builder.append(String.format("  Symbol: %s\n", action.getSymbol()));
            builder.append(String.format("  Action: %s\n", action.getAction()));
            builder.append(String.format("  Amount: %8.2f\n", action.getAmount()));
            builder.append(String.format("  Shares: %8.2f\n", action.getShares()));
            builder.append(String.format("  Price : %8.2f\n", action.getPrice()));
            builder.append(String.format("  Date  : %s\n", action.getDate().toString().split("T")[0]));
            builder.append("----------------------------\n");
        }
        return builder.toString();
    }

    public DateTime getLastTrade() {
        List<Action> actions = cloneList(activity);
        Collections.sort(actions, (c1, c2) -> (int) (c1.getDate().isBefore(c2.getDate()) ? -1 : 1));
        if(actions.isEmpty()) {
            return new DateTime(0);
        }
        return actions.get(actions.size()-1).getDate();
    }

    public static List<Action> cloneList(List<Action> list) {
        List<Action> clone = new ArrayList<Action>(list.size());
        for(Action item: list) clone.add(item.clone());
        return clone;
    }
}
