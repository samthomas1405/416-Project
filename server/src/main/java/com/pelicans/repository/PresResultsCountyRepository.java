package com.pelicans.repository;

import com.pelicans.model.PresResultsCountyDoc;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PresResultsCountyRepository extends MongoRepository<PresResultsCountyDoc, String> {
    List<PresResultsCountyDoc> findByStateAbbr(String stateAbbr);
    Optional<PresResultsCountyDoc> findByStateAbbrAndFips5(String stateAbbr, String fips5);
}



