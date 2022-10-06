package com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.client;


import com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.json.WatsonDocument;
import com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.json.WatsonInput;
import com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.json.WatsonOutput;
import com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.json.WatsonTone;
import org.springframework.stereotype.Component;

@Component
public class WatsonClient {

    public WatsonOutput getTone(WatsonInput watsonInput) {
        return new WatsonOutput(
                new WatsonDocument(
                        new WatsonTone[]{}
                )
        );
    }

}
