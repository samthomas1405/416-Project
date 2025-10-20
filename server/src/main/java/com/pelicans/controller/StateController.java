package com.pelicans.controller;

import com.pelicans.model.State;
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
        State state = repo.findByStateFips(stateFips);
        if (state == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(state);
    }
}
