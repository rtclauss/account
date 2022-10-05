package com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.couchbase.config.AbstractCouchbaseConfiguration;
import org.springframework.data.couchbase.repository.config.EnableCouchbaseRepositories;

@Configuration
@EnableCouchbaseRepositories(basePackages = "com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql")
public class CouchbaseConfig extends AbstractCouchbaseConfiguration {
    private final Environment env;

    @Autowired
    public CouchbaseConfig(Environment env) {
        this.env = env;
    }

    @Override
    public String getConnectionString() {
        return env.getProperty("spring.couchbase.connection-string");
    }

    @Override
    public String getUserName() {
        return env.getProperty("spring.couchbase.username");
    }

    @Override
    public String getPassword() {
        return env.getProperty("spring.couchbase.password");
    }

    @Override
    public String getBucketName() {
        return env.getProperty("spring.couchbase.bucket");
    }

    @Override
    protected boolean autoIndexCreation() {
        return true;
    }
}
