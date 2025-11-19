package com.pelicans.repository;

import com.pelicans.model.EavsMetricDoc;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EavsMetricRepository extends MongoRepository<EavsMetricDoc, String> {
  EavsMetricDoc findByStateId(String stateId);
}


