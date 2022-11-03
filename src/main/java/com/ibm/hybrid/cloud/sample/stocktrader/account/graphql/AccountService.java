package com.ibm.hybrid.cloud.sample.stocktrader.account.graphql;

import com.ibm.hybrid.cloud.sample.stocktrader.account.AccountUtilities;
import com.ibm.hybrid.cloud.sample.stocktrader.account.client.ODMClient;
import com.ibm.hybrid.cloud.sample.stocktrader.account.client.WatsonClient;
import com.ibm.hybrid.cloud.sample.stocktrader.account.jnosql.db.AccountRepository;
import com.ibm.hybrid.cloud.sample.stocktrader.account.json.Feedback;
import com.ibm.hybrid.cloud.sample.stocktrader.account.json.WatsonInput;
import com.ibm.hybrid.cloud.sample.stocktrader.account.json.graphql.Account;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import jakarta.nosql.mapping.Database;
import jakarta.nosql.mapping.DatabaseType;
import jakarta.nosql.mapping.Pagination;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.opentracing.Traced;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
@Traced
public class AccountService {
    private static final Logger logger = Logger.getLogger(AccountService.class.getName());
    private static final String FAIL = "FAIL";

    private static final int    CONFLICT         = 409;         //odd that JAX-RS has no ConflictException

    private static boolean initialized = false;

    @Inject
    private AccountUtilities utilities;


    @Inject
    @Database(DatabaseType.DOCUMENT)
    private AccountRepository repository;

    private @Inject
    @RestClient ODMClient odmClient;
    private @Inject
    @RestClient WatsonClient watsonClient;

    private @Inject JsonWebToken token;

    private @Inject
    @ConfigProperty(name = "ODM_ID", defaultValue = "odmAdmin") String odmId;
    private @Inject
    @ConfigProperty(name = "ODM_PWD", defaultValue = "odmAdmin") String odmPwd;
    private @Inject
    @ConfigProperty(name = "WATSON_ID", defaultValue = "apikey") String watsonId;
    private @Inject
    @ConfigProperty(name = "WATSON_PWD") String watsonPwd; //if using an API Key, it goes here

    // OpenTracing tracer
    private @Inject Tracer tracer;

    // Override ODM Client URL if secret is configured to provide URL
    static {
        String mpUrlPropName = ODMClient.class.getName() + "/mp-rest/url";
        String urlFromEnv = System.getenv("ODM_URL");
        if ((urlFromEnv != null) && !urlFromEnv.isEmpty()) {
            logger.info("Using ODM URL from config map: " + urlFromEnv);
            System.setProperty(mpUrlPropName, urlFromEnv);
        } else {
            logger.info("ODM URL not found from env var from config map, so defaulting to value in jvm.options: " + System.getProperty(mpUrlPropName));
        }

        mpUrlPropName = WatsonClient.class.getName() + "/mp-rest/url";
        urlFromEnv = System.getenv("WATSON_URL");
        if ((urlFromEnv != null) && !urlFromEnv.isEmpty()) {
            logger.info("Using Watson URL from config map: " + urlFromEnv);
            System.setProperty(mpUrlPropName, urlFromEnv);
        } else {
            logger.info("Watson URL not found from env var from config map, so defaulting to value in jvm.options: " + System.getProperty(mpUrlPropName));
        }
    }

