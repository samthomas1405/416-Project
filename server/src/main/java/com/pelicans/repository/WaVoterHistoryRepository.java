package com.pelicans.repository;

import com.pelicans.model.WaVoterHistoryDoc;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WaVoterHistoryRepository extends MongoRepository<WaVoterHistoryDoc, String> {
    List<WaVoterHistoryDoc> findByStateVoterId(String stateVoterId);
    List<WaVoterHistoryDoc> findByElectionYear(Integer electionYear);
    List<WaVoterHistoryDoc> findByStateVoterIdAndElectionYear(String stateVoterId, Integer electionYear);
}



