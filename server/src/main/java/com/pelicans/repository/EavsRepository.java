package com.pelicans.repository;

import com.pelicans.model.EavsDoc;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EavsRepository extends MongoRepository<EavsDoc, String> {
    List<EavsDoc> findByYearAndStateAbbr(Integer year, String stateAbbr);
    List<EavsDoc> findByYear(Integer year);
    Optional<EavsDoc> findByYearAndStateAbbrAndFips5(Integer year, String stateAbbr, String fips5);
    List<EavsDoc> findByYearAndFips5(Integer year, String fips5);
}



