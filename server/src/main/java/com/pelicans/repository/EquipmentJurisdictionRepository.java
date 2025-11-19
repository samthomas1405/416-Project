package com.pelicans.repository;

import com.pelicans.model.EquipmentJurisdictionDoc;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipmentJurisdictionRepository extends MongoRepository<EquipmentJurisdictionDoc, String> {
    List<EquipmentJurisdictionDoc> findByStateAbbr(String stateAbbr);
    Optional<EquipmentJurisdictionDoc> findByStateAbbrAndFipsCode(String stateAbbr, String fipsCode);
}



