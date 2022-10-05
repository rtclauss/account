package com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.json;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.couchbase.core.mapping.Document;

@Data
@Document
@AllArgsConstructor
@NoArgsConstructor
public class Account {

    @Id
    private String _id;
    private String _rev;
    private String owner;
    private String loyalty;
    private double balance;
    private double commissions;
    private int free;
    private String sentiment;
    private double nextCommission;
    private String operation;
}
