package com.pelicans.controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.http.MediaType;
import java.nio.file.Files;
import java.io.IOException;

@RestController
@RequestMapping("/json")
public class JsonController {

  String usStates;
  String usCounties;

  public JsonController() throws IOException {
      Resource resource = new ClassPathResource("us-states.json");
      usStates = new String(Files.readAllBytes(resource.getFile().toPath()));
      resource = new ClassPathResource("us-counties.json");
      usCounties = new String(Files.readAllBytes(resource.getFile().toPath()));
  }

  @CrossOrigin
  @GetMapping("/us-states")
  public ResponseEntity<String> usStatesJson() throws IOException {
      return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(usStates);
  }

  @CrossOrigin
  @GetMapping("/us-counties")
  public ResponseEntity<String> usCountiesJson() throws IOException {
      return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(usCounties);
  }

}
