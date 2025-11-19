package com.pelicans.repository;

import com.pelicans.model.WaDemographicsCountyDoc;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WaDemographicsCountyRepository extends MongoRepository<WaDemographicsCountyDoc, String> {
    List<WaDemographicsCountyDoc> findByCountyCode(String countyCode);
    Optional<WaDemographicsCountyDoc> findByCountyCodeAndAgeGroup2024(String countyCode, String ageGroup2024);
}



