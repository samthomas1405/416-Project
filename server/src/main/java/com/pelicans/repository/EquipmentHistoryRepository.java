package com.pelicans.repository;

import com.pelicans.model.EquipmentHistoryDoc;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipmentHistoryRepository extends MongoRepository<EquipmentHistoryDoc, String> {
    List<EquipmentHistoryDoc> findByStateAbbr(String stateAbbr);
    List<EquipmentHistoryDoc> findByStateAbbrAndYear(String stateAbbr, Integer year);
    List<EquipmentHistoryDoc> findByStateAbbrAndEquipmentCategory(String stateAbbr, String equipmentCategory);
    List<EquipmentHistoryDoc> findByYear(Integer year);
    Optional<EquipmentHistoryDoc> findByStateAbbrAndYearAndEquipmentCategory(String stateAbbr, Integer year, String equipmentCategory);
}

