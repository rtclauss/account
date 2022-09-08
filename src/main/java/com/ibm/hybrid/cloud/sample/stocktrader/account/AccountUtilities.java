/*
       Copyright 2020-2022 IBM Corp All Rights Reserved

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

import com.ibm.hybrid.cloud.sample.stocktrader.account.client.ODMClient;
import com.ibm.hybrid.cloud.sample.stocktrader.account.client.WatsonClient;
import com.ibm.hybrid.cloud.sample.stocktrader.account.json.Feedback;
import com.ibm.hybrid.cloud.sample.stocktrader.account.json.LoyaltyChange;
import com.ibm.hybrid.cloud.sample.stocktrader.account.json.ODMLoyaltyRule;
import com.ibm.hybrid.cloud.sample.stocktrader.account.json.WatsonInput;
import com.ibm.hybrid.cloud.sample.stocktrader.account.json.WatsonOutput;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

//Logging (JSR 47)
import java.util.logging.Level;
import java.util.logging.Logger;

//JDBC 4.0 (JSR 221)
import java.sql.SQLException;

//mpOpenTracing 1.3
import org.eclipse.microprofile.opentracing.Traced;

//JMS 2.0
import javax.annotation.Resource;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;

//JSON-P 1.1 (JSR 353).  This replaces my old usage of IBM's JSON4J (com.ibm.json.java.JSONObject)
import javax.json.JsonObject;

//Servlet 4.0
import javax.servlet.http.HttpServletRequest;

/** Utility class that wraps communication with various types of services in the cloud */
public class AccountUtilities {
	private static Logger logger = Logger.getLogger(AccountUtilities.class.getName());

	private boolean odmBroken   = false; //used to only report failures of calls to ODM once, rather than every time

	//Our ODM rule will return its own values for levels, generally in all caps
	private static final String BASIC    = "Basic";
	private static final String BRONZE   = "Bronze";
	private static final String SILVER   = "Silver";
	private static final String GOLD     = "Gold";
	private static final String PLATINUM = "Platinum";

	@Resource(lookup = "jms/Portfolio/NotificationQueue")
	private Queue queue;
	@Resource(lookup = "jms/Portfolio/NotificationQueueConnectionFactory")
	private QueueConnectionFactory queueCF;

	private static SimpleDateFormat timestampFormatter = null;

	private static final String mqId = System.getenv("MQ_ID");
	private static final String mqPwd = System.getenv("MQ_PASSWORD");

	/** Invoke a business rule to determine the loyalty level corresponding to an account balance */
	@Traced
	String invokeODM(ODMClient odmClient, String odmId, String odmPwd, String owner, double overallTotal, String oldLoyalty, HttpServletRequest request) {
		String loyalty = null;
		ODMLoyaltyRule input = new ODMLoyaltyRule(overallTotal);
		try {
			String credentials = odmId+":"+odmPwd;
			String basicAuth = "Basic "+Base64.getEncoder().encodeToString(credentials.getBytes());

			try {
				//call the LoyaltyLevel business rule to get the current loyalty level of this portfolio
				logger.fine("Calling loyalty-level ODM business rule for "+owner);
				ODMLoyaltyRule result = odmClient.getLoyaltyLevel(basicAuth, input);

				loyalty = result.determineLoyalty();
				logger.fine("New loyalty level for "+owner+" is "+loyalty);
			} catch (Throwable t) {
				logger.warning("Error invoking ODM:" + t.getClass().getName() + ": "+t.getMessage() + ".  Loyalty level will remain unchanged.");
				if (!odmBroken) logException(t);
				odmBroken = true; //so logs aren't full of this stack trace on every getAccount
			}

			if ((oldLoyalty==null) || (loyalty==null)) return loyalty;
			if (!oldLoyalty.equalsIgnoreCase(loyalty)) try {
				logger.info("Change in loyalty level detected.");

				LoyaltyChange message = new LoyaltyChange(owner, oldLoyalty, loyalty);
	
				String user = request.getRemoteUser(); //logged-in user
				if (user != null) message.setId(user);
	
				logger.fine(message.toString());
	
				invokeJMS(message);
			} catch (JMSException jms) { //in case MQ is not configured, just log the exception and continue
				logger.warning("Unable to send message to JMS provider.  Continuing without notification of change in loyalty level.");
				logException(jms);
				Exception linked = jms.getLinkedException(); //get the nested exception from MQ
				if (linked != null) logException(linked);
			} catch (Throwable t) { //in case MQ is not configured, just log the exception and continue
				logger.warning("An unexpected error occurred.  Continuing without notification of change in loyalty level.");
				logException(t);
			}
		} catch (Throwable t) {
			logger.warning("Unable to get loyalty level, via "+input.toString()+".  Using cached value instead");
			logException(t);
			loyalty = oldLoyalty;
		}
		return loyalty;
	}

