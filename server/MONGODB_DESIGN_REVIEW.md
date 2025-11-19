# MongoDB Design Review Document

## Overview

This document addresses MongoDB design decisions for the voting data project, including collection schemas, aggregation strategies, and API response patterns.

---

## 1. API Response Strategy: Single JSON vs Multiple Requests

### Decision: **Hybrid Approach - Single JSON per Use Case with Nested Data**

Each GUI use case will return **a single JSON response** containing all required data, structured hierarchically. This minimizes round trips and provides better performance.

### Example: GUI-2 Display State

**Endpoint**: `GET /api/states/{stateAbbr}/display`

**Response Structure** (Single JSON):
```json
{
  "state": {
    "stateAbbr": "MA",
    "stateName": "Massachusetts",
    "geometry": { /* GeoJSON */ },
    "centroidLon": -71.5376,
    "centroidLat": 42.2373,
    "zoomLevel": 7
  },
  "counties": [
    {
      "fips5": "25001",
      "countyName": "Barnstable County",
      "geometry": { /* GeoJSON */ },
      "eavs": {
        "totalRegistered": 123456,
        "missingnessScore": 0.42
      },
      "cvap": {
        "totalCVAP": 150000,
        "registrationPercentage": 82.3
      }
    }
  ],
  "provisionalBallots": {
    "stateTotal": {
      "E2A": 1000,
      "E2B": 500,
      /* ... other categories */
    }
  },
  "activeVoters": {
    "active": 100000,
    "inactive": 5000,
    "total": 105000
  }
}
```

**Rationale**:
- ✅ Single round trip reduces latency
- ✅ Frontend receives all data needed for the view
- ✅ Easier to cache and manage state
- ✅ Consistent with RESTful API design

**Exception**: For very large datasets (e.g., `wa_voter` with 5M+ records), we may use pagination with multiple requests.

---

## 2. MongoDB Collections Overview

### Total Collections: 20

#### Core Data Collections (7) ✅ Imported
1. `Eavs` - EAVS 2016-2024 unified data (Model: `EavsDoc`)
2. `PresResultsCounty` - 2024 presidential results by county (Model: `PresResultsCountyDoc`)
3. `PresResultsMaTown` - 2024 presidential results by MA town (Model: `PresResultsMaTownDoc`)
4. `CvapCounty` - CVAP demographics by county (Model: `CvapCountyDoc`)
5. `EquipmentDevice` - Voting equipment devices (Model: `EquipmentDeviceDoc`)
6. `EquipmentJurisdiction` - Equipment quality by jurisdiction (Model: `EquipmentJurisdictionDoc`)
7. `FelonyPolicy` - Felony voting policy (Model: `FelonyPolicyDoc`)

#### WA Registration Collections (7) ✅ Imported
8. `WaRegistrationAge` - WA registration by age (Model: `WaRegistrationAgeDoc`)
9. `WaRegistrationGender` - WA registration by gender (Model: `WaRegistrationGenderDoc`)
10. `WaParticipationAge` - WA 2024 participation by age (Model: `WaParticipationAgeDoc`)
11. `WaDemographicsPrecinct` - WA precinct demographics (Model: `WaDemographicsPrecinctDoc`)
12. `WaDemographicsCounty` - WA county age demographics (Model: `WaDemographicsCountyDoc`)
13. `wa_voter` - WA voter-level data (Model: `WaVoterDoc`, large file, disabled by default)
14. `wa_voter_history` - WA voter history (Model: `WaVoterHistoryDoc`, large file, disabled by default)

#### Geographic Collections (4) ✅ Imported
15. `states` - State metadata (Model: `StateDoc`)
16. `GeoStates` - State boundaries with geometry (Model: `GeoStateDoc`)
17. `GeoCounties` - County boundaries with geometry (Model: `GeoCountyDoc`)
18. `GeoWAPrecincts` - WA precinct boundaries with geometry (Model: `GeoWaPrecinctDoc`)

#### Pre-aggregated Collections (2) ✅ Imported
19. `eavs_metrics` - Pre-aggregated EAVS metrics (Model: `EavsMetricDoc`)
20. `eavs_choropleth` - Pre-aggregated choropleth data (Model: `ProvisionalChoroplethDoc`)

---

## 3. Schema for Each Collection

### 3.1 Core EAVS Data

