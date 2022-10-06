package com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.jnosql.db;


import com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.client.ODMClient;
import com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.json.Account;
import com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.json.LoyaltyChange;
import com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.json.ODMLoyaltyRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AccountService {

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
        return accountRepository.findAll();
    }

    public List<Account> getAllAccounts(int pageSize, int pageNumber) {
        var pageRequest = PageRequest.of(pageNumber, pageSize);
        return accountRepository.findAll(pageRequest).getContent();
    }

    public Account getAccountById(String id) {
        return accountRepository.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found")
                );
    }

    public List<Account> getAccountsByOwnerName(String ownerName) {
        return accountRepository.findByOwner(ownerName);
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
        accountRepository.deleteById(id);
        return account;
    }

    public Account updatePortfolio(String id, double portfolioTotal) {
        var account = getAccountById(id);
        var loyalty = calculateLoyalty(portfolioTotal);
        sendMessage(account, loyalty);

        account.setLoyalty(loyalty);
        var newCommission = account.calculateCommission();
        account.updateBalanceAndCommissions(newCommission);
        accountRepository.save(account);
        return account;
    }

    private void sendMessage(Account account, String newLoyalty) {
        jmsTemplate.convertAndSend(
                LoyaltyChange.builder()
                        .fId("test_id")
                        .fOwner(account.getOwner())
                        .fOld(account.getLoyalty())
                        .fNew(newLoyalty)
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
