package com.ibm.hybrid.cloud.sample.stocktrader.account.jnosql.db;

import com.ibm.hybrid.cloud.sample.stocktrader.account.json.graphql.Account;
import jakarta.nosql.mapping.Pagination;
import jakarta.nosql.mapping.Repository;

import java.util.Optional;
import java.util.stream.Stream;


/**
 *
 */
public interface AccountRepository extends Repository<Account, String> {

    Stream<Account> findAll();

    Stream<Account> findAll(Pagination pagination);

    Stream<Account> findByOwner(String owner);

    Optional<Account> findById(String id);
}



