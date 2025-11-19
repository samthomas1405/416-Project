package com.pelicans.repository;

import com.pelicans.model.CvapCountyDoc;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CvapCountyRepository extends MongoRepository<CvapCountyDoc, String> {
    List<CvapCountyDoc> findByStateAbbrAndFips5(String stateAbbr, String fips5);
    List<CvapCountyDoc> findByStateAbbr(String stateAbbr);
    Optional<CvapCountyDoc> findByStateAbbrAndFips5AndCvapCategoryCode(String stateAbbr, String fips5, String cvapCategoryCode);
}



