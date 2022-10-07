package com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.controllers;

import com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.jnosql.db.AccountService;
import com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.json.Account;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Slf4j
@Controller
public class AccountController {

    private final AccountService accountService;

    @Autowired
    public AccountController(AccountService accountRepository) {
        this.accountService = accountRepository;
    }

    //todo ask about traced annotations counted and timed
    @QueryMapping
    public List<Account> allAccounts() {
        log.info("getting all accounts");
        return accountService.getAllAccounts();
    }

    @QueryMapping(name = "allAccountsByPage")
    public List<Account> allAccounts(@Argument int pageSize, @Argument int pageNumber) {
        return accountService.getAllAccounts(pageSize, pageNumber - 1);
    }

    @QueryMapping(name = "retrieveAccountById")
    public Account getAccountById(@Argument String id) {
        return accountService.getAccountById(id);
    }

    @QueryMapping(name = "retrieveAccountsByOwnerName")
    public List<Account> getAccountsByOwnerName(@Argument String ownerName) {
        return accountService.getAccountsByOwnerName(ownerName);
    }

    @MutationMapping
    public Account createAccount(@Argument String ownerName) {
        return accountService.createAccount(
                Account.builder()
                        .owner(ownerName)
                        .build()
        );
    }

    @MutationMapping
    public Account deleteAccount(@Argument String id) {
        return accountService.deleteAccountById(id);
    }

    @MutationMapping
    public Account updateAccount(@Argument String id, @Argument double portfolioTotal) {
        return accountService.updatePortfolio(id, portfolioTotal);
    }
}
