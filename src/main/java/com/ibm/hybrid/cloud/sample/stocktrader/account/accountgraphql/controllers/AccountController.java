package com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.controllers;

import com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.jnosql.db.AccountService;
import com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.json.Account;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Slf4j
@Controller
public class AccountController {

    private final AccountService accountRepository;

    @Autowired
    public AccountController(AccountService accountRepository) {
        this.accountRepository = accountRepository;
    }

    @QueryMapping
    public List<Account> allAccounts() {
        log.info("getting all accounts");
        return accountRepository.getAllAccounts();
    }
}
