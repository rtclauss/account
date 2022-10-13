package com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.controllers;

import com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.jnosql.db.AccountService;
import com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.json.Account;
import io.micrometer.core.annotation.Timed;
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

    @Timed(value = "account.all", description = "How long does it take to get all Accounts")
    @QueryMapping
    public List<Account> allAccounts() {
        return accountService.getAllAccounts();
    }

    @Timed(value = "account.by-page", description = "How long does it take to get all Accounts paginated")
    @QueryMapping(name = "allAccountsByPage")
    public List<Account> allAccounts(@Argument int pageSize, @Argument int pageNumber) {
        return accountService.getAllAccounts(pageSize, pageNumber - 1);
    }

    @Timed(value = "account.by-id", description = "How long does it take to get an Account by id")
    @QueryMapping(name = "retrieveAccountById")
    public Account getAccountById(@Argument String ownerId) {
        return accountService.getAccountById(ownerId);
    }

    @Timed(value = "account.by-owner-name", description = "How long does it take to get an Accounts from owner")
    @QueryMapping(name = "retrieveAccountsByOwnerName")
    public List<Account> getAccountsByOwnerName(@Argument String ownerName) {
        return accountService.getAccountsByOwnerName(ownerName);
    }

    @Timed(value = "account.create", description = "How long does it take to create an Account")
    @MutationMapping
    public Account createAccount(@Argument String ownerName) {
        return accountService.createAccount(
                Account.builder()
                        .owner(ownerName)
                        .build()
        );
    }

    @Timed(value = "account.delete", description = "How long does it take to delete an Account")
    @MutationMapping
    public Account deleteAccount(@Argument String id) {
        return accountService.deleteAccountById(id);
    }

    @Timed(value = "account.update", description = "How long does it take to update an Account")
    @MutationMapping
    public Account updateAccount(@Argument String id, @Argument double portfolioTotal) {
        return accountService.updatePortfolio(id, portfolioTotal);
    }
}
