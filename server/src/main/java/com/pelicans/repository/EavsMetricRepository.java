package com.pelicans.repository;

import com.pelicans.model.EavsMetric;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EavsMetricRepository extends MongoRepository<EavsMetric, String> {
  EavsMetric findByStateId(String stateId);
}


