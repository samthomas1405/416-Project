package com.pelicans.repository;

import com.pelicans.model.GeoStateDoc;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GeoStateRepository extends MongoRepository<GeoStateDoc, String> {
    Optional<GeoStateDoc> findByStateAbbr(String stateAbbr);
    Optional<GeoStateDoc> findByStateFips(String stateFips);
}

