package com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.json;

public class ODMLoyaltyRule {

    private LoyaltyDecision theLoyaltyDecision = null;

    public ODMLoyaltyRule() { //default constructor
    }

    public ODMLoyaltyRule(double tradeTotal) { //convenience constructor
        LoyaltyDecision newLoyaltyDecision = new LoyaltyDecision(tradeTotal);
        setTheLoyaltyDecision(newLoyaltyDecision);
    }

    public LoyaltyDecision getTheLoyaltyDecision() {
        return theLoyaltyDecision;
    }

    public void setTheLoyaltyDecision(LoyaltyDecision newLoyaltyDecision) {
        theLoyaltyDecision = newLoyaltyDecision;
    }

    public String determineLoyalty() {
        String loyalty = "Unknown";
        if (theLoyaltyDecision != null) loyalty = theLoyaltyDecision.getLoyalty();
        return loyalty;
    }
}
