package com.pelicans.repository;

import com.pelicans.model.Equipment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface EquipmentRepository extends MongoRepository<Equipment, String> {
  List<Equipment> findByStateId(String stateId);
}
