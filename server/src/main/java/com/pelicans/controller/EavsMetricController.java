package com.pelicans.controller;

import com.pelicans.model.EavsMetricDoc;
import com.pelicans.repository.EavsMetricRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/api/eavsmetric")
public class EavsMetricController {

  private final EavsMetricRepository eavsMetricRepository;

  @Autowired
  public EavsMetricController(EavsMetricRepository eavsMetricRepository) {
    this.eavsMetricRepository = eavsMetricRepository;
  }

  @GetMapping("/provisional/{stateId}")
  public Map<String, Object> getProvisionalVoters(@PathVariable String stateId) {
    return eavsMetricRepository.findByStateId(stateId).getCategories().getProvisional();
  }

  @GetMapping("/activevoters/{stateId}")
  public Map<String, Object> getActiveVoters(@PathVariable String stateId) {
    return eavsMetricRepository.findByStateId(stateId).getCategories().getActiveVoters();
  }

  @GetMapping("/poolbookdeletions/{stateId}")
  public Map<String, Object> getPollbookDeletions(@PathVariable String stateId) {
    return eavsMetricRepository.findByStateId(stateId).getCategories().getPollbookDeletions();
  }

  @GetMapping("/mailrejections/{stateId}")
  public Map<String, Object> getMailRejections(@PathVariable String stateId) {
    return eavsMetricRepository.findByStateId(stateId).getCategories().getMailRejections();
  }

}