#### `Eavs` (Collection Name)
**Model Class**: `EavsDoc`
```json
{
  "_id": "2024|MA|25001",
  "year": 2024,
  "stateAbbr": "MA",
  "jurisdictionName": "BARNSTABLE COUNTY",
  "fipscode": "250010000",
  "fips5": "25001",
  "missingnessScore": 0.42857142857142855,
  "equipmentQualityScore": 0.44,
  "registration": {
    "totalRegistered": { "A1A": 46292.0 },
    "sameDayRegistration": { "A3A": 3.0 },
    /* ... other A* fields */
  },
  "voting": {
    "totalVotes": { "B1A": 1822.0 },
    "electionDayVotes": { "B5A": 1822.0 },
    /* ... other B* fields */
  },
  "mailBallots": {
    "mailBallotsSent": { "C1A": 475.0 },
    /* ... other C* fields */
  },
  "provisional": {
    "provisionalBallotsCast": { "E1A": 1274.0 },
    "provisionalBallotCategories": { "E2A": 162.0, "E2B": 978.0 }
  },
  "equipment": {
    "equipmentInfo": { "F1A": 28388.0 }
  },
  "other": {
    "otherData": { /* other fields */ }
  },
  "createdAt": ISODate("2024-01-01T00:00:00Z"),
  "updatedAt": ISODate("2024-01-01T00:00:00Z")
}
```

**Indexes**:
- `_id` (unique)
- `{year: 1, stateAbbr: 1, fips5: 1}` (unique compound) - `year_state_fips_unique`
- `{year: 1, stateAbbr: 1}` - `year_state_idx`
- `{year: 1, fips5: 1}` - `year_fips5_idx`
- `fips5` (implicit via compound index)
- `stateAbbr` (implicit via compound index)

---

### 3.2 Election Results

#### `PresResultsCounty` (Collection Name)
**Model Class**: `PresResultsCountyDoc`
```json
{
  "_id": "IA|19001",
  "stateAbbr": "IA",
  "fips5": "19001",
  "countyName": "Adair County",
  "votesDem2024Pres": 1234,
  "votesRep2024Pres": 5678,
  "votesOther2024Pres": 90,
  "totalVotes2024Pres": 7002,
  "demShare2024Pres": 0.1762,
  "repShare2024Pres": 0.8109,
  "createdAt": ISODate("2024-01-01T00:00:00Z"),
  "updatedAt": ISODate("2024-01-01T00:00:00Z")
}
```

**Indexes**:
- `_id` (unique)
- `{stateAbbr: 1, fips5: 1}` (unique compound)
- `fips5`

---

### 3.3 CVAP Demographics

#### `CvapCounty` (Collection Name)
**Model Class**: `CvapCountyDoc`
```json
{
  "_id": "MA|25001|1",
  "stateAbbr": "MA",
  "stateFips": "25",
  "stateName": "Massachusetts",
  "fips5": "25001",
  "countyName": "Barnstable County",
  "geoid": "0500000US25001",
  "cvapCategoryCode": "1",
  "cvapCategory": "Total",
  "totalPopulationEst": 228996,
  "adultPopulationEst": 189234,
  "citizenPopulationEst": 185432,
  "cvapEstimate": 185432,
  "totalPopulationMoe": 1234,
  "adultPopulationMoe": 987,
  "citizenPopulationMoe": 876,
  "cvapMoe": 876,
  "createdAt": ISODate("2024-01-01T00:00:00Z"),
  "updatedAt": ISODate("2024-01-01T00:00:00Z")
}
```

**Indexes**:
- `_id` (unique)
- `{stateAbbr: 1, fips5: 1, cvapCategoryCode: 1}` (unique compound)
- `{stateAbbr: 1, fips5: 1}` (implicit via compound index)
- `fips5` (implicit via compound index)

---

### 3.4 Equipment Data

#### `EquipmentDevice` (Collection Name)
**Model Class**: `EquipmentDeviceDoc`
```json
{
  "_id": "MA|25001|ES&S|DS200|scanner",
  "stateAbbr": "MA",
  "fipsCode": "25001",
  "stateName": "Massachusetts",
  "jurisdiction": "BARNSTABLE COUNTY",
  "equipmentType": "scanner",
  "manufacturer": "ES&S",
  "model": "DS200",
  "firstYearInUse": 2018,
  "barcode": "Yes",
  "vppat": "N/A",
  "electionDayStandard": true,
  "electionDayAccessible": false,
  "earlyVotingStandard": true,
  "earlyVotingAccessible": false,
  "mailBallotEquipment": false,
  "extraText": "",
  "qualityScore": 0.75,
  "createdAt": ISODate("2024-01-01T00:00:00Z"),
  "updatedAt": ISODate("2024-01-01T00:00:00Z")
}
```

