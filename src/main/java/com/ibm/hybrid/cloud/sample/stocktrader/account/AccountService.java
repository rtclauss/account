/*
       Copyright 2020 IBM Corp All Rights Reserved

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

// TODO: Using an in-memory HashMap temporarily - will switch to Cloudant shortly
package com.ibm.hybrid.cloud.sample.stocktrader.account;

import com.ibm.hybrid.cloud.sample.stocktrader.account.client.ODMClient;
import com.ibm.hybrid.cloud.sample.stocktrader.account.client.WatsonClient;
import com.ibm.hybrid.cloud.sample.stocktrader.account.json.Account;
import com.ibm.hybrid.cloud.sample.stocktrader.account.json.Feedback;
import com.ibm.hybrid.cloud.sample.stocktrader.account.json.WatsonInput;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

//Logging (JSR 47)
import java.util.logging.Level;
import java.util.logging.Logger;

//CDI 2.0
import javax.inject.Inject;
import javax.enterprise.context.RequestScoped;

//mpConfig 1.3
import org.eclipse.microprofile.config.inject.ConfigProperty;

//mpJWT 1.1
import org.eclipse.microprofile.auth.LoginConfig;

//mpMetrics 2.0
import org.eclipse.microprofile.metrics.annotation.Counted;

//mpOpenTracing 1.3
import org.eclipse.microprofile.opentracing.Traced;

//mpRestClient 1.3
import org.eclipse.microprofile.rest.client.inject.RestClient;

//Servlet 4.0
import javax.servlet.http.HttpServletRequest;

//JAX-RS 2.1 (JSR 339)
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.BadRequestException; //400 error
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;

@ApplicationPath("/")
@Path("/")
@LoginConfig(authMethod = "MP-JWT", realmName = "jwt-jaspi")
@RequestScoped //enable interceptors (note you need a WEB-INF/beans.xml in your war)
/** This microservice takes care of non-stock related attributes of a customer's account.  This includes
 *  commissions, account balance, sentiment, free trades, and loyalty level determination.  This version
 *  persists data to an IBM Cloudant non-SQL datastore.
 */
public class AccountService extends Application {
	private static Logger logger = Logger.getLogger(AccountService.class.getName());

	private static final double ERROR            = -1.0;
	private static final int    CONFLICT         = 409;         //odd that JAX-RS has no ConflictException
	private static final String FAIL             = "FAIL";      //trying to create a portfolio with this name will always throw a 400

	private static boolean initialized = false;
	private static boolean staticInitialized = false;

	private static HashMap<String, Account> accountData = null;

	private AccountUtilities utilities = new AccountUtilities();

	private @Inject @RestClient ODMClient odmClient;
	private @Inject @RestClient WatsonClient watsonClient;

	private @Inject @ConfigProperty(name = "ODM_ID", defaultValue = "odmAdmin") String odmId;
	private @Inject @ConfigProperty(name = "ODM_PWD", defaultValue = "odmAdmin") String odmPwd;
	private @Inject @ConfigProperty(name = "WATSON_ID", defaultValue = "apikey") String watsonId;
	private @Inject @ConfigProperty(name = "WATSON_PWD") String watsonPwd; //if using an API Key, it goes here
	private @Inject @ConfigProperty(name = "KAFKA_TOPIC", defaultValue = "stocktrader") String kafkaTopic;
	private @Inject @ConfigProperty(name = "KAFKA_ADDRESS", defaultValue = "") String kafkaAddress;

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

	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
//	@RolesAllowed({"StockTrader", "StockViewer"}) //Couldn't get this to work; had to do it through the web.xml instead :(
	public Account[] getAccounts() {
		Account[] accountArray = null;

		Collection<Account> accounts = accountData.values();
		if (accounts!=null) {
			accountArray = new Account[accountData.size()];
			Iterator<Account> iter = accounts.iterator();
			for (int index=0; iter.hasNext(); index++) {
				accountArray[index] = iter.next();
			}
		}

		return accountArray;
	}

