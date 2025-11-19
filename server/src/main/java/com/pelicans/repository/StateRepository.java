package com.pelicans.repository;

import com.pelicans.model.StateDoc;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StateRepository extends MongoRepository<StateDoc, String> {
    Optional<StateDoc> findByStateAbbr(String stateAbbr);
    Optional<StateDoc> findByStateFips(String stateFips);
}
