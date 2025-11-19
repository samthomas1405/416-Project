# Database Schema Design

## Overview

This document describes the database schema for the voting data project. The schema supports multiple data domains:

- **EAVS (2016–2024)** – Core survey for all states & years
- **Election results (2024)** – Presidential results for MA (town) and IA/WA (county)
- **Population & CVAP (2023)** – County-level CVAP breakdowns by race/ethnicity
- **Voter registration & turnout (WA)** – Official county summaries + detailed VRDB extracts
- **Voting equipment & quality (2022)** – Verifier-like device data + derived quality scores
- **Felony voting policy (2024)** – Q51 results for IA, IL, MA, NC, WA
- **Geography** – State, county, and WA precinct boundaries

## Current Implementation: MongoDB

The project currently uses **MongoDB** with Spring Data MongoDB. The schema below is adapted from the original PostgreSQL design to work with MongoDB's document model.

## Schema Collections

### 1. Core Dimensions

#### 1.1 `states` Collection
**Model Class**: `StateDoc`  
**Purpose**: Lookup for state metadata

**Document Structure**:
```json
{
  "_id": "MA",
  "stateAbbr": "MA",
  "stateName": "Massachusetts",
  "stateFips": "25",
  "createdAt": ISODate("2024-01-01T00:00:00Z"),
  "updatedAt": ISODate("2024-01-01T00:00:00Z")
}
```

**Indexes**:
- `_id` (unique, on `stateAbbr`)
- `stateFips` (unique)

**Source**: `data_clean/geo/us_states.geojson` or derived from EAVS

---

#### 1.2 Geographic Collections
**Note**: County metadata is stored in `GeoCounties` collection (see section 8.2). There is no separate `counties` collection - all geographic data including boundaries is in the `Geo*` collections.

---

### 2. EAVS Data

#### 2.1 `Eavs` Collection
**Model Class**: `EavsDoc`  
**Purpose**: Unified EAVS 2016–2024 table, one document per jurisdiction per year

