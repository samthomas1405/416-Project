package com.pelicans.repository;

import com.pelicans.model.WaVoterDoc;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WaVoterRepository extends MongoRepository<WaVoterDoc, String> {
    Optional<WaVoterDoc> findByStateVoterId(String stateVoterId);
    List<WaVoterDoc> findByCountyName(String countyName);
    List<WaVoterDoc> findByCountyCodeAndPrecinctCode(String countyCode, String precinctCode);
}



