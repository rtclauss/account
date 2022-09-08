package com.ibm.hybrid.cloud.sample.stocktrader.account.graphql;

import com.ibm.hybrid.cloud.sample.stocktrader.account.AccountUtilities;
import com.ibm.hybrid.cloud.sample.stocktrader.account.client.ODMClient;
import com.ibm.hybrid.cloud.sample.stocktrader.account.client.WatsonClient;
import com.ibm.hybrid.cloud.sample.stocktrader.account.jnosql.db.AccountRepository;
import com.ibm.hybrid.cloud.sample.stocktrader.account.json.Feedback;
import com.ibm.hybrid.cloud.sample.stocktrader.account.json.WatsonInput;
import com.ibm.hybrid.cloud.sample.stocktrader.account.json.graphql.Account;
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
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ApplicationScoped
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
            boolean ownerExistInRepo = repository.findByOwner(owner).findFirst().isPresent();
            if(ownerExistInRepo){
                logger.warning("Account already exists for: "+owner);
                throw new WebApplicationException("Account already exists for "+owner+"!", CONFLICT);
            }

            logger.fine("Creating account for " + account.getOwner());

            Account response = repository.save(account);
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
                accountList = repository.findAll().collect(Collectors.toList());
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
                accountList = repository.findAll(page).collect(Collectors.toList());
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
        try {
            account = repository.findById(id);
        } catch (Throwable t) {
            logger.warning("Unknown error finding account for " + id);
            logException(t);
        }
        return account.get();
    }

    public List<Account> getAccountsByName(String name) {
        List<Account> accounts = null;
        logger.fine("Entering getAccountsByName");
        try {
            accounts = repository.findByOwner(name).collect(Collectors.toList());
        } catch (Throwable t) {
            logger.warning("Unknown error finding account for " + name);
            logException(t);
        }
        return accounts;
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

                feedback = utilities.invokeWatson(watsonClient, watsonId, watsonPwd, input);
                freeTrades += feedback.getFree();

                account.setFree(freeTrades);
                account.setSentiment(feedback.getSentiment());
                repository.save(account);

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

    public String deleteAccount(String id) {
        Account account = null;
        logger.fine("Entering deleteAccount for " + id);
        try {
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
        }

        return id;
    }

    public Account updateAccount(String id, double tradeAmount) {
        logger.fine("Entering updateAccount");

        Account account = null;
        try {
            account = getAccount(id); //this computes new loyalty, etc.

            if (account != null) {
                String owner = account.getOwner();
                String loyalty = calculateLoyalty(account);

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
                repository.save(account);
            } else {
                logger.warning("Account is null for " + id + " in updateAccount");
            }
        } catch (Throwable t) {
            logger.warning("Error in updateAccount for " + id);
            logException(t);
        }

        return account;
    }

    public String calculateLoyalty(Account account) {
        logger.fine("Invoking external business rule for " + account.getId());

        //this can be a call to either IBM ODM, or my simple Lambda function alternative, depending on the URL configured in the CR yaml
        // TODO I know this isn't right but I want to press ahead. The old logic passed the "amount" of the current trade to calculate the loyalty
        String loyalty = utilities.invokeODM(odmClient, odmId, odmPwd, account.getOwner(), account.getBalance(), account.getLoyalty(), this.token.getName());
        if ((loyalty != null) && !loyalty.equalsIgnoreCase(account.getLoyalty())) { //don't rev the Cloudant doc if nothing's changed
            account.setLoyalty(loyalty);

            int free = account.getFree();
            account.setNextCommission(free > 0 ? 0.0 : utilities.getCommission(loyalty));

            // logger.fine("Calling accountDB.update() for " + account.get_id() + " in getAccount due to new loyalty level");
            // accountDB.update(account);
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
}
