package com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.jnosql;

import com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.client.WatsonClient;
import com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.jnosql.db.AccountService;
import com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.json.Feedback;
import com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.json.WatsonInput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FeedbackService {

    private final AccountService accountService;
    private final WatsonClient watsonClient;

    @Autowired
    public FeedbackService(AccountService accountService, WatsonClient watsonClient) {
        this.accountService = accountService;
        this.watsonClient = watsonClient;
    }

    public Feedback submitFeedback(String id, WatsonInput watsonInput) {
        var account = accountService.getAccountById(id);
        var watsonOutput = watsonClient.getTone(watsonInput);
        var sentiment = watsonOutput.determineSentiment();
        var feedback = Feedback.getFeedbackFromSentiment(sentiment);
        account.setSentimentAndFree(feedback);
        accountService.saveAccount(account);
        return feedback;
    }
}
