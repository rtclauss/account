package com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.jnosql.db;

import com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.json.Account;
import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends CouchbaseRepository<Account, String> {
}
