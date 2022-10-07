package com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.controllers;


import com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.jnosql.FeedbackService;
import com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.json.Feedback;
import com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.json.WatsonInput;
import io.micrometer.core.annotation.Timed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

@Controller
public class FeedbackController {

    private final FeedbackService feedbackService;

    @Autowired
    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;

    }

    @Timed(value = "feedback.create", description = "How long does it take to submit feedback from an Account")
    @MutationMapping
    public Feedback submitFeedback(@Argument String id, @Argument WatsonInput watsonInput) {
        return feedbackService.submitFeedback(id, watsonInput);
    }
}
