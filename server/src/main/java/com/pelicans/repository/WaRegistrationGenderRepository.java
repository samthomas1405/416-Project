package com.pelicans.repository;

import com.pelicans.model.WaRegistrationGenderDoc;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WaRegistrationGenderRepository extends MongoRepository<WaRegistrationGenderDoc, String> {
    List<WaRegistrationGenderDoc> findByStateAbbrAndCountyName(String stateAbbr, String countyName);
    List<WaRegistrationGenderDoc> findByStateAbbr(String stateAbbr);
}



