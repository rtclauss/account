package com.ibm.hybrid.cloud.sample.stocktrader.account.db;

import com.ibm.hybrid.cloud.sample.stocktrader.account.json.Account;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Repository;

import java.util.stream.Stream;


// TODO move to PageableRepository
@Repository
public interface AccountRepository extends CrudRepository<Account, String> {
    Stream<Account> findByOwner(String owner);
}
