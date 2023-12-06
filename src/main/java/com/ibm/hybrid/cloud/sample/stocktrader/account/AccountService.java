/*
       Copyright 2020-2022 IBM Corp All Rights Reserved
       Copyright 2022-2023 Kyndryl Corp, All Rights Reserved

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package com.ibm.hybrid.cloud.sample.stocktrader.account;

import com.cloudant.client.api.ClientBuilder;
import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.Response;
import com.cloudant.client.org.lightcouch.DocumentConflictException;
import com.cloudant.client.org.lightcouch.NoDocumentException;
import com.ibm.hybrid.cloud.sample.stocktrader.account.client.ODMClient;
import com.ibm.hybrid.cloud.sample.stocktrader.account.client.WatsonClient;
import com.ibm.hybrid.cloud.sample.stocktrader.account.json.Account;
import com.ibm.hybrid.cloud.sample.stocktrader.account.json.Feedback;
import com.ibm.hybrid.cloud.sample.stocktrader.account.json.WatsonInput;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.auth.LoginConfig;
import org.eclipse.microprofile.config.inject.ConfigProperty;
// TODO Quarkus 3.6.x does not support OpenTelemetry Metics yet
//import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

//@ApplicationPath("/")
@Path("/account")
@LoginConfig(authMethod = "MP-JWT", realmName = "jwt-jaspi")
//@RequestScoped //enable interceptors (note you need a WEB-INF/beans.xml in your war)
/** This microservice takes care of non-stock related attributes of a customer's account.  This includes
 *  commissions, account balance, sentiment, free trades, and loyalty level determination.  This version
 *  persists data to a CouchDB-derived non-SQL datastore.
 */
@ApplicationScoped
public class AccountService {
    private static final Logger logger = Logger.getLogger(AccountService.class.getName());

    private static final double DONT_RECALCULATE = -1.0;
    private static final int CONFLICT = 409;         //odd that JAX-RS has no ConflictException
    private static final String FAIL = "FAIL";      //trying to create an account with this name will always throw a 400
    private static final String DELAY = "com.ibm.hybrid.cloud.sample.stocktrader.account.delayUpdate";

    //we'll define an mpMetrics gauge per loyalty level
    private static final String BASIC = "basic";
    private static final String BRONZE = "bronze";
    private static final String SILVER = "silver";
    private static final String GOLD = "gold";
    private static final String PLATINUM = "platinum";
    private static final String UNKNOWN = "unknown";
    private static final String DOLLARS = "USD";

    private boolean delayUpdate = false;

    Database accountDB;
    CloudantClient cloudantClient;

    private int basic = 0, bronze = 0, silver = 0, gold = 0, platinum = 0, unknown = 0; //loyalty level counts

    @Inject private AccountUtilities utilities;

    @Inject JsonWebToken jwt;

    @RestClient private ODMClient odmClient;
    @RestClient private WatsonClient watsonClient;

    @ConfigProperty(name = "odm.id", defaultValue = "odmAdmin") String odmId;
    @ConfigProperty(name = "odm.pwd", defaultValue = "odmAdmin") String odmPwd;
    @ConfigProperty(name = "watson.id", defaultValue = "apikey") String watsonId;
    @ConfigProperty(name = "watson.pwd") String watsonPwd; //if using an API Key, it goes here

    @ConfigProperty(name = "cloudant.url") String cloudantUrl;
    @ConfigProperty(name = "cloudant.id") String cloudantId;
    @ConfigProperty(name = "cloudant.password") String cloudantPassword;
    @ConfigProperty(name = "cloudant.db") String cloudantDb;


