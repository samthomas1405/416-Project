package com.pelicans.controller;

import com.pelicans.model.EavsDoc;
import com.pelicans.repository.EavsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("/api/eavs")
public class EavsController {

    private final EavsRepository repo;

    @Autowired
    public EavsController(EavsRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/provisional/{stateId}/regions")
    public List<EavsDoc.Provisional>  getProvisionalByRegion(@PathVariable String stateId) {
        List<EavsDoc> regions = repo.findByStateFips(stateId);
        List<EavsDoc.Provisional> provisionals = regions.stream()
            .map(EavsDoc::getProvisional)
            .collect(Collectors.toList());
        return provisionals;
    }

    // @GetMapping("/provisional/{stateId}")
    // public Map<String, Object> getProvisionalVoters(@PathVariable String stateId) {
    //   return eavsRepository.findByStateId(stateId).getCategories().getProvisional();
    // }

    // @GetMapping("/activevoters/{stateId}")
    // public Map<String, Object> getActiveVoters(@PathVariable String stateId) {
    //   return eavsRepository.findByStateId(stateId).getCategories().getActiveVoters();
    // }

    // @GetMapping("/poolbookdeletions/{stateId}")
    // public Map<String, Object> getPollbookDeletions(@PathVariable String stateId) {
    //   return eavsRepository.findByStateId(stateId).getCategories().getPollbookDeletions();
    // }

    // @GetMapping("/mailrejections/{stateId}")
    // public Map<String, Object> getMailRejections(@PathVariable String stateId) {
    //   return eavsRepository.findByStateId(stateId).getCategories().getMailRejections();
    // }

}
