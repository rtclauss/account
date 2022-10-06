package com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.json;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WatsonOutput {
    private WatsonDocument documentTone;

    public String determineSentiment() {
        String sentiment = "Unknown";

        if (documentTone != null) {
            WatsonTone[] tones = documentTone.getTones();
            double score = 0.0;
            for (WatsonTone tone : tones) {
                double newScore = tone.getScore();
                if (newScore > score) { //we might get back multiple tones; if so, go with the one with the highest score
                    sentiment = tone.getToneName();
                    score = newScore;
                }
            }
        }

        return sentiment;
    }


}