    /**
     * @param account
     * @return created account with new Id
     */
    public Account createAccount(Account account) {
        if (account.getOwner() != null) try {
            if (FAIL.equalsIgnoreCase(account.getOwner())) {
                logger.warning("Throwing a 400 error for owner: " + account.getOwner());
                throw new BadRequestException("Invalid value for account owner: " + account.getOwner());
            }

            String owner = account.getOwner();
            // rtclauss let's break this down to find where the slowdown is (9/20/22
//            Span checkIfOwnerExistsSpan = tracer.buildSpan("repository.findByOwner(owner).findFirst().isPresent()").start();
//            try(Scope childScope = tracer.scopeManager().activate(checkIfOwnerExistsSpan)) {
//                boolean ownerExistInRepo = repository.findByOwner(owner).findFirst().isPresent();
//                if (ownerExistInRepo) {
//                    logger.warning("Account already exists for: " + owner);
//                    throw new WebApplicationException("Account already exists for " + owner + "!", CONFLICT);
//                }
//            } finally {
//                checkIfOwnerExistsSpan.finish();
//            }

//            Span checkIfOwnerExistsSpan = tracer.buildSpan("repository.findByOwner(owner).findFirst().isPresent()").start();
//            try(Scope childScope = tracer.scopeManager().activate(checkIfOwnerExistsSpan)) {
//                boolean ownerExistInRepo = repository.findByOwner(owner).findFirst().isPresent();
//                if (ownerExistInRepo) {
//                    logger.warning("Account already exists for: " + owner);
//                    throw new WebApplicationException("Account already exists for " + owner + "!", CONFLICT);
//                }
//            } finally {
//                checkIfOwnerExistsSpan.finish();
//            }

            Optional<Account> accountOwner;
            Span findByOwnerQuerySpan = tracer.buildSpan("repository.findByOwner(owner)").start();
            try(Scope childScope = tracer.scopeManager().activate(findByOwnerQuerySpan)) {
                accountOwner = repository.findByOwner(owner);
            } finally {
                findByOwnerQuerySpan.finish();
            }

            boolean ownerExistInRepo;
            Span checkIfOwnerExists = tracer.buildSpan("accountOwnerStream.findFirst().isPresent()").start();
            try(Scope childScope = tracer.scopeManager().activate(checkIfOwnerExists)) {
                ownerExistInRepo = accountOwner.isPresent();
            } finally {
                checkIfOwnerExists.finish();
            }

            if (ownerExistInRepo) {
                logger.warning("Account already exists for: " + owner);
                throw new WebApplicationException("Account already exists for " + owner + "!", CONFLICT);
            }



            logger.fine("Creating account for " + account.getOwner());
            Account response = saveAccountToDb(account);
            System.out.println(response);
            if (response != null) {
                String id = response.getId();
                logger.fine("Created new account for " + account.getOwner() + " with id " + id);
            } else {
                logger.warning("Failed to get response from accountDB.save()"); //shouldn't get here - exception should have been thrown if the save failed
            }

            logger.fine("Account created successfully: " + account.getOwner());
        } catch (Throwable t) {
            logger.warning("Failure to create account for " + account.getOwner());
            logException(t);
        }
        else {
            logger.warning("Owner is null in createAccount");
        }
        return account;
    }



    public List<Account> getAllAccounts() {
        List<Account> accountList = null;
        int size = 0;

        try {
            logger.fine("Entering getAccounts");
            if (repository != null) {
                Span getAllAccountsSpan = tracer.buildSpan("repository.findAll().collect(Collectors.toList())").start();
                try(Scope childScope = tracer.scopeManager().activate(getAllAccountsSpan)) {
                accountList = repository.findAll().collect(Collectors.toList());
                } finally {
                    getAllAccountsSpan.finish();
                }
                size = accountList.size();
            } else {
                logger.warning("accountDB is null, so returning empty array.  Investigate why the CDI injection failed for details");
            }
        } catch (Throwable t) {
            logger.warning("Failure getting accounts");
            logException(t);
        }

        logger.fine("Returning " + size + " accounts");
        if (logger.isLoggable(Level.FINE)) for (int index = 0; index < size; index++) {
            Account account = accountList.get(index);
            logger.fine("account[" + index + "]=" + account);
        }
        return accountList;
    }

    /**
     * Pagination of accounts
     * @param pageNumber
     * @param pageSize
     * @return
     */
    public List<Account> getAllAccounts(Integer pageNumber, Integer pageSize) {
        List<Account> accountList = null;
        int size = 0;

        try {
            logger.fine("Entering getAccounts");
            if (repository != null) {
                logger.warning("Getting Page " + pageNumber + " with this many results on the page " + pageSize);
                var page = Pagination.page(pageNumber).size(pageSize);
                Span allAccountsInPagination = tracer.buildSpan("repository.findAll(page).collect(Collectors.toList())").start();
                try(Scope childScope = tracer.scopeManager().activate(allAccountsInPagination)) {
                accountList = repository.findAll(page).collect(Collectors.toList());
                } finally {
                    allAccountsInPagination.finish();
                }
                size = accountList.size();
            } else {
                logger.warning("accountDB is null, so returning empty array.  Investigate why the CDI injection failed for details");
            }
        } catch (Throwable t) {
            logger.warning("Failure getting accounts");
            logException(t);
        }

        logger.fine("Returning " + size + " accounts");
        if (logger.isLoggable(Level.FINE)) for (int index = 0; index < size; index++) {
            Account account = accountList.get(index);
            logger.fine("account[" + index + "]=" + account);
        }
        return accountList;
    }

