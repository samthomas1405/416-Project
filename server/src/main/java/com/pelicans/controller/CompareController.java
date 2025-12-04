package com.pelicans.controller;

import com.pelicans.model.CvapCountyDoc;
import com.pelicans.model.EavsDoc;
import com.pelicans.model.FelonyPolicyDoc;
import com.pelicans.model.GeoStateDoc;
import com.pelicans.repository.CvapCountyRepository;
import com.pelicans.repository.EavsRepository;
import com.pelicans.repository.FelonyPolicyRepository;
import com.pelicans.repository.GeoStateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("/api/compare")
public class CompareController {

    private static final int REPUBLICAN_STATE_FIPS = 19;
    private static final int DEMOCRATIC_STATE_FIPS = 25;
    private static final int DEFAULT_YEAR = 2024;
    private static final String CVAP_CATEGORY_TOTAL = "1";

    private final EavsRepository eavsRepo;
    private final FelonyPolicyRepository felonyRepo;
    private final CvapCountyRepository cvapRepo;
    private final GeoStateRepository geoStateRepo;

    @Autowired
    public CompareController(
            EavsRepository eavsRepo,
            FelonyPolicyRepository felonyRepo,
            CvapCountyRepository cvapRepo,
            GeoStateRepository geoStateRepo) {
        this.eavsRepo = eavsRepo;
        this.felonyRepo = felonyRepo;
        this.cvapRepo = cvapRepo;
        this.geoStateRepo = geoStateRepo;
    }

    /**
     * GUI-15: Compare Republican and Democratic states
     * Returns comparison data for Iowa (R) and Massachusetts (D)
     * 
     * @return Map with "republican" and "democratic" state data
     */
    @Cacheable
    @GetMapping("/rd")
    public Map<String, Object> compareRepublicanDemocratic() {
        Map<String, Object> response = new HashMap<>();
        
        Map<String, Object> republican = buildStateData(REPUBLICAN_STATE_FIPS);
        Map<String, Object> democratic = buildStateData(DEMOCRATIC_STATE_FIPS);
        
        response.put("republican", republican);
        response.put("democratic", democratic);
        
        return response;
    }

    private Map<String, Object> buildStateData(int stateFips) {
        Map<String, Object> stateData = new HashMap<>();
        
        String stateFipsStr = String.valueOf(stateFips);
        Optional<GeoStateDoc> geoState = geoStateRepo.findById(stateFipsStr);
        String stateName = geoState.map(GeoStateDoc::getStateName).orElse("Unknown");
        String stateAbbr = geoState.map(GeoStateDoc::getStateAbbr).orElse("");
        
        stateData.put("stateFips", stateFipsStr);
        stateData.put("state", stateName);
        
        String felonyRights = extractFelonyVotingRights(stateFipsStr);
        stateData.put("felony_rights", felonyRights);
        
        List<EavsDoc> eavsDocs = eavsRepo.findByYearAndStateFips(DEFAULT_YEAR, stateFips);
        List<CvapCountyDoc> cvapDocs = cvapRepo.findByStateAbbr(stateAbbr);
        
        Map<String, Object> mailBallotData = calculateMailBallotData(eavsDocs);
        Map<String, Object> turnoutData = calculateTurnoutData(eavsDocs, cvapDocs);
        
        stateData.put("pct_mail", mailBallotData.get("percentage"));
        stateData.put("mail_sent", mailBallotData.get("mailSent"));
        stateData.put("total_votes", mailBallotData.get("totalVotes"));
        
        stateData.put("turnout_pct", turnoutData.get("percentage"));
        stateData.put("votes_cast", turnoutData.get("votesCast"));
        stateData.put("total_cvap", turnoutData.get("totalCvap"));
        
        return stateData;
    }

    private String extractFelonyVotingRights(String stateFips) {
        Optional<FelonyPolicyDoc> policy = felonyRepo.findByStateFips(stateFips);
        if (policy.isEmpty() || policy.get().getQ51Fields() == null) {
            return "Not available";
        }
        
        Map<String, String> q51Fields = policy.get().getQ51Fields();
        
        if (q51Fields.containsKey("Q51Comment") && q51Fields.get("Q51Comment") != null) {
            String comment = q51Fields.get("Q51Comment");
            if (!comment.isEmpty() && !comment.equals("0")) {
                return simplifyFelonyComment(comment);
            }
        }
        
        return deriveFelonyCategory(q51Fields);
    }

    private String simplifyFelonyComment(String comment) {
        String lower = comment.toLowerCase();
        if (lower.contains("incarcerat")) {
            return "Incarc only";
        } else if (lower.contains("parole") || lower.contains("probation")) {
            return "Parole/Probation lose";
        } else if (lower.contains("automatic")) {
            return "Auto restore";
        } else if (lower.contains("additional") || lower.contains("appeal")) {
            return "Additional action";
        }
        return comment.length() > 50 ? comment.substring(0, 47) + "..." : comment;
    }