**Document Structure**:
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
    "totalRegistered": { "A1A": 46292.0, "A1B": 41817.0, "A1C": 4475.0 },
    "sameDayRegistration": { "A3A": 3.0, "A3B": 10309.0, "A3C": 70.0 },
    "registrationMethods": { "A4A": 25747.0, "A4B": 15365.0 },
    "pollbookDeletions": { "A12A": 1752.0, "A12B": 30.0, "A12C": 1722.0 }
  },
  "voting": {
    "totalVotes": { "B1A": 1822.0 },
    "electionDayVotes": { "B5A": 1822.0, "B5B": 45.0, "B5C": 1777.0 },
    "earlyVoting": { "B6A": 536.0, "B6B": 14.0, "B6C": 522.0 }
  },
  "mailBallots": {
    "mailBallotsSent": { "C1A": 475.0 },
    "mailBallotsRejected": { "C9A": 475.0, "C9B": 0.0, "C9C": 436.0 }
  },
  "provisional": {
    "provisionalBallotsCast": { "E1A": 1274.0 },
    "provisionalBallotCategories": { "E1B": 162.0, "E1C": 978.0, "E1D": 43.0 }
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
- `_id` (unique, on `year|stateAbbr|fips5`)
- Compound: `{year: 1, stateAbbr: 1, fips5: 1}` (unique) - `year_state_fips_unique`
- Compound: `{year: 1, stateAbbr: 1}` - `year_state_idx`
- Compound: `{year: 1, fips5: 1}` - `year_fips5_idx`
- `fips5` (implicit via compound index)
- `stateAbbr` (implicit via compound index)

**Source**: `data_clean/eavs/eavs_2016_2024_normalized.csv`

**Note**: EAVS fields are organized into nested categories (`registration`, `voting`, `mailBallots`, `provisional`, `equipment`, `other`) for easier querying and better organization.

---

### 3. 2024 Presidential Results

#### 3.1 `PresResultsCounty` Collection
**Model Class**: `PresResultsCountyDoc`  
**Purpose**: County-level 2024 presidential results for IA and WA

**Document Structure**:
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
- `_id` (unique, on `stateAbbr|fips5`)
- Compound: `{stateAbbr: 1, fips5: 1}` (unique)
- `fips5`

**Source**: `data_clean/results_2024/pres_2024_by_county.csv`

---

#### 3.2 `PresResultsMaTown` Collection
**Model Class**: `PresResultsMaTownDoc`  
**Purpose**: Town-level results for MA

**Document Structure**:
```json
{
  "_id": "MA|ABINGTON",
  "stateAbbr": "MA",
  "townName": "ABINGTON",
  "votesDem2024Pres": 3456,
  "votesRep2024Pres": 2345,
  "votesOther2024Pres": 123,
  "totalVotes2024Pres": 5924,
  "demShare2024Pres": 0.5837,
  "repShare2024Pres": 0.3959,
  "createdAt": ISODate("2024-01-01T00:00:00Z"),
  "updatedAt": ISODate("2024-01-01T00:00:00Z")
}
```

**Indexes**:
- `_id` (unique, on `stateAbbr|townName`)
- Compound: `{stateAbbr: 1, townName: 1}` (unique)

**Source**: `data_clean/results_2024/pres_2024_ma_by_town.csv`

---

### 4. CVAP Data (2019–2023)

#### 4.1 `CvapCounty` Collection
**Model Class**: `CvapCountyDoc`  
**Purpose**: 2023 county-level CVAP + population by racial/ethnic categories

**Document Structure**:
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
- `_id` (unique, on `stateAbbr|fips5|cvapCategoryCode`)
- Compound: `{stateAbbr: 1, fips5: 1, cvapCategoryCode: 1}` (unique)
- Compound: `{stateAbbr: 1, fips5: 1}`
- `fips5`

**Source**: `data_clean/cvap/cvap_2019_2023_county_long.csv`

---

### 5. WA Registration & VRDB

#### 5.1 `WaRegistrationAge` Collection
**Model Class**: `WaRegistrationAgeDoc`  
**Purpose**: Official WA county-level registration counts by age group

**Document Structure**:
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
- `_id` (unique, on `stateAbbr|countyName|ageGroup`)
- Compound: `{stateAbbr: 1, countyName: 1, ageGroup: 1}` (unique)
- `countyName`

**Source**: `data_clean/registration/wa_registration_by_county_age.csv`

---

#### 5.2 `WaRegistrationGender` Collection
**Model Class**: `WaRegistrationGenderDoc`  
**Purpose**: Official WA county-level registration counts by gender

**Document Structure**:
```json
{
  "_id": "WA|ADAMS|M",
  "stateAbbr": "WA",
  "countyName": "ADAMS",
  "gender": "M",
  "registeredVoters": 5678,
  "createdAt": ISODate("2024-01-01T00:00:00Z"),
  "updatedAt": ISODate("2024-01-01T00:00:00Z")
}
```

**Indexes**:
- `_id` (unique, on `stateAbbr|countyName|gender`)
- Compound: `{stateAbbr: 1, countyName: 1, gender: 1}` (unique)

**Source**: `data_clean/registration/wa_registration_by_county_gender.csv`

---

#### 5.3 `WaParticipationAge` Collection
**Model Class**: `WaParticipationAgeDoc`  
**Purpose**: WA 2024 General turnout and registration shares by age group and county

**Document Structure**:
```json
{
  "_id": "WA|ADAMS|18-24|2024|General",
  "stateAbbr": "WA",
  "countyName": "ADAMS",
  "ageGroup": "18-24",
  "year": 2024,
  "electionType": "General",
  "totalPopulation": 12345,
  "totalVoters": 9876,
  "registeredPopulationShare": 0.80,
  "voterTurnoutShare": 0.65,
  "createdAt": ISODate("2024-01-01T00:00:00Z"),
  "updatedAt": ISODate("2024-01-01T00:00:00Z")
}
```

**Indexes**:
- `_id` (unique, on `stateAbbr|countyName|ageGroup|year|electionType`)
- Compound: `{stateAbbr: 1, countyName: 1, ageGroup: 1, year: 1, electionType: 1}` (unique)
- `countyName`

**Source**: `data_clean/registration/wa_participation_2024_by_county_age.csv`

---

#### 5.4 `wa_voter` Collection
**Model Class**: `WaVoterDoc`  
**Purpose**: De-identified WA voter-level VRDB

**Document Structure**:
```json
{
  "_id": "WA-123456789",
  "stateVoterId": "WA-123456789",
  "birthyear": 1985,
  "age2024": 39,
  "ageGroup2024": "35-44",
  "gender": "M",
  "countyCode": "AD",
  "countyName": "ADAMS",
  "precinctCode": "001",
  "precinctPart": "A",
  "legislativeDistrict": "09",
  "congressionalDistrict": "04",
  "registrationDate": "2020-01-15",
  "lastVoted": "2024-11-05",
  "statusCode": "Active",
  "createdAt": ISODate("2024-01-01T00:00:00Z"),
  "updatedAt": ISODate("2024-01-01T00:00:00Z")
}
```

**Indexes**:
- `_id` (unique, on `stateVoterId`)
- `stateVoterId` (unique)
- `countyCode`
- `precinctCode`
- Compound: `{countyCode: 1, precinctCode: 1, precinctPart: 1}`

**Source**: `data_clean/registration/wa_vrdb_voters.csv`

**⚠️ Note**: This collection is huge (~5M rows). Consider:
- Loading only if performance is acceptable
- Or keeping as CSV and loading only aggregated summaries

---

#### 5.5 `WaDemographicsPrecinct` Collection
**Model Class**: `WaDemographicsPrecinctDoc`  
**Purpose**: Precinct-level counts by age group, gender, status

**Document Structure**:
```json
{
  "_id": "WA|AD|001|A|18-24|M|Active",
  "stateAbbr": "WA",
  "countyCode": "AD",
  "countyName": "ADAMS",
  "precinctCode": "001",
  "precinctPart": "A",
  "legislativeDistrict": "09",
  "congressionalDistrict": "04",
  "ageGroup2024": "18-24",
  "gender": "M",
  "statusCode": "Active",
  "registeredVoters": 123,
  "createdAt": ISODate("2024-01-01T00:00:00Z"),
  "updatedAt": ISODate("2024-01-01T00:00:00Z")
}
```

**Indexes**:
- `_id` (unique, on composite key)
- Compound: `{countyCode: 1, precinctCode: 1, precinctPart: 1, ageGroup2024: 1, gender: 1, statusCode: 1}` (unique)
- Compound: `{countyCode: 1, precinctCode: 1, precinctPart: 1}`

**Source**: `data_clean/registration/wa_vrdb_precinct_age_gender_summary_2024.csv`

---

#### 5.6 `WaDemographicsCounty` Collection
**Model Class**: `WaDemographicsCountyDoc`  
**Purpose**: VRDB-based county registration counts by age group

**Document Structure**:
```json
{
  "_id": "WA|AD|18-24",
  "stateAbbr": "WA",
  "countyCode": "AD",
  "countyName": "ADAMS",
  "ageGroup2024": "18-24",
  "registeredVoters": 1234,
  "createdAt": ISODate("2024-01-01T00:00:00Z"),
  "updatedAt": ISODate("2024-01-01T00:00:00Z")
}
```

**Indexes**:
- `_id` (unique, on `stateAbbr|countyCode|ageGroup2024`)
- Compound: `{countyCode: 1, ageGroup2024: 1}` (unique)
- `countyCode`

**Source**: `data_clean/registration/wa_vrdb_county_age_summary_2024.csv`

---

#### 5.7 `wa_voter_history` Collection
**Model Class**: `WaVoterHistoryDoc`  
**Purpose**: History of voters participating in elections (2023–2026)

**Document Structure**:
```json
{
  "_id": "WA-123456789|2024-11-05",
  "stateAbbr": "WA",
  "voterHistoryId": "WA-123456789|2024-11-05",
  "stateVoterId": "WA-123456789",
  "countyCode": "AD",
  "countyCodeVoting": "AD",
  "electionDate": ISODate("2024-11-05T00:00:00Z"),
  "electionDateStr": "2024-11-05",
  "electionYear": 2024,
  "createdAt": ISODate("2024-01-01T00:00:00Z"),
  "updatedAt": ISODate("2024-01-01T00:00:00Z")
}
```

**Indexes**:
- `_id` (unique, on `voterHistoryId`)
- `stateVoterId`
- `electionYear`
- Compound: `{stateVoterId: 1, electionYear: 1}`

**Source**: `data_clean/registration/wa_vrdb_voter_history.csv`

---

### 6. Equipment & Quality (2022)

#### 6.1 `EquipmentDevice` Collection
**Model Class**: `EquipmentDeviceDoc`  
**Purpose**: Each document is a specific equipment record with quality score

**Document Structure**:
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
- `_id` (unique, on composite key)
- Compound: `{stateAbbr: 1, fipsCode: 1, manufacturer: 1, model: 1, equipmentType: 1}` (unique)
- `fipsCode`
- `qualityScore`

**Source**: `data_clean/equipment/equipment_2022_with_quality.csv`

---

#### 6.2 `EquipmentJurisdiction` Collection
**Model Class**: `EquipmentJurisdictionDoc`  
**Purpose**: One document per jurisdiction with average equipment quality

**Document Structure**:
```json
{
  "_id": "MA|25001",
  "stateAbbr": "MA",
  "fipsCode": "25001",
  "stateName": "Massachusetts",
  "jurisdiction": "BARNSTABLE COUNTY",
  "avgQualityScore": 0.75,
  "createdAt": ISODate("2024-01-01T00:00:00Z"),
  "updatedAt": ISODate("2024-01-01T00:00:00Z")
}
```

**Indexes**:
- `_id` (unique, on `stateAbbr|fipsCode`)
- Compound: `{stateAbbr: 1, fipsCode: 1}` (unique)
- `fipsCode`
- `avgQualityScore`

**Source**: `data_clean/equipment/equipment_quality_by_jurisdiction_2022.csv`

---

### 7. Felony Voting Policy (Q51)

#### 7.1 `FelonyPolicy` Collection
**Model Class**: `FelonyPolicyDoc`  
**Purpose**: Store Q51 responses for IA, IL, MA, NC, WA

**Document Structure**:
```json
{
  "_id": "MA",
  "stateAbbr": "MA",
  "stateFull": "Massachusetts",
  "q51Fields": {
    "Q51_1": "Yes",
    "Q51_2": "No",
    "Q51a_1": "Yes",
    "Q51b": "No",
    "Q51c_1": "Yes"
  },
  "createdAt": ISODate("2024-01-01T00:00:00Z"),
  "updatedAt": ISODate("2024-01-01T00:00:00Z")
}
```

**Indexes**:
- `_id` (unique, on `stateAbbr`)
- `stateAbbr` (unique)

**Source**: `data_clean/eavs/felony_policy_2024_q51.csv`

---

### 8. Geography Collections

Geographic boundaries stored as GeoJSON format:

#### 8.1 `GeoStates` Collection
**Model Class**: `GeoStateDoc`  
**Purpose**: State boundaries with geometry
**Document Structure**:
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
    "coordinates": [[[...]]]
  },
  "createdAt": ISODate("2024-01-01T00:00:00Z"),
  "updatedAt": ISODate("2024-01-01T00:00:00Z")
}
```

**Indexes**:
- `_id` (unique, on `stateAbbr`)
- `2dsphere` index on `geometry`

**Source**: `data_clean/geo/us_states.geojson`

---

#### 8.2 `GeoCounties` Collection
**Model Class**: `GeoCountyDoc`  
**Purpose**: County boundaries with geometry
**Document Structure**:
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
    "coordinates": [[[...]]]
  },
  "createdAt": ISODate("2024-01-01T00:00:00Z"),
  "updatedAt": ISODate("2024-01-01T00:00:00Z")
}
```

