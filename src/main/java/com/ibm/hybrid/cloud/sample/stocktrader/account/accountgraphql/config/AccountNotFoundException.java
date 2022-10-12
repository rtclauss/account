package com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.config;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException() {
        super("Account not found");
    }
}
