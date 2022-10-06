package com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.json;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.core.mapping.Field;
import org.springframework.data.couchbase.core.mapping.id.GeneratedValue;
import org.springframework.data.couchbase.core.mapping.id.GenerationStrategy;

@Data
@Builder
@Document
@AllArgsConstructor
@NoArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationStrategy.UNIQUE)
    private String id;

    @Field
    private String owner;

    @Field
    private String loyalty;

    @Field
    private double balance;

    @Field
    private double commissions;

    @Field
    private int free;

    @Field
    private String sentiment;

    @Field
    private double nextCommission;

    @Field
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
