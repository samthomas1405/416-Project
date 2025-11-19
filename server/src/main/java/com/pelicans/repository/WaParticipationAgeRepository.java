package com.pelicans.repository;

import com.pelicans.model.WaParticipationAgeDoc;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WaParticipationAgeRepository extends MongoRepository<WaParticipationAgeDoc, String> {
    List<WaParticipationAgeDoc> findByStateAbbrAndCountyName(String stateAbbr, String countyName);
    List<WaParticipationAgeDoc> findByStateAbbr(String stateAbbr);
}



