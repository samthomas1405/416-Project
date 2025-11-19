package com.pelicans.repository;

import com.pelicans.model.EquipmentDeviceDoc;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipmentDeviceRepository extends MongoRepository<EquipmentDeviceDoc, String> {
    List<EquipmentDeviceDoc> findByStateAbbr(String stateAbbr);
    List<EquipmentDeviceDoc> findByStateAbbrAndFipsCode(String stateAbbr, String fipsCode);
}



