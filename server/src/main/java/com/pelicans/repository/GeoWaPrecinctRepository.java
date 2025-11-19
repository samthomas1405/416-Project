package com.pelicans.repository;

import com.pelicans.model.GeoWaPrecinctDoc;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GeoWaPrecinctRepository extends MongoRepository<GeoWaPrecinctDoc, String> {
}

