package com.ibm.hybrid.cloud.sample.stocktrader.account.jnosql.db;

import jakarta.nosql.Configurations;
import jakarta.nosql.Settings;
import jakarta.nosql.document.DocumentCollectionManager;
import jakarta.nosql.document.DocumentCollectionManagerFactory;
import jakarta.nosql.document.DocumentConfiguration;
import org.eclipse.jnosql.communication.couchdb.document.CouchDBConfigurations;
import org.eclipse.jnosql.communication.couchdb.document.CouchDBDocumentConfiguration;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;

@ApplicationScoped
// TODO Remove this class and replace with Microprofile Config settings
// https://www.jnosql.org/docs/config.html
public class CouchDBDocumentCollectionManagerProducer {
    private static Logger logger = Logger.getLogger(CouchDBDocumentCollectionManagerProducer.class.getName());

    // Also known as the db name
    @Inject
    @ConfigProperty(name = "CLOUDANT_DB", defaultValue = "account")
    private String collection;

    @Inject
    @ConfigProperty(name = "CLOUDANT_URL", defaultValue = "http://username:password@hostname:port")
    private String cloudantURL;

    @Inject
    @ConfigProperty(name = "CLOUDANT_ID", defaultValue = "user")
    private String cloudantId;

    @Inject
    @ConfigProperty(name = "CLOUDANT_PASSWORD", defaultValue = "password")
    private String cloudantPassword;

    private DocumentConfiguration configuration;

    private DocumentCollectionManagerFactory managerFactory;

    @PostConstruct
    public void init() {
        if (cloudantURL == null) {
            cloudantURL = System.getenv("CLOUDANT_URL");
        }
        if (cloudantId == null) {
            cloudantId = System.getenv("CLOUDANT_ID");
        }
        if (cloudantPassword == null) {
            cloudantPassword = System.getenv("CLOUDANT_PASSWORD");
        }
        if(collection ==null){
            collection = System.getenv("CLOUDANT_DB");
        }
        configuration = new CouchDBDocumentConfiguration();
        Settings.SettingsBuilder builder = Settings.builder();
        try {
            //System.out.println("Here is the cloudant url");
            System.out.println(cloudantURL);
            var cloudantUri = new URI(cloudantURL);
            builder.put(CouchDBConfigurations.PORT.get(), cloudantUri.getPort());
            builder.put(Configurations.USER.get(), cloudantId);
            builder.put(Configurations.PASSWORD.get(), cloudantPassword);
            builder.put(Configurations.HOST.get(), cloudantUri.getHost());
            managerFactory = configuration.get(builder.build());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Produces
    public DocumentCollectionManager getManager() {
        return managerFactory.get(collection);

    }
}
