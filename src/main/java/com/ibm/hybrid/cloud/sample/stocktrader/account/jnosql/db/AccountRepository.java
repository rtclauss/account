package com.ibm.hybrid.cloud.sample.stocktrader.account.jnosql.db;

import com.ibm.hybrid.cloud.sample.stocktrader.account.json.graphql.Account;
import jakarta.nosql.mapping.Pagination;
import jakarta.nosql.mapping.Param;
import jakarta.nosql.mapping.Query;
import jakarta.nosql.mapping.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;


/**
 *
 */
public interface AccountRepository extends Repository<Account, String> {

    @Deprecated
    Stream<Account> findAll();

    Stream<Account> findAll(Pagination pagination);

//    @Query("select * from Account where owner in (@owners)")
    List<Account> findByOwnerIn( /* @Param("owners") */ List<String> owners);

    Optional<Account> findByOwner(String owner);

    Optional<Account> findById(String id);
}