**Indexes**:
- `_id` (unique, on `stateAbbr|fips5`)
- Compound: `{stateAbbr: 1, fips5: 1}` (unique)
- `2dsphere` index on `geometry`

**Source**: `data_clean/geo/us_counties_selected.geojson`

---

#### 8.3 `GeoWAPrecincts` Collection
**Model Class**: `GeoWaPrecinctDoc`  
**Purpose**: WA precinct boundaries with geometry
**Document Structure**:
```json
{
  "_id": "WA|53001|001|PRECINCT 001",
  "stateAbbr": "WA",
  "countyFips": "53001",
  "countyName": "ADAMS",
  "precinctNumber": "001",
  "precinctName": "PRECINCT 001",
  "centroidLon": -118.1234,
  "centroidLat": 46.5678,
  "geometry": {
    "type": "MultiPolygon",
    "coordinates": [[[...]]]
  },
  "createdAt": ISODate("2024-01-01T00:00:00Z"),
  "updatedAt": ISODate("2024-01-01T00:00:00Z")
}
```

**Indexes**:
- `_id` (unique, on composite key)
- Compound: `{countyFips: 1, precinctNumber: 1, precinctName: 1}` (unique)
- `2dsphere` index on `geometry`

**Source**: `data_clean/geo/wa_precincts_2024.geojson`