    public List<Account> getAccountsByOwner(List<String> owners) {
        var accountList = new ArrayList<Account>();
        int size = 0;

        try {
            logger.fine("Entering getAccounts");
            if (repository != null) {
                Span allAccountsByOwners = tracer.buildSpan("repository.findAll(page).collect(Collectors.toList())").start();
                try(Scope childScope = tracer.scopeManager().activate(allAccountsByOwners)) {
                    // @rtclauss
                    // This is inefficient but a hacky workaround until the commented out section below is fixed
                    // This doesn't work wither as a regular stream or a parallel stream
                    /* accountList = owners.stream()
                            .map(ownerName -> repository.findByOwner(ownerName))
                            .flatMap(Optional::stream) // Filter out the accounts we didn't find.
                            .collect(Collectors.toList());
                    */
                    // @rtclauss an old school way of looping
                    for(String ownerName: owners){
                        var account = repository.findByOwner(ownerName);
                        if(account.isPresent()){
                            accountList.add(account.get());
                        }
                    }

                    // @rtclauss
                    // This is broken due to a bug in JNoSQL
                    // GH Issue https://github.com/eclipse/jnosql-communication-driver/issues/185
                    // accountList = repository.findByOwnerIn(owners);
                } finally {
                    allAccountsByOwners.finish();
                }
                size = accountList.size();
            } else {
                logger.warning("accountDB is null, so returning empty array.  Investigate why the CDI injection failed for details");
            }
        } catch (Throwable t) {
            logger.warning("Failure getting accounts");
            logException(t);
        }

        logger.fine("Returning " + size + " accounts");
        if (logger.isLoggable(Level.FINE)) for (int index = 0; index < size; index++) {
            Account account = accountList.get(index);
            logger.fine("account[" + index + "]=" + account);
        }
        return accountList;
    }

    public Account getAccount(String id) {
        Optional<Account> account = null;
        logger.fine("Entering getAccount");
        Span findByIdSpan = tracer.buildSpan("repository.findById(id)").start();
        try(Scope childScope = tracer.scopeManager().activate(findByIdSpan)) {
            account = repository.findById(id);
        } catch (Throwable t) {
            logger.warning("Unknown error finding account for " + id);
            logException(t);
        }finally {
            findByIdSpan.finish();
        }
        return account.get();
    }

    public Account getAccountByOwnerName(String name) {
        Optional<Account> account = null;
        logger.fine("Entering getAccountsByName");
        Span findByOwnerNameSpan = tracer.buildSpan("repository.findByOwner(name).collect(Collectors.toList())").start();
        try(Scope childScope = tracer.scopeManager().activate(findByOwnerNameSpan)) {
            account = repository.findByOwner(name);
        } catch (Throwable t) {
            logger.warning("Unknown error finding account for " + name);
            logException(t);
        }finally{
            findByOwnerNameSpan.finish();
        }
        return account.get();
    }

    public Feedback submitFeedback(String id, WatsonInput input) {
        String sentiment = "Unknown";
        Feedback feedback = null;
        try {
            if (!initialized) initialize();

            logger.fine("Getting account for " + id + " in submitFeedback");
            Account account = getAccount(id);

            if (account != null) {
                int freeTrades = account.getFree();
                Span callWatsonSpan = tracer.buildSpan("utilities.invokeWatson(watsonClient, watsonId, watsonPwd, input)").start();
                try(Scope childScope = tracer.scopeManager().activate(callWatsonSpan)) {
                    feedback = utilities.invokeWatson(watsonClient, watsonId, watsonPwd, input);
                }finally{
                    callWatsonSpan.finish();
                }
                freeTrades += feedback.getFree();

                account.setFree(freeTrades);
                account.setSentiment(feedback.getSentiment());
                saveAccountToDb(account);

                logger.info("Returning feedback: " + feedback.toString());
            } else {
                logger.warning("Account not found for " + id + " in submitFeedback");
            }
        } catch (Throwable t) {
            logger.warning("Failure submitting feedback for " + id);
            logException(t);
        }

        return feedback;
    }