    private String deriveFelonyCategory(Map<String, String> q51Fields) {
        String q51b = q51Fields.getOrDefault("Q51b", "");
        if (q51b.equals("1")) {
            return "No denial";
        } else if (q51b.equals("2")) {
            return "Auto restore";
        } else if (q51b.equals("3")) {
            return "Parole/Probation lose";
        } else if (q51b.equals("4")) {
            return "Additional action";
        }
        return "Not available";
    }

    private Map<String, Object> calculateMailBallotData(List<EavsDoc> eavsDocs) {
        long totalMailSent = 0;
        long totalVotes = 0;
        
        for (EavsDoc doc : eavsDocs) {
            if (doc.getMailBallots() != null && doc.getMailBallots().getMailBallotsSent() != null) {
                Integer c1a = getValueCaseInsensitive(doc.getMailBallots().getMailBallotsSent(), "C1A");
                if (c1a != null && c1a > 0) {
                    totalMailSent += c1a;
                }
            }
            
            if (doc.getEquipment() != null && doc.getEquipment().getEquipmentInfo() != null) {
                Integer f1a = getValueCaseInsensitive(doc.getEquipment().getEquipmentInfo(), "F1a");
                if (f1a != null && f1a > 0) {
                    totalVotes += f1a;
                }
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("mailSent", totalMailSent);
        result.put("totalVotes", totalVotes);
        result.put("percentage", totalVotes > 0 ? (double) totalMailSent / totalVotes : 0.0);
        return result;
    }

    private Map<String, Object> calculateDropBoxData(List<EavsDoc> eavsDocs) {
        long totalDropBox = 0;
        long totalVotes = 0;
        
        for (EavsDoc doc : eavsDocs) {
            if (doc.getMailBallots() != null && doc.getMailBallots().getDropBoxReturns() != null) {
                Map<String, Integer> dropBoxMap = doc.getMailBallots().getDropBoxReturns();
                Integer dropBoxValue = null;
                
                if (dropBoxMap.containsKey("C6a")) {
                    dropBoxValue = dropBoxMap.get("C6a");
                } else if (dropBoxMap.containsKey("C6A")) {
                    dropBoxValue = dropBoxMap.get("C6A");
                } else if (dropBoxMap.containsKey("C3a")) {
                    dropBoxValue = dropBoxMap.get("C3a");
                } else if (dropBoxMap.containsKey("C3A")) {
                    dropBoxValue = dropBoxMap.get("C3A");
                } else if (dropBoxMap.containsKey("C3")) {
                    dropBoxValue = dropBoxMap.get("C3");
                } else {
                    dropBoxValue = getValueCaseInsensitive(dropBoxMap, "C6a");
                    if (dropBoxValue == null || dropBoxValue <= 0) {
                        dropBoxValue = getValueCaseInsensitive(dropBoxMap, "C3a");
                    }
                    if (dropBoxValue == null || dropBoxValue <= 0) {
                        dropBoxValue = getValueCaseInsensitive(dropBoxMap, "C3");
                    }
                }
                
                if (dropBoxValue != null && dropBoxValue > 0) {
                    totalDropBox += dropBoxValue;
                }
            }
            
            if (doc.getVoting() != null && doc.getVoting().getTotalVotes() != null) {
                Integer b1a = getValueCaseInsensitive(doc.getVoting().getTotalVotes(), "B1A");
                if (b1a != null && b1a > 0) {
                    totalVotes += b1a;
                }
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("dropBoxCount", totalDropBox);
        result.put("totalVotes", totalVotes);
        result.put("percentage", totalVotes > 0 ? (double) totalDropBox / totalVotes : 0.0);
        return result;
    }

    private Map<String, Object> calculateTurnoutData(List<EavsDoc> eavsDocs, List<CvapCountyDoc> cvapDocs) {
        long totalVotes = 0;
        long totalCvap = 0;
        
        for (EavsDoc doc : eavsDocs) {
            if (doc.getEquipment() != null && doc.getEquipment().getEquipmentInfo() != null) {
                Integer f1a = getValueCaseInsensitive(doc.getEquipment().getEquipmentInfo(), "F1a");
                if (f1a != null && f1a > 0) {
                    totalVotes += f1a;
                }
            }
        }
        
        for (CvapCountyDoc cvap : cvapDocs) {
            if (CVAP_CATEGORY_TOTAL.equals(cvap.getCvapCategoryCode())) {
                if (cvap.getCvapEstimate() != null && cvap.getCvapEstimate() > 0) {
                    totalCvap += cvap.getCvapEstimate();
                }
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("votesCast", totalVotes);
        result.put("totalCvap", totalCvap);
        result.put("percentage", totalCvap > 0 ? (double) totalVotes / totalCvap : 0.0);
        return result;
    }

    private Integer getValueCaseInsensitive(Map<String, Integer> map, String key) {
        if (map == null || key == null) {
            return null;
        }
        
        Integer value = map.get(key);
        if (value != null && value > 0) {
            return value;
        }
        
        value = map.get(key.toLowerCase());
        if (value != null && value > 0) {
            return value;
        }
        
        value = map.get(key.toUpperCase());
        if (value != null && value > 0) {
            return value;
        }
        
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(key)) {
                Integer val = entry.getValue();
                if (val != null && val > 0) {
                    return val;
                }
            }
        }
        
        return null;
    }
}

