package com.ibm.hybrid.cloud.sample.stocktrader.account.graphql;

import com.ibm.hybrid.cloud.sample.stocktrader.account.json.Feedback;
import com.ibm.hybrid.cloud.sample.stocktrader.account.json.WatsonInput;
import com.ibm.hybrid.cloud.sample.stocktrader.account.json.graphql.Account;
import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Timed;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import java.io.IOException;
import java.util.logging.Logger;

@GraphQLApi
public class AccountMutations {
    private static Logger logger = Logger.getLogger(AccountMutations.class.getName());

    @Inject AccountService accountService;

    @Mutation
    @Description("Create a new account for a person")
    @Counted(name = "createAccountCount", displayName = "Create Stock Trader account", description = "Number of accounts created in the Stock Trader application")
    @Timed(name = "createAccountTimer", description = "How long does it take to create an Account", unit = MetricUnits.NANOSECONDS)
    @RolesAllowed({"StockTrader"})
    public Account createAccount(@Name("ownerName") String name){
        var account = new Account(name);
        accountService.createAccount(account);
        return account;

    }

    @Mutation
    @Timed(name = "submitFeedbackTimer", description = "How long does it take to submit feedback from an Account", unit = MetricUnits.NANOSECONDS)
    @Counted(name = "submitFeedbackCount", displayName = "Stock Trader accounts", description = "Number of accounts created in the Stock Trader application")
    @RolesAllowed({"StockTrader"})
    public Feedback submitFeedback(@Name("id") String id, @Name("watsonFeedback") WatsonInput input) throws IOException {
        return accountService.submitFeedback(id, input);
    }

    @Mutation
    @Timed(name = "deleteAccountTimer", description = "How long does it take to delete an Account", unit = MetricUnits.NANOSECONDS)
    @Counted(name = "deleteAccountCount", displayName = "Delete Stock Trader account", description = "Number of accounts created in the Stock Trader application")
    @RolesAllowed({"StockTrader"})
    public String deleteAccount(@Name("id")String id) {
        return accountService.deleteAccount(id);
    }

    @Mutation
    @Timed(name = "updateAccountTimer", description = "How long does it take to update an Account", unit = MetricUnits.NANOSECONDS)
    @Counted(name = "updateAccountCount", displayName = "Update Stock Trader account", description = "Number of accounts updated in the Stock Trader application")
    @RolesAllowed({"StockTrader"})
    public Account updateAccount(@Name("id")String id, @Name("tradeAmount") double tradeAmount) throws IOException {
        return accountService.updateAccount(id, tradeAmount);
    }
}
