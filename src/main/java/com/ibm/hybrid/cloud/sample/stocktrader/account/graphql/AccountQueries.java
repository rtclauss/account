package com.ibm.hybrid.cloud.sample.stocktrader.account.graphql;


import com.ibm.hybrid.cloud.sample.stocktrader.account.json.graphql.Account;
import org.eclipse.microprofile.graphql.*;

import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.eclipse.microprofile.opentracing.Traced;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import java.util.List;
import java.util.logging.Logger;

@Traced
@GraphQLApi
public class AccountQueries {

    private static Logger LOGGER = Logger.getLogger(AccountQueries.class.getName());
    @Inject
    AccountService accountService;

    // TODO to separate our reads from our writes we should have a separate reader service that reads from to read replicas
    // this would also require an additional Operator configuration endpoint that points at the read replica(s)
    //@Inject AccountQueryImpl accountMutationsImplService;


    // This is the GraphQL context from the caller. We can get at the requested parameters from here.
    // This could be added to the GraphQL standard in Microprofile at a later date.
    //@Inject Context context;

    @Query("allAccounts")
    @Description("Retrieve all accounts. This can be an expensive operation. Please use pagination API first.")
    @Timed(name = "getAccountsTimer", description = "How long does it take to get all Accounts", unit = MetricUnits.NANOSECONDS)
    @Counted(name = "getAccountsCount", displayName = "Get All Stock Trader accounts", description = "Number of times all accounts retrieved in the Stock Trader application")
    @RolesAllowed({"StockTrader", "StockViewer"})
    //Couldn't get this to work; had to do it through the web.xml instead :(
    public List<Account> getAllAccounts() {
        LOGGER.fine("GraphQL AccountQueries.getAllAccounts()");
        var allAccounts = accountService.getAllAccounts();
        return allAccounts;
    }

    @Query("allAccountsByPage")
    @Description("Get all accounts via pagination.")
    @Timed(name = "getAccountsTimer", description = "How long does it take to get all Accounts", unit = MetricUnits.NANOSECONDS)
    @Counted(name = "getAccountsCount", displayName = "Get All Stock Trader accounts", description = "Number of times all accounts retrieved in the Stock Trader application")
    @RolesAllowed({"StockTrader", "StockViewer"})
    public List<Account> getAllAccounts(@Description("Page number to retrieve") Integer pageNumber, @Description("Number of results per page") Integer pageSize) {
        LOGGER.fine("GraphQL AccountQueries.getAllAccounts(" + pageNumber + "," + pageSize + ")");

        var allAccounts = accountService.getAllAccounts(pageNumber, pageSize);
        return allAccounts;
    }

    @Query("retrieveAccountById")
    @Timed(name = "getAccountTimer", description = "How long does it take to get an Account", unit = MetricUnits.NANOSECONDS)
    @Counted(name = "getAccountCount", displayName = "Get Stock Trader account", description = "Number of accounts retrieved in the Stock Trader application")
    @RolesAllowed({"StockTrader", "StockViewer"})
    public Account getAccount(@Name("ownerId") String ownerId) {
        LOGGER.fine("GraphQL AccountQueries.getAccount(" + ownerId + ")");
        return accountService.getAccount(ownerId);
    }

    @Query("retrieveAccountsByOwnerName")
    @Timed(name = "getAccountTimer", description = "How long does it take to get an Account", unit = MetricUnits.NANOSECONDS)
    @Counted(name = "getAccountCount", displayName = "Get Stock Trader account", description = "Number of accounts retrieved in the Stock Trader application")
    @RolesAllowed({"StockTrader", "StockViewer"})
    public List<Account> getAccountsByOwnerName(@Name("owner") String ownerName) {
        LOGGER.fine("GraphQL AccountQueries.getAccountsByOwnerName(" + ownerName + ")");
        return accountService.getAccountsByName(ownerName);
    }

    // Let's treat the following as @Source items that are calculated only if needed/queried for!
    // We can add this as a separately exposed query by adding @Query annotation to the method.
    //
    // In practice, we can't do this today (16 Sept 2022) because calculating loyalty also depends on the portfolio total.
    // public String loyalty(@Source Account account){
    //     return accountService.calculateLoyalty(account);
    // }

}
