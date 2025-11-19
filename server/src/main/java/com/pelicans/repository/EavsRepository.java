package com.pelicans.repository;

import com.pelicans.model.EavsDoc;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EavsRepository extends MongoRepository<EavsDoc, String> {
    List<EavsDoc> findByStateFips(String fips);
    List<EavsDoc> findByYearAndStateFips(Integer year, String stateFips);
    Optional<EavsDoc> findByYearAndStateFipsAndFips5(Integer year, String stateFips, String fips5);
    List<EavsDoc> findByYear(Integer year);
    List<EavsDoc> findByYearAndFips5(Integer year, String fips5);
}



