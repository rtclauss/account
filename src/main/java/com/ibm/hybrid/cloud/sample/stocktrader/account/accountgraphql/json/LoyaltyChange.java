package com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.json;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoyaltyChange {

    private String fOwner;
    private String fOld;
    private String fNew;
    private String fId;

}