	@POST
	@Path("/{owner}")
	@Produces(MediaType.APPLICATION_JSON)
	@Counted(name="accounts", displayName="Stock Trader accounts", description="Number of accounts created in the Stock Trader application")
	//	@RolesAllowed({"StockTrader"}) //Couldn't get this to work; had to do it through the web.xml instead :(
	public Account createAccount(@PathParam("owner") String owner) {
		Account account = null;
		if (owner != null) {
			if (owner.equalsIgnoreCase(FAIL)) {
				logger.warning("Throwing a 400 error for owner: "+owner);
				throw new BadRequestException("Invalid value for account owner: "+owner);
			}
			Account existing = accountData.get(owner);

			if (existing == null) {
				//loyalty="Basic", balance=50.0, commissions=0.0, free=0, sentiment="Unknown", nextCommission=9.99
				account = new Account(owner, "Basic", 50.0, 0.0, 0, "Unknown", 9.99);

				logger.info("Creating account for "+owner);

				accountData.put(owner, account);
			} else {
				logger.warning("Account already exists for: "+owner);
				throw new WebApplicationException("Account already exists for "+owner+"!", CONFLICT);			
			}

			logger.info("Account created successfully");
		}

		return account;
	}

	@GET
	@Path("/{owner}")
	@Produces(MediaType.APPLICATION_JSON)
//	@RolesAllowed({"StockTrader", "StockViewer"}) //Couldn't get this to work; had to do it through the web.xml instead :(
	public Account getAccount(@PathParam("owner") String owner, @QueryParam("total") double total, @Context HttpServletRequest request) throws IOException {
		Account account = accountData.get(owner);
		if (account != null) {
			if (total != ERROR) {
				String oldLoyalty = account.getLoyalty();

				String loyalty = utilities.invokeODM(odmClient, odmId, odmPwd, owner, total, oldLoyalty, request);
				account.setLoyalty(loyalty);

				int free = account.getFree();
				account.setNextCommission(free>0 ? 0.0 : utilities.getCommission(loyalty));

				accountData.put(owner, account);
			}

			logger.info("Returning "+account.toString());
		} else {
			logger.warning("No account found for "+owner);
		}

		return account;
	}

	@PUT
	@Path("/{owner}")
	@Produces(MediaType.APPLICATION_JSON)
//	@RolesAllowed({"StockTrader"}) //Couldn't get this to work; had to do it through the web.xml instead :(
	public Account updateAccount(@PathParam("owner") String owner, @QueryParam("total") double total, @Context HttpServletRequest request) throws IOException {
		Account account = getAccount(owner, total, request); //this computes new loyalty, etc.

		if (account!=null) {
			String loyalty = account.getLoyalty();

			double commission = utilities.getCommission(loyalty);

			int free = account.getFree();
			if (free > 0) { //use a free trade if available
				free--;
				commission = 0.0;

				logger.info("Using free trade for "+owner);
				account.setFree(free);
			} else {
				double commissions = account.getCommissions();
				commissions += commission;

				double balance = account.getBalance();
				balance -= commission;

				logger.info("Charging commission of $"+commission+" for "+owner);
				account.setCommissions(commissions);
				account.setBalance(balance);
			}

			accountData.put(owner, account);
		}

		return account;
	}

	@DELETE
	@Path("/{owner}")
	@Produces(MediaType.APPLICATION_JSON)
//	@RolesAllowed({"StockTrader"}) //Couldn't get this to work; had to do it through the web.xml instead :(
	public Account deleteAccount(@PathParam("owner") String owner) {
		Account account = accountData.remove(owner);

		logger.info("Successfully deleted portfolio for "+owner);

		return account; //maybe this method should return void instead?
	}

	@POST
	@Path("/{owner}/feedback")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
//	@RolesAllowed({"StockTrader"}) //Couldn't get this to work; had to do it through the web.xml instead :(
	public Feedback submitFeedback(@PathParam("owner") String owner, WatsonInput input, @Context HttpServletRequest request) throws IOException {
		String sentiment = "Unknown";
		try {
			initialize();
		} catch (Throwable t) {
			logger.warning("Error occurred during initialization");
			t.printStackTrace();
		}

		Account account = getAccount(owner, ERROR, request);
		int freeTrades = account.getFree();

		Feedback feedback = utilities.invokeWatson(watsonClient, watsonId, watsonPwd, input);
		freeTrades += feedback.getFree();

		account.setFree(freeTrades);
		account.setSentiment(feedback.getSentiment());

		logger.info("Returning feedback: "+feedback.toString());
		return feedback;
	}

	private static void staticInitialize() {
		if (!staticInitialized) {
			logger.info("Obtaining HashMap");

			accountData = new HashMap();

			logger.info("HashMap successfully obtained!"); //exception would have occurred otherwise

			staticInitialized = true;
		}
	}

	@Traced
	private void initialize() {
		if (!staticInitialized) staticInitialize();

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
}
