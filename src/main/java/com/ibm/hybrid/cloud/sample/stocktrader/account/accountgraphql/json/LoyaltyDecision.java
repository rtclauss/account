package com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.json;


import lombok.Data;

@Data
public class LoyaltyDecision {
    private double tradeTotal;
    private String loyalty = "UNKNOWN";

    public LoyaltyDecision(double tradeTotal) {
        this.tradeTotal = tradeTotal;
    }
}
