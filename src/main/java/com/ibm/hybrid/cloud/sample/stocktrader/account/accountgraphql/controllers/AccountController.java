package com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.controllers;

import com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.jnosql.db.AccountService;
import com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.json.Account;
import io.micrometer.core.annotation.Timed;
import io.opentracing.Scope;
import io.opentracing.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Controller
public class AccountController {

    @Autowired
    private Tracer tracer;

    private final AccountService accountService;

    @Autowired
    public AccountController(AccountService accountRepository) {
        this.accountService = accountRepository;
    }

    @Timed(value = "account.all", description = "How long does it take to get all Accounts")
    @QueryMapping
    public List<Account> allAccounts() {
        List<Account> allAccounts = new ArrayList<>();
        var getAllAccountsSpan = tracer.buildSpan("AccountController.allAccounts()").start();
        try (Scope childScope = tracer.scopeManager().activate(getAllAccountsSpan)) {
            allAccounts = accountService.getAllAccounts();
        } finally {
            getAllAccountsSpan.finish();
        }
        return allAccounts;
    }

    @Timed(value = "account.by-page", description = "How long does it take to get all Accounts paginated")
    @QueryMapping(name = "allAccountsByPage")
    public List<Account> allAccounts(@Argument int pageSize, @Argument int pageNumber) {
        return accountService.getAllAccounts(pageSize, pageNumber - 1);
    }

    @Timed(value = "account.by-id", description = "How long does it take to get an Account by id")
    @QueryMapping(name = "retrieveAccountById")
    public Account getAccountById(@Argument String ownerId) {
        Account account = new Account();
        var getAccountSpan = tracer.buildSpan("AccountController.getAccountById()").start();
        try (Scope childScope = tracer.scopeManager().activate(getAccountSpan)) {
            account = accountService.getAccountById(ownerId);
        } finally {
            getAccountSpan.finish();
        }
        return account;
    }

    @Timed(value = "account.by-owner-name", description = "How long does it take to get an Accounts from owner")
    @QueryMapping(name = "retrieveAccountByOwnerName")
    public List<Account> getAccountsByOwnerName(@Argument String ownerName) {
        List<Account> allAccounts = new ArrayList<>();
        var getAllAccountsSpan = tracer.buildSpan("AccountController.allAccounts(String ownerName)").start();
        try (Scope childScope = tracer.scopeManager().activate(getAllAccountsSpan)) {
            allAccounts = accountService.getAccountsByOwnerName(ownerName);
        } finally {
            getAllAccountsSpan.finish();
        }
        return allAccounts;
    }

    @QueryMapping(name = "retrieveAccountsByOwner")
    public List<Account> getAccountsByOwner(@Argument List<String> owners) {
        List<Account> allAccounts = new ArrayList<>();
        var getAllAccountsSpan = tracer.buildSpan("AccountController.getAccountsByOwner(List<String> owners)").start();
        try (Scope childScope = tracer.scopeManager().activate(getAllAccountsSpan)) {
            allAccounts = accountService.getAccountsByOwner(owners);
        } finally {
            getAllAccountsSpan.finish();
        }
        return allAccounts;
    }

    @Timed(value = "account.create", description = "How long does it take to create an Account")
    @MutationMapping
    public Account createAccount(@Argument String ownerName) {
        Account account = new Account();
        var getAccountSpan = tracer.buildSpan("AccountController.createAccount()").start();
        try (Scope childScope = tracer.scopeManager().activate(getAccountSpan)) {
            account = accountService.createAccount(
                    Account.builder()
                            .owner(ownerName)
                            .build()
            );
        } finally {
            getAccountSpan.finish();
        }
        return account;
    }

    @Timed(value = "account.delete", description = "How long does it take to delete an Account")
    @MutationMapping
    public Account deleteAccount(@Argument String id) {
        Account account = new Account();
        var getAccountSpan = tracer.buildSpan("AccountController.deleteAccount()").start();
        try (Scope childScope = tracer.scopeManager().activate(getAccountSpan)) {
            account = accountService.deleteAccountById(id);
        } finally {
            getAccountSpan.finish();
        }
        return account;
    }

    @Timed(value = "account.update", description = "How long does it take to update an Account")
    @MutationMapping
    public Account updateAccount(@Argument String id, @Argument double portfolioTotal) {
        Account account = new Account();
        var getAccountSpan = tracer.buildSpan("AccountController.updateAccount()").start();
        try (Scope childScope = tracer.scopeManager().activate(getAccountSpan)) {
            account = accountService.updatePortfolio(id, portfolioTotal);
        } finally {
            getAccountSpan.finish();
        }

        return account;
    }
}
