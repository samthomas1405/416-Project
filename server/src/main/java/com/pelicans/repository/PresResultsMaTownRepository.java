package com.pelicans.repository;

import com.pelicans.model.PresResultsMaTownDoc;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PresResultsMaTownRepository extends MongoRepository<PresResultsMaTownDoc, String> {
    List<PresResultsMaTownDoc> findByStateAbbr(String stateAbbr);
    Optional<PresResultsMaTownDoc> findByStateAbbrAndTownName(String stateAbbr, String townName);
}



