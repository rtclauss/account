package com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.json;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WatsonTone {
    private double score;
    private String toneId;
    private String toneName;
}