**Indexes**:
- `_id` (unique)
- `{stateAbbr: 1, fipsCode: 1, manufacturer: 1, model: 1, equipmentType: 1}` (unique compound)
- `fipsCode`
- `qualityScore`

---

### 3.5 WA Registration Data

#### `WaRegistrationAge` (Collection Name)
**Model Class**: `WaRegistrationAgeDoc`
```json
{
  "_id": "WA|ADAMS|18-24",
  "stateAbbr": "WA",
  "countyName": "ADAMS",
  "ageGroup": "18-24",
  "registeredVoters": 1234,
  "createdAt": ISODate("2024-01-01T00:00:00Z"),
  "updatedAt": ISODate("2024-01-01T00:00:00Z")
}
```

**Indexes**:
- `_id` (unique)
- `{stateAbbr: 1, countyName: 1, ageGroup: 1}` (unique compound)
- `countyName`

---

### 3.6 Geographic Data

#### `GeoStates` (Collection Name)
**Model Class**: `GeoStateDoc`
```json
{
  "_id": "MA",
  "stateAbbr": "MA",
  "stateFips": "25",
  "stateName": "Massachusetts",
  "centroidLon": -71.5376,
  "centroidLat": 42.2373,
  "geometry": {
    "type": "MultiPolygon",
    "coordinates": [[[[/* coordinates */]]]]
  },
  "createdAt": ISODate("2024-01-01T00:00:00Z"),
  "updatedAt": ISODate("2024-01-01T00:00:00Z")
}
```

**Indexes**:
- `_id` (unique)
- `2dsphere` index on `geometry`

---

#### `GeoCounties` (Collection Name)
**Model Class**: `GeoCountyDoc`
```json
{
  "_id": "MA-25001",
  "stateAbbr": "MA",
  "fips5": "25001",
  "countyName": "Barnstable County",
  "centroidLon": -70.2049,
  "centroidLat": 41.7008,
  "geometry": {
    "type": "MultiPolygon",
    "coordinates": [[[[/* coordinates */]]]]
  },
  "createdAt": ISODate("2024-01-01T00:00:00Z"),
  "updatedAt": ISODate("2024-01-01T00:00:00Z")
}
```

**Indexes**:
- `_id` (unique)
- `{stateAbbr: 1, fips5: 1}` (unique compound)
- `fips5`
- `2dsphere` index on `geometry`

---

## 4. Multiple Collection Aggregation Strategy

### Strategy: **Application-Level Joins with MongoDB Aggregation Pipelines**

We use a combination of:
1. **MongoDB Aggregation Pipelines** for complex multi-collection queries
2. **Application-Level Joins** in Spring Boot for simpler cases
3. **Pre-aggregated Views** for frequently accessed data

---

### 4.1 MongoDB Aggregation Pipeline Example: GUI-2 Display State

**Use Case**: Get state display data (state geometry, counties, EAVS, CVAP)

**Pipeline**:
```javascript
// Step 1: Get state geometry
db.GeoStates.findOne({ stateAbbr: "MA" })

// Step 2: Get counties with geometry
db.GeoCounties.find({ stateAbbr: "MA" })

// Step 3: Aggregate EAVS and CVAP data by county
db.Eavs.aggregate([
  { $match: { stateAbbr: "MA", year: 2024 } },
  {
    $lookup: {
      from: "CvapCounty",
      let: { fips5: "$fips5" },
      pipeline: [
        { $match: { $expr: { $and: [
          { $eq: ["$fips5", "$$fips5"] },
          { $eq: ["$cvapCategoryCode", "1"] }  // Total CVAP
        ]}}}
      ],
      as: "cvap"
    }
  },
  {
    $project: {
      fips5: 1,
      jurisdictionName: 1,
      totalRegistered: "$registration.totalRegistered.A1A",
      missingnessScore: 1,
      cvapEstimate: { $arrayElemAt: ["$cvap.cvapEstimate", 0] },
      registrationPercentage: {
        $multiply: [
          { $divide: ["$registration.totalRegistered.A1A", { $arrayElemAt: ["$cvap.cvapEstimate", 0] }] },
          100
        ]
      }
    }
  }
])
```

