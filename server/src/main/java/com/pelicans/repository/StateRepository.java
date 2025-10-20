package com.pelicans.repository;

import com.pelicans.model.State;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface StateRepository extends MongoRepository<State, String> {
  State findByStateFips(String stateFips);
}