	@Traced
	public String invokeODM(ODMClient odmClient, String odmId, String odmPwd, String owner, double overallTotal, String oldLoyalty, String user) {
		String loyalty = null;
		ODMLoyaltyRule input = new ODMLoyaltyRule(overallTotal);
		try {
			String credentials = odmId+":"+odmPwd;
			String basicAuth = "Basic "+Base64.getEncoder().encodeToString(credentials.getBytes());

			try {
				//call the LoyaltyLevel business rule to get the current loyalty level of this portfolio
				logger.fine("Calling loyalty-level ODM business rule for "+owner);
				ODMLoyaltyRule result = odmClient.getLoyaltyLevel(basicAuth, input);

				loyalty = result.determineLoyalty();
				logger.fine("New loyalty level for "+owner+" is "+loyalty);
			} catch (Throwable t) {
				logger.warning("Error invoking ODM:" + t.getClass().getName() + ": "+t.getMessage() + ".  Loyalty level will remain unchanged.");
				if (!odmBroken) logException(t);
				odmBroken = true; //so logs aren't full of this stack trace on every getAccount
			}

			if ((oldLoyalty==null) || (loyalty==null)) return loyalty;
			if (!oldLoyalty.equalsIgnoreCase(loyalty)) try {
				logger.info("Change in loyalty level detected.");

				LoyaltyChange message = new LoyaltyChange(owner, oldLoyalty, loyalty);

				if (user != null) message.setId(user);

				logger.fine(message.toString());

				invokeJMS(message);
			} catch (JMSException jms) { //in case MQ is not configured, just log the exception and continue
				logger.warning("Unable to send message to JMS provider.  Continuing without notification of change in loyalty level.");
				logException(jms);
				Exception linked = jms.getLinkedException(); //get the nested exception from MQ
				if (linked != null) logException(linked);
			} catch (Throwable t) { //in case MQ is not configured, just log the exception and continue
				logger.warning("An unexpected error occurred.  Continuing without notification of change in loyalty level.");
				logException(t);
			}
		} catch (Throwable t) {
			logger.warning("Unable to get loyalty level, via "+input.toString()+".  Using cached value instead");
			logException(t);
			loyalty = oldLoyalty;
		}
		return loyalty;
	}

	/** Use the Watson Tone Analyzer to determine the user's sentiment */
	@Traced
	public Feedback invokeWatson(WatsonClient watsonClient, String watsonId, String watsonPwd, WatsonInput input) {
		String sentiment = "Unknown";
		try {
			String credentials = watsonId + ":" + watsonPwd; //Watson accepts basic auth
			String authorization = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());

			logger.info("Calling Watson Tone Analyzer");

			WatsonOutput watson = watsonClient.getTone(authorization, input);
			sentiment = watson.determineSentiment();
		} catch (Throwable t) {
			logger.warning("Error from Watson, with following input: "+input.toString());
			logException(t);
		}

		int freeTrades = 1;
		String message = "Thanks for providing feedback.  Have a free trade on us!";

		if ("Anger".equalsIgnoreCase(sentiment)) {
			logger.info("Tone is angry");
			freeTrades = 3;
			message = "We're sorry you are upset.  Have three free trades on us!";
		} else if ("Unknown".equalsIgnoreCase(sentiment)) {
			logger.info("Tone is unknown");
			freeTrades = 0;
			message = "Error communicating with the Watson Tone Analyzer";
		}

		Feedback feedback = new Feedback(message, freeTrades, sentiment);
		return feedback;
	}

	/** Send a JSON message to our notification queue. */
	@Traced
	void invokeJMS(Object json) throws JMSException {
		if (queueCF != null) {
			logger.fine("Preparing to send a JMS message");

			QueueConnection connection = queueCF.createQueueConnection(mqId, mqPwd);
			QueueSession session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

			String contents = json.toString();
			TextMessage message = session.createTextMessage(contents);

			logger.info("Sending "+contents+" to "+queue.getQueueName());

			//"mqclient" group needs "put" authority on the queue for next two lines to work
			QueueSender sender = session.createSender(queue);
			sender.setDeliveryMode(DeliveryMode.PERSISTENT);
			sender.send(message);

			sender.close();
			session.close();
			connection.close();

			logger.info("JMS Message sent successfully!"); //exception would have occurred otherwise
		} else {
			logger.warning("Unable to inject JMS QueueConnectionFactory - check your MQ configuration.  No JMS message will be sent.");
		}
	}

	public double getCommission(String loyalty) {
		//TODO: turn this into an ODM business rule or a FaaS function (such as in AWS Lambda)
		double commission = 9.99;
		logger.fine("Determining commission - loyalty level = "+loyalty);
		if (loyalty!= null) {
			if (loyalty.equalsIgnoreCase(BRONZE)) {
				commission = 8.99;
			} else if (loyalty.equalsIgnoreCase(SILVER)) {
				commission = 7.99;
			} else if (loyalty.equalsIgnoreCase(GOLD)) {
				commission = 6.99;
			} else if (loyalty.equalsIgnoreCase(PLATINUM)) {
				commission = 5.99;
			} 
		}
		logger.fine("Returning commission: "+commission);

		return commission;
	}

	static void logException(Throwable t) {
		logger.warning(t.getClass().getName()+": "+t.getMessage());

		//only log the stack trace if the level has been set to at least INFO
		if (logger.isLoggable(Level.INFO)) {
			StringWriter writer = new StringWriter();
			t.printStackTrace(new PrintWriter(writer));
			logger.info(writer.toString());
		}
	}
}
