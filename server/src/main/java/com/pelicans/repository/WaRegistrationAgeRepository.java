package com.pelicans.repository;

import com.pelicans.model.WaRegistrationAgeDoc;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WaRegistrationAgeRepository extends MongoRepository<WaRegistrationAgeDoc, String> {
    List<WaRegistrationAgeDoc> findByStateAbbrAndCountyName(String stateAbbr, String countyName);
    List<WaRegistrationAgeDoc> findByStateAbbr(String stateAbbr);
}



