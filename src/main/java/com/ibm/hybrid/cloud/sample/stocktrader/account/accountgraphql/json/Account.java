package com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.groocraft.couchdb.slacker.annotation.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@Document("account") //TODO can this be exposed via secret or hard code?
@AllArgsConstructor
@NoArgsConstructor
public class Account {

    @JsonProperty("_id")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String id;

    @JsonProperty("_rev")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String revision;

    @JsonProperty
    private String owner;

    @JsonProperty
    private String loyalty;

    @JsonProperty
    private double balance;

    @JsonProperty
    private double commissions;

    @JsonProperty
    private int free;

    @JsonProperty
    private String sentiment;

    @JsonProperty
    private double nextCommission;

    @JsonProperty
    private String operation;

    public void setSentimentAndFree(Feedback feedback) {
        setFree(free + feedback.getFree());
        setSentiment(feedback.getSentiment());
    }

    public double calculateCommission() {
        if (loyalty.equalsIgnoreCase(LoyaltyType.BRONZE)) {
            return 8.99;
        } else if (loyalty.equalsIgnoreCase(LoyaltyType.SILVER)) {
            return 7.99;
        } else if (loyalty.equalsIgnoreCase(LoyaltyType.GOLD)) {
            return 6.99;
        } else if (loyalty.equalsIgnoreCase(LoyaltyType.PLATINUM)) {
            return 5.99;
        }
        return 9.99;
    }

    public void updateBalanceAndCommissions(double commission) {
        if (free > 0) {
            setFree(--free);
        } else {
            setCommissions(commissions + commission);
            setBalance(balance - commission);
        }
    }
}
