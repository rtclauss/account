package com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql;

import com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.jnosql.db.AccountService;
import com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.json.Account;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class AccountGraphqlApplication implements CommandLineRunner {

    private final AccountService accountService;
    private final Environment env;

    @Autowired
    public AccountGraphqlApplication(AccountService accountService, Environment env) {
        this.accountService = accountService;
        this.env = env;
    }

    public static void main(String[] args) {
        SpringApplication.run(AccountGraphqlApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        var createDummyAccounts = env.getProperty("app.createDummyAccounts");
        if (createDummyAccounts != null && createDummyAccounts.equals("true")) {
            accountService.createAccount(
                    Account.builder()
                            .owner("123")
                            .loyalty("Basic")
                            .balance(12)
                            .commissions(12)
                            .free(2)
                            .sentiment("test")
                            .nextCommission(0)
                            .operation("operation")
                            .build()
            );
        }
    }
}