**Spring Boot Implementation**:
```java
@Service
public class StateDisplayService {
    
    public StateDisplayResponse getStateDisplay(String stateAbbr) {
        // 1. Get state geometry
        GeoStateDoc state = geoStateRepo.findByStateAbbr(stateAbbr);
        
        // 2. Get counties with geometry
        List<GeoCountyDoc> counties = geoCountyRepo.findByStateAbbr(stateAbbr);
        
        // 3. Get EAVS data for 2024
        List<EavsDoc> eavsData = eavsRepo.findByStateAbbrAndYear(stateAbbr, 2024);
        
        // 4. Get CVAP data
        List<CvapCountyDoc> cvapData = cvapRepo.findByStateAbbrAndFips5AndCvapCategoryCode(stateAbbr, fips5, "1");
        
        // 5. Join in application
        return buildStateDisplayResponse(state, counties, eavsData, cvapData);
    }
}
```

---

### 4.2 Aggregation Strategy by Use Case Type

#### Type A: Single Collection Queries
- **GUI-3**: Provisional ballot bar chart → `Eavs` only
- **GUI-4**: Provisional ballot table → `Eavs` only
- **GUI-6**: Equipment summary → `EquipmentDevice` only

**Strategy**: Direct MongoDB query, no aggregation needed

---

#### Type B: Two Collection Joins
- **GUI-5**: Provisional choropleth → `Eavs` + `GeoCounties` (or use pre-aggregated `eavs_choropleth`)
- **GUI-10**: Equipment type map → `EquipmentDevice` + `GeoCounties`
- **GUI-22**: Registration comparison → `Eavs` + `CvapCounty`

**Strategy**: MongoDB `$lookup` aggregation or application-level join

---

#### Type C: Three+ Collection Joins
- **GUI-2**: State display → `GeoStates` + `GeoCounties` + `Eavs` + `CvapCounty`
- **GUI-15**: State comparison → `FelonyPolicy` + `Eavs` + `PresResultsCounty`
- **GUI-24**: Drop box bubble chart → `Eavs` + `PresResultsCounty` + `GeoCounties`

**Strategy**: 
1. Primary collection query
2. MongoDB `$lookup` for related collections
3. Application-level join for final assembly

---

### 4.3 Pre-aggregated Data Strategy

For frequently accessed aggregations, we store pre-computed values:

#### Example: `eavs_choropleth` Collection
```json
{
  "_id": "2024|E1A",
  "year": 2024,
  "measure": "E1A",
  "bins": 7,
  "states": {
    "25": {
      "values": {
        "25025": 1800,
        "25027": 950
      }
    }
  }
}
```

**Use Case**: GUI-5 Provisional ballot choropleth
- Pre-aggregated by state and measure
- Reduces query time from seconds to milliseconds
- Updated during data import

---

## 5. Range of Preprocessed Data Stored in DB

### 5.1 Pre-computed Scores

1. **Missingness Score** (`Eavs.missingnessScore`)
   - Range: 0.0 - 1.0
   - Calculation: Based on missing values in key EAVS fields
   - Preprocessed: Yes, stored in DB

2. **Equipment Quality Score** (`EquipmentDevice.qualityScore`, `EquipmentJurisdiction.avgQualityScore`)
   - Range: 0.0 - 1.0
   - Factors: Age, OS, certification, scan rate, error rate
   - Preprocessed: Yes, stored in DB

---

### 5.2 Pre-aggregated Percentages

1. **Registration Percentage** (EAVS A1A / CVAP)
   - Calculation: `(registration.totalRegistered.A1A / cvapEstimate) * 100`
   - Preprocessed: No, calculated on-demand
   - Reason: CVAP data may change, calculation is fast

2. **Turnout Percentage** (EAVS B1A / CVAP)
   - Calculation: `(voting.totalVotes.B1A / cvapEstimate) * 100`
   - Preprocessed: No, calculated on-demand

3. **Vote Shares** (`PresResultsCounty.demShare2024Pres`, `repShare2024Pres`)
   - Calculation: `(votesDem / totalVotes) * 100`
   - Preprocessed: Yes, stored in DB
   - Reason: Fixed historical data, frequently accessed

---

### 5.3 Pre-aggregated Choropleth Data

**Collection**: `eavs_choropleth`
- Pre-aggregated by: `year`, `measure` (E1A, E2A, etc.)
- Structure: Nested by state FIPS → county FIPS → value
- Use Cases: GUI-5, GUI-7, GUI-8, GUI-9
- Preprocessed: Yes, stored in DB