---

### 9. Pre-aggregated Collections

#### 9.1 `eavs_metrics` Collection
**Model Class**: `EavsMetricDoc`  
**Purpose**: Pre-aggregated EAVS metrics by state and region

**Document Structure**:
```json
{
  "_id": "2024|25|25-25001",
  "year": 2024,
  "stateId": "25",
  "regionId": "25-25001",
  "categories": {
    "provisional": { "E1A": 1274.0, "E2A": 162.0 },
    "activeVoters": { "A1A": 46292.0, "A1B": 41817.0 },
    "pollbookDeletions": { "A12A": 1752.0 },
    "mailRejections": { "C9A": 475.0 },
    "earlyVoting": { "total": 536.0 },
    "provisionalRejected": { "total": 43.0 },
    "uocavaRejected": { "total": 0.0 }
  },
  "derived": {
    "missingnessScore": 0.42,
    "turnoutPct": 65.5
  },
  "provenance": {
    "source": "EAVS 2024 CSV",
    "version": "1.0",
    "ingestedAt": ISODate("2024-01-01T00:00:00Z")
  },
  "createdAt": ISODate("2024-01-01T00:00:00Z"),
  "updatedAt": ISODate("2024-01-01T00:00:00Z")
}
```

**Indexes**:
- `_id` (unique)
- Compound: `{stateId: 1, regionId: 1, year: 1}` (unique)

