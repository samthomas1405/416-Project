package com.pelicans.repository;

import com.pelicans.model.GeoCountyDoc;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GeoCountyRepository extends MongoRepository<GeoCountyDoc, String> {
}