    public Account deleteAccount(String id) {
        Account account = null;
        logger.fine("Entering deleteAccount for " + id);
        Span deleteAccountSpan = tracer.buildSpan(" repository.deleteById(id)").start();
        try(Scope childScope = tracer.scopeManager().activate(deleteAccountSpan)) {
            account = getAccount(id);

            if (account != null) {
                String owner = account.getOwner();
                logger.fine("Deleting account for " + owner);

                repository.deleteById(id);

                logger.fine("Successfully deleted account for " + owner); //exception would have been thrown otherwise
            } else {
                logger.warning("Account not found for " + id + " in deleteAccount");
            }
        } catch (Throwable t) {
            logger.warning("Error occurred in deleteAccount for " + id);
            logException(t);
        }finally {
            deleteAccountSpan.finish();
        }

        return account;
    }

    public Account updateAccount(String id, double portfolioTotal) {
        logger.fine("Entering updateAccount");

        Account account = null;
        try {
            account = getAccount(id); //this computes new loyalty, etc.

            if (account != null) {
                String owner = account.getOwner();
                String loyalty = calculateLoyalty(account, portfolioTotal);
                account.setLoyalty(loyalty);

                double commission = utilities.getCommission(loyalty);

                int free = account.getFree();
                if (free > 0) { //use a free trade if available
                    free--;
                    commission = 0.0;

                    logger.info("Using free trade for " + owner);
                    account.setFree(free);
                } else {
                    double commissions = account.getCommissions();
                    commissions += commission;

                    double balance = account.getBalance();
                    balance -= commission;

                    logger.fine("Charging commission of $" + commission + " for " + owner);
                    account.setCommissions(commissions);
                    account.setBalance(balance);
                }
                logger.fine("Updating account into Cloudant: " + account);
                account = saveAccountToDb(account);
            } else {
                logger.warning("Account is null for " + id + " in updateAccount");
            }
        } catch (Throwable t) {
            logger.warning("Error in updateAccount for " + id);
            logException(t);
        }

        return account;
    }

    public String calculateLoyalty(Account account, double portfolioTotal) {
        String loyalty;
        logger.fine("Invoking external business rule for " + account.getId());

        //this can be a call to either IBM ODM, or my simple Lambda function alternative, depending on the URL configured in the CR yaml
        Span odmSpan = tracer.buildSpan("utilities.invokeODM").start();
        try(Scope childScope = tracer.scopeManager().activate(odmSpan)) {
            loyalty = utilities.invokeODM(odmClient, odmId, odmPwd, account.getOwner(),
                    portfolioTotal /* This should be the portfolio total value*/,
                    account.getLoyalty(), this.token.getName());
        }finally {
            odmSpan.finish();
        }
        return loyalty;
    }

    @Traced
    private void initialize() {
        if (watsonId != null) {
            logger.info("Watson initialization completed successfully!");
        } else {
            logger.warning("WATSON_ID config property is null");
        }

        if (odmId != null) {
            logger.info("Initialization complete");
        } else {
            logger.warning("ODM_ID config property is null");
        }
        initialized = true;
    }

    private void logException(Throwable t) {
        logger.warning(t.getClass().getName() + ": " + t.getMessage());

        //only log the stack trace if the level has been set to at least INFO
        if (logger.isLoggable(Level.INFO)) {
            StringWriter writer = new StringWriter();
            t.printStackTrace(new PrintWriter(writer));
            logger.info(writer.toString());
        }
    }

    private Account saveAccountToDb(Account account) {
        Span saveInDB = tracer.buildSpan("repository.save(account)").start();
        Account response;
        try(Scope childScope = tracer.scopeManager().activate(saveInDB)) {
            response = repository.save(account);
        }finally{
            saveInDB.finish();
        }
        return response;
    }
}
