package com.pelicans.repository;

import com.pelicans.model.FelonyPolicyDoc;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FelonyPolicyRepository extends MongoRepository<FelonyPolicyDoc, String> {
    Optional<FelonyPolicyDoc> findByStateAbbr(String stateAbbr);
    Optional<FelonyPolicyDoc> findByStateFips(String stateFips);
}