    // Injection/initialization takes place after the class is instantiated, so we create the connection to CouchDB/Cloudant
    // afterward the no-arg constructor is called.
    //	https://stackoverflow.com/questions/3406555/why-use-postconstruct#3406631
    @PostConstruct
    public void postConstruct() throws MalformedURLException {
        logger.fine("Constructing Cloudant/Couch Client");
        if (this.cloudantClient == null) {
            synchronized (this) {
                cloudantClient = ClientBuilder.url(new URL(cloudantUrl))
                        .username(cloudantId)
                        .password(cloudantPassword)
                        .build();
                accountDB = cloudantClient.database(cloudantDb, true);

                if (watsonClient != null) {
                    logger.info("Watson initialization completed successfully!");
                } else {
                    logger.warning("WATSON config properties are unset");
                }

                if (odmClient != null) {
                    logger.info("ODM initialization complete");
                } else {
                    logger.warning("ODM config properties are unset");
                }
            }
        }
    }

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"StockTrader", "StockViewer"})
    //Couldn't get this to work; had to do it through the web.xml instead :(
    public List<Account> getAccounts() throws IOException {
        List<Account> accountList = null;
        int size = 0;

        try {
            logger.fine("Entering getAccounts");
            if (accountDB != null) {
                accountList = accountDB.getAllDocsRequestBuilder().includeDocs(true).build().getResponse().getDocsAs(Account.class);
                size = accountList.size();
            } else {
                logger.warning("accountDB is null, so returning empty array.  Investigate why the CDI injection failed for details");
            }
        } catch (Throwable t) {
            logger.warning("Failure getting accounts");
            logException(t);
        }

        logger.fine("Returning " + size + " accounts");

/* Commenting out until https://github.com/OpenLiberty/open-liberty/issues/22592 is addressed
		try {
			basic=0; bronze=0; silver=0; gold=0; platinum=0; unknown=0; //reset loyalty level counts
			for (int index=0; index<size; index++) {
				Account account = accountArray[index];
				logger.fine("account["+index+"]="+account);

				String loyaltyLevel = account.getLoyalty();
				if (loyaltyLevel!=null) {
					if (loyaltyLevel.equalsIgnoreCase(BASIC)) basic++;
					else if (loyaltyLevel.equalsIgnoreCase(BRONZE)) bronze++;
					else if (loyaltyLevel.equalsIgnoreCase(SILVER)) silver++;
					else if (loyaltyLevel.equalsIgnoreCase(GOLD)) gold++;
					else if (loyaltyLevel.equalsIgnoreCase(PLATINUM)) platinum++;
					else unknown++;
				}
			}
		} catch (Throwable t) {
			logException(t);
		}
*/
        return accountList;
    }

    @POST
    @Path("/{owner}")
    @Produces(MediaType.APPLICATION_JSON)
