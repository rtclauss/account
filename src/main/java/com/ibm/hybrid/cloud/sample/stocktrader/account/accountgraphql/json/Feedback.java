package com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.json;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Feedback {

    private String message;
    private int free;
    private String sentiment;

    public static Feedback getFeedbackFromSentiment(String sentiment){
        int freeTrades = 1;
        String message = "Thanks for providing feedback.  Have a free trade on us!";
        if ("Anger".equalsIgnoreCase(sentiment)) {
            freeTrades = 3;
            message = "We're sorry you are upset.  Have three free trades on us!";
        } else if ("Unknown".equalsIgnoreCase(sentiment)) {
            freeTrades = 0;
            message = "Error communicating with the Watson Tone Analyzer";
        }
        return Feedback.builder()
                .message(message)
                .free(freeTrades)
                .sentiment(sentiment)
                .build();
    }
}
