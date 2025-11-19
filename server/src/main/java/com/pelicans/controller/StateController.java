package com.pelicans.controller;

import com.pelicans.model.StateDoc;
import com.pelicans.repository.StateRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("/api/state")
public class StateController {

    private final StateRepository repo;

    public StateController(StateRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/{stateFips}")
    public ResponseEntity<?> getFullDoc(
            @PathVariable String stateFips
    ) {
        return repo.findByStateFips(stateFips)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
