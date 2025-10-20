// src/main/java/com/pelicans/repo/ProvisionalChoroplethRepository.java
package com.pelicans.repository;

import com.pelicans.model.ProvisionalChoroplethDoc;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProvisionalChoroplethRepository extends MongoRepository<ProvisionalChoroplethDoc, String> {}
