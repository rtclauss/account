package com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.jnosql.db;


import com.groocraft.couchdb.slacker.exception.CouchDbException;
import com.groocraft.couchdb.slacker.exception.CouchDbRuntimeException;
import com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.client.ODMClient;
import com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.config.AccountNotFoundException;
import com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.json.Account;
import com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.json.LoyaltyChange;
import com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.json.ODMLoyaltyRule;
import io.opentracing.Scope;
import io.opentracing.Tracer;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class AccountService {

    @Autowired
    private Tracer tracer;

    private final AccountRepository accountRepository;
    private final ODMClient odmClient;

    private final JmsTemplate jmsTemplate;

    @Autowired
    public AccountService(AccountRepository accountRepository, ODMClient odmClient, JmsTemplate jmsTemplate) {
        this.accountRepository = accountRepository;
        this.odmClient = odmClient;
        this.jmsTemplate = jmsTemplate;
    }

    public List<Account> getAllAccounts() {
        return StreamSupport.stream(accountRepository.findAll().spliterator(), false)
                .collect(Collectors.toList());
    }

    public List<Account> getAllAccounts(int pageSize, int pageNumber) {
        var pageRequest = PageRequest.of(Math.abs(pageNumber), Math.abs(pageSize));
        return accountRepository.findAll(pageRequest).getContent();
    }

    public Account getAccountById(String id) {
        return accountRepository.findById(id)
                .orElseThrow(AccountNotFoundException::new);
    }

    public List<Account> getAccountsByOwnerName(String ownerName) {
        return accountRepository.findByOwner(ownerName);
    }

    public List<Account> getAccountsByOwner(List<String> owners) {
        var accountList = new ArrayList<Account>();

        // This mirrors what we had to do in OpenLiberty/JNoSQL
        var outerLoopSpan = tracer.buildSpan("AccountService.getAccountsByOwner(List<String> owners) outer loop").start();
        try (Scope outerScope = tracer.scopeManager().activate(outerLoopSpan)) {
            for (String ownerName : owners) {
                var innerLoopSpan = tracer.buildSpan("AccountService.getAccountsByOwner(List<String> owners) inner loop").start();
                try (Scope innerScope = tracer.scopeManager().activate(innerLoopSpan)) {
                    var account = accountRepository.findByOwner(ownerName);
                    if (account.size() > 0) {
                        accountList.add(account.get(0));
                    }
                } finally {
                    innerLoopSpan.finish();
                }
            }
        }finally {
            outerLoopSpan.finish();
        }

        //return accountRepository.findByOwnerIn(owners);
        return accountList;

    }

    public Account createAccount(Account account) {
        var existedAccount = getAccountsByOwnerName(account.getOwner())
                .stream()
                .filter(x -> x.getOwner().equals(account.getOwner()))
                .findFirst();
        return existedAccount.orElseGet(() -> accountRepository.save(account));
    }

    public Account deleteAccountById(String id) {
        var account = getAccountById(id);
        try {
            accountRepository.deleteById(id);
        } catch (CouchDbRuntimeException cdbre) {
            var newAcct = getAccountById(id);
            System.out.println(Thread.currentThread().getName() + " Tried to delete owner account " + account.getOwner() + " with couchdb id " + account.getId() + ". rev we were trying was " + account.getRevision() + " but now the db has rev " + newAcct.getRevision());
            System.out.println(Thread.currentThread().getName() + " Our copy of account: " + account.toString() + "... DB Copy of account: " + newAcct.toString());
            cdbre.printStackTrace();
        } finally {
            System.out.println(Thread.currentThread().getName() + "Trying to delete id " + id + " again");
            try {
                accountRepository.deleteById(id);
            } catch (CouchDbRuntimeException cdbre) {
                var alreadyDeletedBool = cdbre.getMessage().contains("404");
                System.out.println(Thread.currentThread().getName() + "Id " + id + " was already deleted/not found: " + alreadyDeletedBool);
            }
        }
        return account;
    }

    public Account updatePortfolio(String id, double portfolioTotal) {
        var account = getAccountById(id);
        var loyalty = calculateLoyalty(portfolioTotal);
        sendMessage(account, loyalty);

        Account retryUpdateAccount = null;


//        System.out.println(Thread.currentThread().getName()+ " About to set loyalty for " + account.toString());

        account.setLoyalty(loyalty);

//        System.out.println(Thread.currentThread().getName()+ " Done setting loyalty for " + account.toString());
//        var dbAcct = getAccountById(id);
//        System.out.println(Thread.currentThread().getName()+ " DB has account rev " + dbAcct.toString());
        var newCommission = account.calculateCommission();
//        System.out.println(Thread.currentThread().getName()+ " About to set commission for " + account.toString());
        account.updateBalanceAndCommissions(newCommission);
//        System.out.println(Thread.currentThread().getName()+ " Done setting commission for " + account.toString());
//        System.out.println(Thread.currentThread().getName()+ " DB has account rev " + dbAcct.toString());
        try {
//            System.out.println(Thread.currentThread().getName()+ " about to save " + account.toString());
            account = accountRepository.save(account);
//            System.out.println(Thread.currentThread().getName()+ " DB has account rev " + account.toString());
        } catch (CouchDbRuntimeException cdbre) {
            retryUpdateAccount = getAccountById(id);
            System.out.println(Thread.currentThread().getName() + " Tried to update owner account " + account.getOwner() + " with couchdb id " + account.getId() + ". rev we were trying was " + account.getRevision() + " but new db has rev " + retryUpdateAccount.getRevision());
            System.out.println(Thread.currentThread().getName() + " Our copy of account: " + account.toString() + "... DB Copy of account: " + retryUpdateAccount.toString());
            cdbre.printStackTrace();
        } finally {
            if (retryUpdateAccount != null) {
                System.out.println(Thread.currentThread().getName() + " Final try to update account: " + account.toString() + " using latest DB Copy of account: " + retryUpdateAccount.toString());
                retryUpdateAccount.setLoyalty(loyalty);
                retryUpdateAccount.updateBalanceAndCommissions(newCommission);
                account = accountRepository.save(retryUpdateAccount);
            }
        }
        return account;
    }

    private void sendMessage(Account account, String newLoyalty) {
        //to be added with spring security and validate jwt
        jmsTemplate.convertAndSend(
                LoyaltyChange.builder()
                        .fId("test_id")
                        .fOwner(account.getOwner())
                        .fOld(account.getLoyalty())
                        .fNew(newLoyalty)
                        .build()
                        .toJson()
        );
    }

    private String calculateLoyalty(double portfolioTotal) {
        var inputLoyalty = new ODMLoyaltyRule(portfolioTotal);
        var loyaltyResult = odmClient.getLoyaltyLevel(inputLoyalty);
        return loyaltyResult.determineLoyalty();
    }

    public void saveAccount(Account account) {
        accountRepository.save(account);
    }


}
