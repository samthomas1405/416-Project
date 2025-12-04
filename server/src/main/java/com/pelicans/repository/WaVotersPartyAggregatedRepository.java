package com.pelicans.repository;

import com.pelicans.model.WaVotersPartyAggregatedDoc;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WaVotersPartyAggregatedRepository extends MongoRepository<WaVotersPartyAggregatedDoc, String> {
    List<WaVotersPartyAggregatedDoc> findByStateFips(Integer stateFips);
    List<WaVotersPartyAggregatedDoc> findByCountyCode(String countyCode);
    List<WaVotersPartyAggregatedDoc> findByCountyMajorityParty(String countyMajorityParty);
    List<WaVotersPartyAggregatedDoc> findByStateFipsAndCountyCode(Integer stateFips, String countyCode);
}

