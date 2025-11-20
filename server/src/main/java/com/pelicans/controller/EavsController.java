package com.pelicans.controller;

import com.pelicans.model.EavsDoc;
import com.pelicans.repository.EavsRepository;
import org.springframework.cache.annotation.Cacheable;
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

    private List<EavsDoc.Provisional> getProvisionalsFromDocs(List<EavsDoc> docs) {
        return docs.stream()
            .map(EavsDoc::getProvisional)
            .collect(Collectors.toList());
    }

    @Cacheable
    @GetMapping("/provisional/{stateFips}/regions")
    public List<EavsDoc.Provisional>  getProvisionalByState(@PathVariable String stateFips) {
        List<EavsDoc> docs = repo.findByStateFips(stateFips);
        List<EavsDoc.Provisional> provisionals = getProvisionalsFromDocs(docs);
        return provisionals;
    }

}
