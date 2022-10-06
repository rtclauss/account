package com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.jnosql.db;

import com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.json.Account;
import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountRepository extends CouchbaseRepository<Account, String> {

    List<Account> findByOwner(String ownerName);
}