//	@Counted(name="accounts", description="Number of accounts created in the Stock Trader application")
    @RolesAllowed({"StockTrader"}) //Couldn't get this to work; had to do it through the web.xml instead :(
    public Account createAccount(@PathParam("owner") String owner) {
        Account account = null;
        if (owner != null) try {
            if (owner.equalsIgnoreCase(FAIL)) {
                logger.warning("Throwing a 400 error for owner: " + owner);
                throw new BadRequestException("Invalid value for account owner: " + owner);
            }

            //loyalty="Basic", balance=50.0, commissions=0.0, free=0, sentiment="Unknown", nextCommission=9.99
            account = new Account(owner, "Basic", 50.0, 0.0, 0, "Unknown", 9.99);

            logger.fine("Creating account for " + owner);

            Response response = accountDB.save(account);
            if (response != null) {
                String id = response.getId();
                account.set_id(id);
                logger.fine("Created new account for " + owner + " with id " + id);
            } else {
                logger.warning("Failed to get response from accountDB.save()"); //shouldn't get here - exception should have been thrown if the save failed
            }

            logger.fine("Account created successfully: " + owner);
        } catch (DocumentConflictException conflict) {
            logger.warning("Account already exists for: " + owner);
            logException(conflict);
            throw new WebApplicationException("Account already exists for " + owner + "!", CONFLICT);
        } catch (Throwable t) {
            logger.warning("Failure to create account for " + owner);
            logException(t);
        }
        else {
            logger.warning("Owner is null in createAccount");
        }

        return account;
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"StockTrader", "StockViewer"})
    //Couldn't get this to work; had to do it through the web.xml instead :(
    public Account getAccount(@PathParam("id") String id, @QueryParam("total") double total) throws IOException {
        Account account = null;
        logger.fine("Entering getAccount");
        try {
            account = accountDB.find(Account.class, id);
            if (account != null) {
                if (total == DONT_RECALCULATE) {
                    logger.fine("Skipping recalculation of loyalty level and next commission as requested");
                } else {
                    String owner = account.getOwner();
                    String oldLoyalty = account.getLoyalty();

                    logger.fine("Invoking external business rule for " + id);
                    //this can be a call to either IBM ODM, or my simple Lambda function alternative, depending on the URL configured in the CR yaml
                    String loyalty = utilities.invokeODM(odmClient, odmId, odmPwd, owner, total, oldLoyalty, jwt.getName());
                    if ((loyalty != null) && !loyalty.equalsIgnoreCase(oldLoyalty)) { //don't rev the Cloudant doc if nothing's changed
                        account.setLoyalty(loyalty);

                        int free = account.getFree();
                        account.setNextCommission(free > 0 ? 0.0 : utilities.getCommission(loyalty));

                        if (!delayUpdate) { //if called from updateAccount, let it drive the update to Cloudant
                            logger.fine("Calling accountDB.update() for " + id + " in getAccount due to new loyalty level");
                            accountDB.update(account);
                        }
                    }
                }

                logger.fine("Returning " + account.toString());
            } else {
                logger.warning("Got null in getAccount for " + id + ", rather than expected NoDocumentException");
            }
        } catch (NoDocumentException t) {
            logger.warning("Unable to find account for " + id);
            logException(t);
        } catch (Throwable t) {
            logger.warning("Unknown error finding account for " + id);
            logException(t);
        }

        return account;
    }

    @PUT
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"StockTrader"}) //Couldn't get this to work; had to do it through the web.xml instead :(
    public Account updateAccount(@PathParam("id") String id, @QueryParam("total") double total) throws IOException {
        logger.fine("Entering updateAccount");
        this.delayUpdate = true;

        Account account = null;
        try {
            account = getAccount(id, total); //this computes new loyalty, etc.

            if (account != null) {
                String owner = account.getOwner();
                String loyalty = account.getLoyalty();

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
                accountDB.update(account);
            } else {
                logger.warning("Account is null for " + id + " in updateAccount");
            }
        } catch (Throwable t) {
            logger.warning("Error in updateAccount for " + id);
            logException(t);
        }

        return account;
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"StockTrader"}) //Couldn't get this to work; had to do it through the web.xml instead :(
    public Account deleteAccount(@PathParam("id") String id) {
        Account account = null;
        logger.fine("Entering deleteAccount for " + id);
        try {
            account = accountDB.find(Account.class, id); //this is sometimes failing with "Error: not_found. Reason: deleted"...

            if (account != null) {
                String owner = account.getOwner();
                logger.fine("Deleting account for " + owner);

                accountDB.remove(account);

                logger.fine("Successfully deleted account for " + owner); //exception would have been thrown otherwise
            } else {
                logger.warning("Account not found for " + id + " in deleteAccount");
            }
        } catch (Throwable t) {
            logger.warning("Error occurred in deleteAccount for " + id);
            logException(t);
        }

        return account; //maybe this method should return void instead?
    }

    @POST
    @Path("/{id}/feedback")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"StockTrader"}) //Couldn't get this to work; had to do it through the web.xml instead :(
    public Feedback submitFeedback(@PathParam("id") String id, WatsonInput input) throws IOException {
        String sentiment = "Unknown";
        Feedback feedback = null;
        try {
            logger.fine("Getting account for " + id + " in submitFeedback");
            Account account = getAccount(id, DONT_RECALCULATE);

            if (account != null) {
                int freeTrades = account.getFree();

                feedback = utilities.invokeWatson(watsonClient, watsonId, watsonPwd, input);
                freeTrades += feedback.getFree();

                account.setFree(freeTrades);
                account.setSentiment(feedback.getSentiment());

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

    /* Commenting out until https://github.com/OpenLiberty/open-liberty/issues/22592 is addressed
        @Gauge(name="account_loyalty", tags="level=basic", displayName="Basic", unit=MetricUnits.NONE)
        public int getBasic() {
            return basic;
        }

        @Gauge(name="account_loyalty", tags="level=bronze", displayName="Bronze", unit=MetricUnits.NONE)
        public int getBronze() {
            return bronze;
        }

        @Gauge(name="account_loyalty", tags="level=silver", displayName="Silver", unit=MetricUnits.NONE)
        public int getSilver() {
            return silver;
        }

        @Gauge(name="account_loyalty", tags="level=gold", displayName="Gold", unit=MetricUnits.NONE)
        public int getGold() {
            return gold;
        }

        @Gauge(name="account_loyalty", tags="level=platinum", displayName="Platinum", unit=MetricUnits.NONE)
        public int getPlatinum() {
            return platinum;
        }

        @Gauge(name="account_loyalty", tags="level=unknown", displayName="Unknown", unit=MetricUnits.NONE)
        public int getUnknown() {
            return unknown;
        }
    */
    private void logException(Throwable t) {
        logger.warning(t.getClass().getName() + ": " + t.getMessage());

        //only log the stack trace if the level has been set to at least INFO
        if (logger.isLoggable(Level.INFO)) {
            StringWriter writer = new StringWriter();
            t.printStackTrace(new PrintWriter(writer));
            logger.severe(writer.toString());
        }
    }
}