---

### 5.4 Pre-aggregated Equipment Summaries

**Collection**: `equipment_jurisdiction_2022`
- Pre-aggregated: Average quality score per jurisdiction
- Derived from: `equipment_device_2022`
- Use Cases: GUI-25, GUI-26, GUI-28
- Preprocessed: Yes, stored in DB

---

### 5.5 Pre-aggregated WA Registration Data

**Collections**: 
- `WaDemographicsPrecinct` - Precinct-level aggregations (Model: `WaDemographicsPrecinctDoc`)
- `WaDemographicsCounty` - County-level aggregations (Model: `WaDemographicsCountyDoc`)

- Pre-aggregated: Counts by age group, gender, status
- Derived from: `wa_voter` (if available)
- Use Cases: GUI-17, GUI-18
- Preprocessed: Yes, stored in DB

---

## 6. Query Performance Optimization

### 6.1 Indexing Strategy

**Compound Indexes** for common query patterns:
- `{year: 1, stateAbbr: 1, fips5: 1}` on `Eavs` (unique)
- `{stateAbbr: 1, fips5: 1}` on `GeoCounties` (unique)
- `{stateAbbr: 1, fips5: 1, cvapCategoryCode: 1}` on `CvapCounty` (unique)
- `{stateAbbr: 1, fips5: 1}` on `PresResultsCounty` (unique)
- `{countyFips: 1, precinctNumber: 1, precinctName: 1}` on `GeoWAPrecincts` (unique)

**Geospatial Indexes**:
- `2dsphere` on `geometry` fields in `GeoStates`, `GeoCounties`, `GeoWAPrecincts`

---

### 6.2 Caching Strategy

**Application-Level Caching** (Spring Cache):
- State metadata (rarely changes)
- Felony policy data (static)
- Geographic boundaries (static)

**Response Caching**:
- Cache full JSON responses for 5-10 minutes
- Invalidate on data updates

---

## 7. Summary

### Collections: 20 total
- ✅ **20 imported** (all collections ready)
  - 7 Core data collections
  - 7 WA registration collections (2 large files disabled by default)
  - 4 Geographic collections
  - 2 Pre-aggregated collections

### Aggregation Strategy:
- **Single collection**: Direct queries
- **Two collections**: MongoDB `$lookup` or application join
- **Three+ collections**: Hybrid approach (pipeline + application join)

### Preprocessed Data:
- ✅ Missingness scores
- ✅ Equipment quality scores
- ✅ Vote shares
- ✅ Choropleth aggregations
- ✅ Equipment summaries
- ✅ WA registration aggregations
- ❌ Percentages (calculated on-demand)

### API Response Pattern:
- **Single JSON per use case** with nested data
- Exception: Large datasets use pagination

---

## 8. Current Implementation Status

### Model Naming Convention
All model classes follow the `*Doc` suffix pattern:
- `EavsDoc`, `CvapCountyDoc`, `PresResultsCountyDoc`, etc.
- Repository interfaces match: `EavsRepository`, `CvapCountyRepository`, etc.

### Collection Naming Convention
Collections use simplified PascalCase names:
- `Eavs`, `CvapCounty`, `PresResultsCounty`, `GeoStates`, etc.
- Exception: `wa_voter`, `wa_voter_history`, `states`, `eavs_metrics`, `eavs_choropleth` (kept for compatibility)

### Importer Status
All 19 importers are implemented and **disabled by default** in `application.properties`:
- Enable specific importers by setting `*.enabled=true`
- Large files (`wa_voter`, `wa_voter_history`) are disabled by default

### Data Structure
- `EavsDoc` uses nested categories: `registration`, `voting`, `mailBallots`, `provisional`, `equipment`, `other`
- All EAVS fields organized by category for easier querying
- Geographic collections store GeoJSON geometry as `org.bson.Document`

---

## 9. Design Review Checklist

- [x] List of all MongoDB collections (20)
- [x] Schema for each collection (detailed above)
- [x] Multiple collection aggregation strategy (hybrid approach)
- [x] Range of preprocessed data (scores, aggregations, percentages)
- [x] API response pattern (single JSON per use case)
- [x] Indexing strategy
- [x] Performance optimization approach
- [x] Model and collection naming conventions
- [x] Importer configuration and status



