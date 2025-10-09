// package com.pelicans.controller;

// import com.pelicans.model.Equipment;
// import com.pelicans.repository.EquipmentRepository;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.PathVariable;
// import org.springframework.web.bind.annotation.CrossOrigin;
// import org.springframework.web.bind.annotation.RestController;
// import org.springframework.web.bind.annotation.RequestMapping;

// import java.util.List;

// @Profile("web")
// @ConditionalOnProperty(name = "features.equipment.enabled", havingValue = "true", matchIfMissing = false)
// @RestController
// @RequestMapping("/api/equipment")
// public class EquipmentController {

//   private final EquipmentRepository equipmentRepository;

//   @Autowired
//   public EquipmentController(EquipmentRepository equipmentRepository) {
//     this.equipmentRepository = equipmentRepository;
//     // replace with real data
//     Equipment test = new Equipment("Model A", 120, "OS X", Equipment.Certification.VVSG_1_0, false);
//     equipmentRepository.save(test);
//   }

//   @GetMapping("/{stateId}")
//   public List<Equipment> getEquipment(@PathVariable String stateId) {
//     return equipmentRepository.findByStateId(stateId);
//   }

// }
