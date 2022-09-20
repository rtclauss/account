package com.ibm.hybrid.cloud.sample.stocktrader.account.db;

import jakarta.nosql.document.DocumentCollectionManager;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;


/**
 * This class reads MP-Config values for your given document store and instantiates the connection.
 *
 * For example, to instantiate CouchDB/Cloudant you need at least the following properties defined:
 * <pre>
 *     document=document
 *     document.settings.couchdb.host=couchdb.hostname.com
 *     document.settings.couchdb.port=5879
 *     document.settings.couchdb.username=username
 *     document.settings.couchdb.password=password
 *     # document.settings.couchdb.enable.ssl=true
 *     document.database=account
 *     document.provider=org.eclipse.jnosql.mapping.document.CouchDBDocumentConfiguration
 * </pre>
 *
 * While for MongoDB you need:
 * <pre>
 *     document=document
 *     # Use either host or URL not both as host will override the URL
 *     # document.settings.mongodb.host=couchdb.hostname.com
 *     document.settings.mongodb.user=username
 *     document.settings.mongodb.password=password
 *     # Use either host or URL not both as defining host will not allow URL to be instantiated
 *     document.settings.mongodb.url=mongodb+ssl://mongodb.com/
 *     document.provider=org.eclipse.jnosql.mapping.document.MongoDBDocumentConfiguration
 * </pre>
 *
 * The various properties can be viewed in the JNoSQL GitHub at
 * https://github.com/eclipse/jnosql-communication-driver/tree/master
 *
 * For CouchDB the list of properties is at: https://github.com/eclipse/jnosql-communication-driver/blob/master/couchdb-driver/src/main/java/org/eclipse/jnosql/communication/couchdb/document/CouchDBDocumentConfiguration.java
 * For MongoDB the list is at: https://github.com/eclipse/jnosql-communication-driver/blob/master/mongodb-driver/src/main/java/org/eclipse/jnosql/communication/mongodb/document/MongoDBDocumentConfigurations.java
 *
 *
 */
@ApplicationScoped
public class DocumentDBCollectionManagerProducer {
    @Inject
    @ConfigProperty(name = "document")
    private DocumentCollectionManager entityManager;

    @Produces
    public DocumentCollectionManager getManager(){
        return entityManager;
    }
}