**Source**: Derived from `Eavs` collection

---

#### 9.2 `eavs_choropleth` Collection
**Model Class**: `ProvisionalChoroplethDoc`  
**Purpose**: Pre-aggregated choropleth data for visualization

**Document Structure**:
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
  },
  "createdAt": ISODate("2024-01-01T00:00:00Z"),
  "updatedAt": ISODate("2024-01-01T00:00:00Z")
}
```

**Indexes**:
- `_id` (unique, on `year|measure`)

**Source**: Derived from `Eavs` collection

---

## Load Order & Indexing

### Recommended Load Order

1. **Dimension tables**:
   - `states`
   - `GeoStates`
   - `GeoCounties`
   - `GeoWAPrecincts`

2. **Core facts**:
   - `Eavs`
   - `PresResultsCounty`
   - `PresResultsMaTown`
   - `CvapCounty`

3. **Equipment**:
   - `EquipmentDevice`
   - `EquipmentJurisdiction`

4. **Registration & VRDB**:
   - `WaRegistrationAge`
   - `WaRegistrationGender`
   - `WaParticipationAge`
   - `WaDemographicsPrecinct`
   - `WaDemographicsCounty`
   - `wa_voter_history`
   - `wa_voter` (last, if loading at all - large file, disabled by default)

5. **Policy**:
   - `FelonyPolicy`

6. **Pre-aggregated**:
   - `eavs_metrics`
   - `eavs_choropleth`

### Indexing Strategy

All collections should have:
- Unique `_id` index (automatically created)
- Indexes on foreign key fields (e.g., `fips5`, `stateAbbr`)
- Compound indexes for common query patterns
- `2dsphere` indexes on geometry fields (if storing GeoJSON)

---

## Important Constraints

1. **No party registration in WA** – Do not create or expect party fields
2. **No voter names** – All data in `data_clean` is de-identified
3. **FIPS code handling** – Connecticut uses "90" instead of "09" in EAVS data (non-standard)
4. **MA uses towns, not counties** – EAVS regions in MA are town/city level

---

## Migration Notes

### From PostgreSQL Schema to MongoDB

Key adaptations:
- **Primary keys** → `_id` field (composite keys become concatenated strings)
- **Foreign keys** → Reference fields (no enforced referential integrity)
- **Normalized tables** → Denormalized documents (where appropriate)
- **JOINs** → Application-level joins or embedded documents
- **Transactions** → MongoDB transactions (if needed) or eventual consistency

### Data Type Mappings

- `CHAR(2)` → `String`
- `TEXT` → `String`
- `INT` → `Integer`
- `BIGINT` → `Long` or `String` (for FIPS codes)
- `DOUBLE PRECISION` → `Double`
- `DATE` → `Date` or `String` (ISO format)
- `geometry(MULTIPOLYGON, 4326)` → GeoJSON object

---

## Use Cases Mapping

| Use Case | Collections |
|----------|------------|
| EAVS maps, provisional/pollbook/mail | `Eavs`, `GeoCounties` |
| Missingness (Prepro-5) | `Eavs` (missingnessScore) |
| US / state splash maps (GUI-1/2) | `GeoStates`, `GeoCounties` |
| 2024 Presidential splits & comparisons | `PresResultsCounty`, `PresResultsMaTown`, `Eavs`, `CvapCounty` |
| Felony policy | `FelonyPolicy` |
| Equipment type & quality | `EquipmentDevice`, `EquipmentJurisdiction`, `Eavs` |
| WA registration & turnout | `WaRegistrationAge`, `WaRegistrationGender`, `WaParticipationAge`, `WaDemographicsPrecinct`, `WaDemographicsCounty` |
| WA micro-level analysis | `wa_voter`, `wa_voter_history` |
| Pre-aggregated choropleth | `eavs_choropleth` |
| Pre-aggregated metrics | `eavs_metrics` |

---

## Collection Summary

**Total Collections: 20**

### Core Data (7)
1. `Eavs` - EAVS 2016-2024 unified data
2. `PresResultsCounty` - 2024 presidential results by county
3. `PresResultsMaTown` - 2024 presidential results by MA town
4. `CvapCounty` - CVAP demographics by county
5. `EquipmentDevice` - Voting equipment devices
6. `EquipmentJurisdiction` - Equipment quality by jurisdiction
7. `FelonyPolicy` - Felony voting policy

### WA Registration (7)
8. `WaRegistrationAge` - WA registration by age
9. `WaRegistrationGender` - WA registration by gender
10. `WaParticipationAge` - WA 2024 participation by age
11. `WaDemographicsPrecinct` - WA precinct demographics
12. `WaDemographicsCounty` - WA county age demographics
13. `wa_voter` - WA voter-level data (large file, disabled by default)
14. `wa_voter_history` - WA voter history (large file, disabled by default)

### Geographic (4)
15. `states` - State metadata
16. `GeoStates` - State boundaries with geometry
17. `GeoCounties` - County boundaries with geometry
18. `GeoWAPrecincts` - WA precinct boundaries with geometry

### Pre-aggregated (2)
19. `eavs_metrics` - Pre-aggregated EAVS metrics
20. `eavs_choropleth` - Pre-aggregated choropleth data

---

## Model Naming Convention

All model classes follow the `*Doc` suffix pattern:
- `EavsDoc`, `CvapCountyDoc`, `PresResultsCountyDoc`, `GeoStateDoc`, etc.
- Repository interfaces match: `EavsRepository`, `CvapCountyRepository`, etc.

## Collection Naming Convention

Collections use simplified PascalCase names:
- `Eavs`, `CvapCounty`, `PresResultsCounty`, `GeoStates`, etc.
- Exception: `wa_voter`, `wa_voter_history`, `states`, `eavs_metrics`, `eavs_choropleth` (kept for compatibility)



