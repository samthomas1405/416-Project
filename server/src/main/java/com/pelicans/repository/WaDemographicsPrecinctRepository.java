package com.pelicans.repository;

import com.pelicans.model.WaDemographicsPrecinctDoc;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WaDemographicsPrecinctRepository extends MongoRepository<WaDemographicsPrecinctDoc, String> {
    List<WaDemographicsPrecinctDoc> findByCountyCodeAndPrecinctCodeAndPrecinctPart(String countyCode, String precinctCode, String precinctPart);
    List<WaDemographicsPrecinctDoc> findByCountyCode(String countyCode);
}



