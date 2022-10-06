package com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.client;

import com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.json.ODMLoyaltyRule;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class ODMClient {

    public ODMLoyaltyRule getLoyaltyLevel(ODMLoyaltyRule input) {
        return new ODMLoyaltyRule(ThreadLocalRandom.current().nextDouble());
    }
}
