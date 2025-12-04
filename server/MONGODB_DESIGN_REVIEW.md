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

### Total Collections: 21

#### Core Data Collections (7) ✅ Imported
1. `Eavs` - EAVS 2016-2024 unified data (Model: `EavsDoc`)
2. `PresResultsCounty` - 2024 presidential results by county (Model: `PresResultsCountyDoc`)
3. `PresResultsMaTown` - 2024 presidential results by MA town (Model: `PresResultsMaTownDoc`)
4. `CvapCounty` - CVAP demographics by county (Model: `CvapCountyDoc`)
5. `EquipmentDevice` - Voting equipment devices (Model: `EquipmentDeviceDoc`)
6. `EquipmentJurisdiction` - Equipment quality by jurisdiction (Model: `EquipmentJurisdictionDoc`)
7. `FelonyPolicy` - Felony voting policy (Model: `FelonyPolicyDoc`)

#### WA Registration Collections (8) ✅ Imported
8. `WaRegistrationAge` - WA registration by age (Model: `WaRegistrationAgeDoc`)
9. `WaRegistrationGender` - WA registration by gender (Model: `WaRegistrationGenderDoc`)
10. `WaParticipationAge` - WA 2024 participation by age (Model: `WaParticipationAgeDoc`)
11. `WaDemographicsPrecinct` - WA precinct demographics (Model: `WaDemographicsPrecinctDoc`)
12. `WaDemographicsCounty` - WA county age demographics (Model: `WaDemographicsCountyDoc`)
13. `WaVotersPartyAggregated` - WA voters aggregated by party proxy, age, gender, status (Model: `WaVotersPartyAggregatedDoc`)
14. `wa_voter` - WA voter-level data (Model: `WaVoterDoc`, large file, disabled by default)
15. `wa_voter_history` - WA voter history (Model: `WaVoterHistoryDoc`, large file, disabled by default)

#### Geographic Collections (4) ✅ Imported
16. `states` - State metadata (Model: `StateDoc`)
17. `GeoStates` - State boundaries with geometry (Model: `GeoStateDoc`)
18. `GeoCounties` - County boundaries with geometry (Model: `GeoCountyDoc`)
19. `GeoWAPrecincts` - WA precinct boundaries with geometry (Model: `GeoWaPrecinctDoc`)

#### Pre-aggregated Collections (2) ✅ Imported
20. `eavs_metrics` - Pre-aggregated EAVS metrics (Model: `EavsMetricDoc`)
21. `eavs_choropleth` - Pre-aggregated choropleth data (Model: `ProvisionalChoroplethDoc`)

---

## 3. Schema for Each Collection

### 3.1 Core EAVS Data

#### `Eavs` (Collection Name)
**Model Class**: `EavsDoc`
```json
{
  "_id": "2024|25|25001",
  "year": 2024,
  "stateFips": 25,
  "jurisdictionName": "BARNSTABLE COUNTY",
  "fipscode": "250010000",
  "fips5": "25001",
  "missingnessScore": 0.42857142857142855,
  "equipmentQualityScore": 0.44,
  "registration": {
    "totalRegistered": { "A1A": 46292, "A1B": 41817, "A1C": 4475 },
    "sameDayRegistration": { "A3A": 3, "A3B": 10309 },
    "registrationMethods": { "A4A": 25747, "A4B": 15365 },
    "pollbookDeletions": { "A12A": 1752, "A12B": 30 }
  },
  "voting": {
    "totalVotes": { "B1A": 1822, "B1B": 138, "B1C": 39 },
    "electionDayVotes": { "B2A": 150, "B2B": 3 },
    "earlyVotingTotals": { "B5A": 536, "B5B": 14, "B5C": 522 }
  },
  "mailBallots": {
    "mailBallotsSent": { "C1A": 475, "C1B": 819 },
    "mailBallotsRejected": { "C9A": 475, "C9B": 0, "C9C": 436 }
  },
  "provisional": {
    "jurisdictionName": "BARNSTABLE COUNTY",
    "provisionalBallotsCast": { "E1A": 1274, "E1D": 55 },
    "provisionalBallotCategories": { "E2A": 162, "E2B": 978, "E2C": 43 }
  },
  "equipment": {
    "equipmentInfo": { "F1A": 28388, "F1C": 0, "F1E": 66 },
    "equipmentTypes": { "F4A": 0, "F4B": 0, "F4C": 0 }
  },
  "other": {
    "otherData": { "D1A": 30, "D2A": 30, "D3A": 201 }
  },
  "createdAt": ISODate("2024-01-01T00:00:00Z"),
  "updatedAt": ISODate("2024-01-01T00:00:00Z")
}
```

**Key Design Decisions**:
- **`_id` format**: `"year|stateFips|fips5"` (e.g., `"2024|25|25001"`) - uses `stateFips` (Integer) for standardization
- **Data types**: All nested maps use `Map<String, Integer>` (not `Object`) for type safety and query efficiency
- **Sentinel values**: EAVS sentinel values (`-999999`, `-888888`, negative numbers) are stored as `-1` instead of `null`
- **Nested structure**: Fields organized into categories (`registration`, `voting`, `mailBallots`, `provisional`, `equipment`, `other`) for better organization
- **`jurisdictionName`**: Stored both at top level and in `provisional.jurisdictionName` for convenience
- **No `questions` map**: Removed flat map structure in favor of organized nested categories

**Data Type Rationale**:
- **`_id` (String)**: Composite key as string for MongoDB `_id` requirement. Format `"year|stateFips|fips5"` enables efficient lookups and maintains human readability.
- **`year` (Integer)**: Numeric type for range queries (e.g., `year >= 2020`), sorting, and aggregation operations. More efficient than string comparisons.
- **`stateFips` (Integer)**: Federal standard 2-digit FIPS code. Integer enables numeric comparisons and joins with other federal datasets. Stored as Integer (not String) for consistency with federal data standards.
- **`jurisdictionName` (String)**: Text field for human-readable jurisdiction names. Preserved as-is from source data (uppercase, various formats).
- **`fipscode` (String)**: Original 9-digit EAVS code preserved as string to maintain leading zeros (e.g., `"250010000"`). Leading zeros would be lost if stored as Integer.
- **`fips5` (String)**: 5-digit county FIPS code as string to preserve leading zeros (e.g., `"01001"` for Alabama counties). Required for proper matching with federal datasets.
- **`missingnessScore` (Double)**: Calculated ratio (0.0-1.0) requiring decimal precision. Double allows for precise calculations and comparisons.
- **`equipmentQualityScore` (Double)**: Calculated quality metric (0.0-1.0) requiring decimal precision for accurate scoring.
- **Nested maps `Map<String, Integer>`**: 
  - **Key (String)**: EAVS field codes (e.g., `"A1A"`, `"B1B"`) are categorical identifiers, naturally strings.
  - **Value (Integer)**: All EAVS counts are whole numbers (voter counts, ballot counts). Integer type:
    - Enables efficient numeric aggregation (`$sum`, `$avg`)
    - Prevents type coercion issues in queries
    - Uses less storage than Double
    - Supports range queries (`$gt`, `$lt`) efficiently
  - **Why not `Object`**: Type safety prevents runtime errors, enables compile-time validation, and improves query performance (MongoDB can optimize Integer operations).
- **Sentinel value `-1`**: EAVS uses `-999999` (Data Not Available) and `-888888` (Not Applicable). Stored as `-1` because:
  - Single sentinel value simplifies query logic
  - Negative number clearly indicates missing/invalid data
  - Enables filtering: `{ $gt: 0 }` to exclude missing values
  - More efficient than `null` checks in aggregation pipelines

**Indexes**:
- `_id` (unique)
- `{year: 1, stateFips: 1, fips5: 1}` (unique compound) - `year_state_fips_unique`
- `{year: 1, stateFips: 1}` - `year_state_idx`
- `{year: 1, fips5: 1}` - `year_fips5_idx`
- `fips5` (implicit via compound index)
- `stateFips` (implicit via compound index)

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

**Data Type Rationale**:
- **`_id` (String)**: Composite key `"stateAbbr|fips5"` for unique identification. Uses `stateAbbr` for human readability in queries.
- **`stateAbbr` (String)**: 2-letter state abbreviation. String type preserves standard format and enables exact matching.
- **`fips5` (String)**: 5-digit county FIPS code as string to preserve leading zeros (e.g., `"01001"`).
- **`countyName` (String)**: Human-readable county name. Text field for display purposes.
- **`votesDem2024Pres` (Integer)**: Vote counts are whole numbers. Integer enables efficient aggregation and comparison operations.
- **`votesRep2024Pres` (Integer)**: Same rationale as Democratic votes.
- **`votesOther2024Pres` (Integer)**: Same rationale as other vote counts.
- **`totalVotes2024Pres` (Integer)**: Sum of all votes. Integer for consistency and efficient calculations.
- **`demShare2024Pres` (Double)**: Percentage/proportion (0.0-1.0) requiring decimal precision. Pre-calculated and stored to avoid repeated division operations.
- **`repShare2024Pres` (Double)**: Same rationale as Democratic share. Pre-calculated for performance.

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

**Data Type Rationale**:
- **`_id` (String)**: Composite key `"stateAbbr|fips5|cvapCategoryCode"` uniquely identifies each demographic category per county.
- **`stateAbbr` (String)**: 2-letter state abbreviation for human-readable queries.
- **`stateFips` (String)**: Federal 2-digit FIPS code stored as string to preserve leading zeros (e.g., `"01"` for Alabama). String format matches source data.
- **`stateName` (String)**: Full state name for display purposes.
- **`fips5` (String)**: 5-digit county FIPS code as string to preserve leading zeros.
- **`countyName` (String)**: Human-readable county name.
- **`geoid` (String)**: Census GEOID format (e.g., `"0500000US25001"`) is a structured identifier requiring string preservation.
- **`cvapCategoryCode` (String)**: Categorical code (e.g., `"1"` for Total, `"2"` for Hispanic) stored as string to match source data format.
- **`cvapCategory` (String)**: Human-readable category name (e.g., `"Total"`, `"Hispanic"`).
- **`totalPopulationEst` (Integer)**: Population estimates are whole numbers. Integer enables efficient aggregation and comparison.
- **`adultPopulationEst` (Integer)**: Same rationale as total population.
- **`citizenPopulationEst` (Integer)**: Same rationale as other population counts.
- **`cvapEstimate` (Integer)**: Citizen Voting Age Population estimate. Integer for consistency with other population fields.
- **`totalPopulationMoe` (Integer)**: Margin of Error (MOE) values are whole numbers from ACS data. Integer type for consistency.
- **`adultPopulationMoe` (Integer)**: Same rationale as other MOE fields.
- **`citizenPopulationMoe` (Integer)**: Same rationale as other MOE fields.
- **`cvapMoe` (Integer)**: Same rationale as other MOE fields.

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

**Data Type Rationale**:
- **`_id` (String)**: Composite key `"stateAbbr|fipsCode|manufacturer|model|equipmentType"` uniquely identifies each equipment device.
- **`stateAbbr` (String)**: 2-letter state abbreviation for human-readable queries.
- **`fipsCode` (String)**: 5-digit county FIPS code as string to preserve leading zeros.
- **`stateName` (String)**: Full state name for display purposes.
- **`jurisdiction` (String)**: Jurisdiction name (often uppercase) preserved as-is from source data.
- **`equipmentType` (String)**: Categorical value (e.g., `"scanner"`, `"DRE"`) stored as string.
- **`manufacturer` (String)**: Equipment manufacturer name (e.g., `"ES&S"`, `"Dominion"`).
- **`model` (String)**: Equipment model name (e.g., `"DS200"`, `"ImageCast"`).
- **`firstYearInUse` (Integer)**: Year value for range queries and age calculations. Integer enables efficient date-based filtering.
- **`barcode` (String)**: Categorical response (e.g., `"Yes"`, `"No"`, `"N/A"`) stored as string to preserve exact values.
- **`vppat` (String)**: Voter-verifiable paper audit trail status (e.g., `"Yes"`, `"No"`, `"N/A"`) stored as string.
- **`electionDayStandard` (Boolean)**: Binary flag for whether equipment is used on election day. Boolean enables efficient filtering (`{ electionDayStandard: true }`).
- **`electionDayAccessible` (Boolean)**: Same rationale as other Boolean flags.
- **`earlyVotingStandard` (Boolean)**: Same rationale as other Boolean flags.
- **`earlyVotingAccessible` (Boolean)**: Same rationale as other Boolean flags.
- **`mailBallotEquipment` (Boolean)**: Same rationale as other Boolean flags.
- **`extraText` (String)**: Free-form text field for additional equipment information. String type accommodates variable-length text.
- **`qualityScore` (Double)**: Calculated quality metric (0.0-1.0) requiring decimal precision. Pre-computed and stored for performance.

**Indexes**:
- `_id` (unique)
- `{stateAbbr: 1, fipsCode: 1, manufacturer: 1, model: 1, equipmentType: 1}` (unique compound)
- `fipsCode`
- `qualityScore`

---

### 3.5 Felony Voting Policy

#### `FelonyPolicy` (Collection Name)
**Model Class**: `FelonyPolicyDoc`
```json
{
  "_id": "25",
  "stateAbbr": "MA",
  "stateFips": "25",
  "stateFull": "Massachusetts",
  "q51Fields": {
    "Q51_1": "1",
    "Q51_2": "0",
    "Q51_REF": "0",
    "Q51a_1": "1",
    "Q51a_2": "0",
    "Q51b": "2",
    "Q51c_1": "0",
    "Q51Comment": "Only those voters who are currently incarcerated for a felony conviction are ineligible to vote."
  },
  "createdAt": ISODate("2024-01-01T00:00:00Z"),
  "updatedAt": ISODate("2024-01-01T00:00:00Z")
}
```

**Key Design Decisions**:
- **`_id` format**: Uses `stateFips` (e.g., `"25"`) for standardization with federal data
- **Q51 fields**: Stored as `Map<String, String>` to accommodate variable field names and values
- **One document per state**: 5 states total (IA, IL, MA, NC, WA)

**Data Type Rationale**:
- **`_id` (String)**: State FIPS code stored as string (e.g., `"25"`) to match federal data format and enable string-based lookups.
- **`stateAbbr` (String)**: 2-letter state abbreviation for human-readable queries and display.
- **`stateFips` (String)**: Federal 2-digit FIPS code stored as string to preserve format consistency with source data.
- **`stateFull` (String)**: Full state name for display purposes.
- **`q51Fields` (Map<String, String>)**: 
  - **Key (String)**: Field names vary by state (e.g., `"Q51_1"`, `"Q51a_1"`, `"Q51Comment"`). String keys accommodate variable schema.
  - **Value (String)**: Responses include numeric codes (`"0"`, `"1"`, `"2"`), text comments, and various formats. String type:
    - Preserves exact response format from source data
    - Accommodates both numeric codes and free-text comments
    - Avoids type coercion issues (some fields are codes, others are text)
    - Enables flexible querying without type assumptions
  - **Why not structured fields**: Q51 survey has variable fields per state. Map structure accommodates schema variation without requiring nullable fields for every possible Q51 sub-question.

**Indexes**:
- `_id` (unique, on `stateFips`)
- `stateAbbr` (for lookups)

---

### 3.6 WA Registration Data

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

**Data Type Rationale**:
- **`_id` (String)**: Composite key `"WA|ADAMS|18-24"` uniquely identifies each age group per county.
- **`stateAbbr` (String)**: 2-letter state abbreviation. All records are `"WA"` but stored for consistency and potential multi-state expansion.
- **`countyName` (String)**: County name in uppercase (e.g., `"ADAMS"`) preserved as-is from source data.
- **`ageGroup` (String)**: Age range category (e.g., `"18-24"`, `"25-34"`). String type preserves hyphen format and enables exact matching.
- **`registeredVoters` (Integer)**: Voter counts are whole numbers. Integer enables efficient aggregation (sum, average) and comparison operations.

**Indexes**:
- `_id` (unique)
- `{stateAbbr: 1, countyName: 1, ageGroup: 1}` (unique compound)
- `countyName`

---

#### `WaVotersPartyAggregated` (Collection Name)
**Model Class**: `WaVotersPartyAggregatedDoc`
```json
{
  "_id": "53|AD|REP|18-24|F|Active",
  "stateFips": 53,
  "countyCode": "AD",
  "countyName": "Adams",
  "countyMajorityParty": "REP",
  "ageGroup2024": "18-24",
  "gender": "F",
  "statusCode": "Active",
  "voterCount": 1234,
  "createdAt": ISODate("2024-01-01T00:00:00Z"),
  "updatedAt": ISODate("2024-01-01T00:00:00Z")
}
```

**Key Design Decisions**:
- **`_id` format**: Composite key `"stateFips|countyCode|countyMajorityParty|ageGroup2024|gender|statusCode"` uniquely identifies each demographic/party combination
- **Party preference proxy**: Uses `countyMajorityParty` (REP/DEM) as a proxy for individual party preference since WA doesn't track party registration
- **Pre-aggregated data**: Voter counts are pre-aggregated by county, party, age, gender, and status for efficient queries

**Data Type Rationale**:
- **`_id` (String)**: Composite key as string for MongoDB `_id` requirement. Format enables efficient lookups and maintains human readability.
- **`stateFips` (Integer)**: State FIPS code (53 for Washington). Integer type enables numeric comparisons and joins with other federal datasets. Stored as Integer (not String) for consistency with federal data standards and efficient numeric operations.
- **`countyCode` (String)**: 2-letter county code (e.g., `"AD"` for Adams County). String type preserves format and enables exact matching.
- **`countyName` (String)**: Human-readable county name (e.g., `"Adams"`). Text field for display purposes.
- **`countyMajorityParty` (String)**: County majority party based on voting patterns: `"REP"` (Republican) or `"DEM"` (Democratic). This is a proxy for party preference since WA doesn't track party registration. String type enables categorical filtering and grouping.
- **`ageGroup2024` (String)**: Age group category (e.g., `"18-24"`, `"25-34"`, `"<18"`, `"Unknown"`). String type preserves hyphen format and enables exact matching.
- **`gender` (String)**: Gender category: `"F"` (Female), `"M"` (Male), `"Unknown"`. String type enables categorical filtering.
- **`statusCode` (String)**: Voter registration status: `"Active"` or `"Inactive"`. String type enables categorical filtering.
- **`voterCount` (Integer)**: Number of voters in this category (county, party, age, gender, status combination). Integer enables efficient aggregation (sum, average) and comparison operations. Whole numbers represent discrete voter counts.

**Use Cases**:
- **GUI-19**: Display registered voters with party preference proxy
- **GUI-18**: Voter registration bubble chart (can be used with county majority party as proxy)

**Indexes**:
- `_id` (unique)
- `{stateFips: 1, countyCode: 1, countyMajorityParty: 1, ageGroup2024: 1, gender: 1, statusCode: 1}` (unique compound)
- `stateFips` (for state-level queries)
- `countyCode` (for county-level queries)
- `countyMajorityParty` (for party-based filtering)

---

### 3.7 Geographic Data

#### `GeoStates` (Collection Name)
**Model Class**: `GeoStateDoc`
```json
{
  "_id": "25",
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

**Key Design Decisions**:
- **`_id` format**: Uses `stateFips` (e.g., `"25"`) instead of `stateAbbr` for standardization with federal data
- **Individual documents**: Each state stored as a separate document (51 total: 50 states + DC)
- **GeoJSON storage**: Full geometry stored as `org.bson.Document` for direct frontend consumption

**Data Type Rationale**:
- **`_id` (String)**: State FIPS code stored as string (e.g., `"25"`) to match federal data format. String type enables consistent lookups across collections.
- **`stateAbbr` (String)**: 2-letter state abbreviation for human-readable queries and display.
- **`stateFips` (String)**: Federal 2-digit FIPS code stored as string to preserve leading zeros and format consistency.
- **`stateName` (String)**: Full state name for display purposes.
- **`centroidLon` (Double)**: Longitude coordinate requiring decimal precision (e.g., `-71.5376`). Double enables accurate geospatial calculations.
- **`centroidLat` (Double)**: Latitude coordinate requiring decimal precision (e.g., `42.2373`). Double for consistency with longitude.
- **`geometry` (Document)**: GeoJSON geometry stored as `org.bson.Document` (MongoDB's native document type) because:
  - Preserves complete GeoJSON structure (type, coordinates)
  - Enables MongoDB geospatial queries (`$geoWithin`, `$geoIntersects`)
  - Can be directly serialized to JSON for frontend consumption
  - Supports `2dsphere` indexing for efficient spatial queries
  - More efficient than storing as nested Java objects (avoids serialization overhead)

**Indexes**:
- `_id` (unique, on `stateFips`)
- `stateAbbr` (for lookups)
- `stateFips` (for lookups)
- `2dsphere` index on `geometry`

---

#### `GeoCounties` (Collection Name)
**Model Class**: `GeoCountyDoc`
```json
{
  "_id": "MA|25001",
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

**Key Design Decisions**:
- **`_id` format**: `"stateAbbr|fips5"` (e.g., `"MA|25001"`) - uses `stateAbbr` for human readability
- **Individual documents**: Each county stored as a separate document
- **GeoJSON storage**: Full geometry stored as `org.bson.Document` for direct frontend consumption

**Data Type Rationale**:
- **`_id` (String)**: Composite key `"stateAbbr|fips5"` for unique identification. Uses `stateAbbr` for human readability in queries.
- **`stateAbbr` (String)**: 2-letter state abbreviation for human-readable queries and filtering.
- **`fips5` (String)**: 5-digit county FIPS code as string to preserve leading zeros (e.g., `"01001"` for Alabama counties).
- **`countyName` (String)**: Human-readable county name for display purposes.
- **`centroidLon` (Double)**: Longitude coordinate requiring decimal precision. Double enables accurate geospatial calculations.
- **`centroidLat` (Double)**: Latitude coordinate requiring decimal precision. Double for consistency with longitude.
- **`geometry` (Document)**: GeoJSON geometry stored as `org.bson.Document` for same reasons as `GeoStates`:
  - Preserves complete GeoJSON structure
  - Enables MongoDB geospatial queries
  - Direct JSON serialization for frontend
  - Supports `2dsphere` indexing

**Indexes**:
- `_id` (unique)
- `{stateAbbr: 1, fips5: 1}` (unique compound) - `geo_county_state_fips_unique`
- `fips5`
- `2dsphere` index on `geometry`

---

#### `GeoWAPrecincts` (Collection Name)
**Model Class**: `GeoWaPrecinctDoc`
```json
{
  "_id": "WA|53001|001",
  "stateAbbr": "WA",
  "countyFips": "53001",
  "countyName": "Adams County",
  "precinctNumber": "001",
  "precinctName": "Precinct 001",
  "centroidLon": -118.9876,
  "centroidLat": 46.7890,
  "geometry": {
    "type": "MultiPolygon",
    "coordinates": [[[[/* coordinates */]]]]
  },
  "createdAt": ISODate("2024-01-01T00:00:00Z"),
  "updatedAt": ISODate("2024-01-01T00:00:00Z")
}
```

**Data Type Rationale**:
- **`_id` (String)**: Composite key `"stateAbbr|countyFips|precinctNumber"` uniquely identifies each WA precinct.
- **`stateAbbr` (String)**: 2-letter state abbreviation. All records are `"WA"` but stored for consistency.
- **`countyFips` (String)**: 5-digit county FIPS code as string to preserve leading zeros (e.g., `"53001"`).
- **`countyName` (String)**: Human-readable county name for display purposes.
- **`precinctNumber` (String)**: Precinct identifier (e.g., `"001"`, `"123"`). String type preserves leading zeros and accommodates variable formats.
- **`precinctName` (String)**: Human-readable precinct name (e.g., `"Precinct 001"`).
- **`centroidLon` (Double)**: Longitude coordinate requiring decimal precision. Double enables accurate geospatial calculations.
- **`centroidLat` (Double)**: Latitude coordinate requiring decimal precision. Double for consistency with longitude.
- **`geometry` (Document)**: GeoJSON geometry stored as `org.bson.Document` for same reasons as other geographic collections:
  - Preserves complete GeoJSON structure
  - Enables MongoDB geospatial queries
  - Direct JSON serialization for frontend
  - Supports `2dsphere` indexing

**Indexes**:
- `_id` (unique)
- `{countyFips: 1, precinctNumber: 1, precinctName: 1}` (unique compound) - `geo_wa_precinct_unique`
- `2dsphere` index on `geometry`

---

### 3.8 Pre-aggregated Collections

#### `eavs_choropleth` (Collection Name)
**Model Class**: `ProvisionalChoroplethDoc`
```json
{
  "_id": "2024|E1A",
  "year": 2024,
  "measure": "E1A",
  "bins": [0.0, 10.0, 105.14, 500.0, 1000.0, 5000.0, 10000.0],
  "states": {
    "25": {
      "values": {
        "25025": 1800.0,
        "25027": 950.0,
        "25001": 1274.0
      }
    },
    "19": {
      "values": {
        "19001": 50.0,
        "19003": 120.0
      }
    }
  },
  "createdAt": ISODate("2024-01-01T00:00:00Z"),
  "updatedAt": ISODate("2024-01-01T00:00:00Z")
}
```

**Data Type Rationale**:
- **`_id` (String)**: Composite key `"year|measure"` (e.g., `"2024|E1A"`) uniquely identifies each choropleth dataset.
- **`year` (int)**: Year value for filtering and grouping. Integer enables efficient range queries.
- **`measure` (String)**: EAVS measure code (e.g., `"E1A"`, `"E2A"`). String type preserves categorical identifier format.
- **`bins` (List<Double>)**: Bin boundaries for choropleth color mapping. Double type required for decimal precision in bin calculations (e.g., `105.14`).
- **`states` (Map<String, StateValues>)**: 
  - **Key (String)**: State FIPS code (e.g., `"25"` for Massachusetts). String type preserves leading zeros and enables consistent lookups.
  - **Value (StateValues)**: Nested structure containing county values.
- **`StateValues.values` (Map<String, Double>)**:
  - **Key (String)**: County FIPS code (e.g., `"25025"`). String type preserves leading zeros.
  - **Value (Double)**: Choropleth value requiring decimal precision for accurate color binning and visualization.

**Indexes**:
- `_id` (unique)
- `{year: 1, measure: 1}` (unique compound) - `yr_measure_unique`

---

#### `eavs_metrics` (Collection Name)
**Model Class**: `EavsMetricDoc`
```json
{
  "_id": "2024|NY|NY-36061",
  "year": 2024,
  "stateId": "NY",
  "regionId": "NY-36061",
  "categories": {
    "provisional": { "E1A": 1000, "E2A": 200 },
    "activeVoters": { "active": 50000, "inactive": 2000 },
    "earlyVoting": {
      "total": 15000.5,
      "inPerson": 8000.0,
      "mail": 7000.5
    }
  },
  "derived": {
    "missingnessScore": 0.42,
    "turnoutPct": 65.3
  },
  "provenance": {
    "source": "eavs_2024.csv",
    "version": "1.0",
    "ingestedAt": "2024-01-15T10:30:00Z"
  },
  "createdAt": ISODate("2024-01-01T00:00:00Z"),
  "updatedAt": ISODate("2024-01-01T00:00:00Z")
}
```

**Data Type Rationale**:
- **`_id` (String)**: Composite key `"year|stateId|regionId"` uniquely identifies each pre-aggregated metric set.
- **`year` (int)**: Year value for filtering. Integer enables efficient range queries.
- **`stateId` (String)**: State identifier (e.g., `"NY"`). String type preserves format consistency.
- **`regionId` (String)**: Region identifier (e.g., `"NY-36061"` for New York County). String type accommodates variable region naming schemes.
- **`categories` (Categories)**: Nested structure containing pre-aggregated metrics.
  - **`provisional` (Map<String, Object>)**: Mixed types (Integer counts, potentially Double percentages). Object type accommodates schema variation.
  - **`activeVoters` (Map<String, Object>)**: Same rationale - mixed integer counts.
  - **`earlyVoting.total` (Double)**: Aggregated totals may require decimal precision when averaging across jurisdictions.
  - **`earlyVoting.inPerson` (Double)**: Same rationale as total.
  - **`earlyVoting.mail` (Double)**: Same rationale as total.
  - **`earlyVoting.raw` (Map<String, Object>)**: Raw source data with variable types preserved.
- **`derived` (Map<String, Object>)**: 
  - **Key (String)**: Metric name (e.g., `"missingnessScore"`, `"turnoutPct"`).
  - **Value (Object)**: Mixed types (Double for scores/percentages, potentially Integer for counts). Object type accommodates different metric types.
- **`provenance` (Map<String, Object>)**: 
  - **Key (String)**: Provenance field name (e.g., `"source"`, `"version"`).
  - **Value (Object)**: Mixed types (String for source/version, Date/ISO string for timestamps). Object type accommodates metadata variation.

**Indexes**:
- `_id` (unique)
- `{stateId: 1, regionId: 1, year: 1}` (unique compound) - `yr_state_region_unique`

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
// Step 1: Get state geometry (by stateFips or stateAbbr)
db.GeoStates.findOne({ stateFips: "25" })  // or { stateAbbr: "MA" }

// Step 2: Get counties with geometry
db.GeoCounties.find({ stateAbbr: "MA" })

// Step 3: Aggregate EAVS and CVAP data by county
db.Eavs.aggregate([
  { $match: { stateFips: 25, year: 2024 } },  // MA stateFips = 25
  {
    $lookup: {
      from: "CvapCounty",
      let: { fips5: "$fips5", stateAbbr: "$stateAbbr" },
      pipeline: [
        { $match: { $expr: { $and: [
          { $eq: ["$fips5", "$$fips5"] },
          { $eq: ["$stateAbbr", "MA"] },
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
        // 1. Get state geometry (lookup by stateAbbr, then use stateFips)
        GeoStateDoc state = geoStateRepo.findByStateAbbr(stateAbbr).orElseThrow();
        Integer stateFips = Integer.parseInt(state.getStateFips());
        
        // 2. Get counties with geometry
        List<GeoCountyDoc> counties = geoCountyRepo.findByStateAbbr(stateAbbr);
        
        // 3. Get EAVS data for 2024 (query by stateFips)
        List<EavsDoc> eavsData = eavsRepo.findByStateFipsAndYear(stateFips, 2024);
        
        // 4. Get CVAP data
        List<CvapCountyDoc> cvapData = cvapRepo.findByStateAbbrAndCvapCategoryCode(stateAbbr, "1");
        
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
  - Join key: `stateFips` (Eavs) ↔ `stateFips` (GeoStates), `fips5` (Eavs) ↔ `fips5` (GeoCounties, CvapCounty)
- **GUI-15**: State comparison → `FelonyPolicy` + `Eavs` + `PresResultsCounty`
  - Join key: `stateFips` (FelonyPolicy) ↔ `stateFips` (Eavs), `fips5` (Eavs) ↔ `fips5` (PresResultsCounty)
- **GUI-24**: Drop box bubble chart → `Eavs` + `PresResultsCounty` + `GeoCounties`
  - Join key: `fips5` across all collections

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

### 5.0 Data Range Overview

**EAVS Data**:
- **Years**: 2016-2024 (9 years)
- **States**: All 50 states + DC (51 jurisdictions)
- **Jurisdictions**: ~3,000+ counties/jurisdictions per year
- **Total records**: ~27,000+ EAVS documents

**Geographic Data**:
- **States**: 51 (50 states + DC)
- **Counties**: ~3,200 counties nationwide
- **WA Precincts**: ~8,100 precincts (2024)

**Election Results**:
- **Year**: 2024 only
- **PresResultsCounty**: IA, WA counties
- **PresResultsMaTown**: MA towns

**CVAP Data**:
- **Year**: 2023 (5-year ACS estimates)
- **Categories**: Multiple race/ethnicity categories per county

**Equipment Data**:
- **Year**: 2022
- **Coverage**: All states with equipment data

**Felony Policy**:
- **Year**: 2024
- **States**: IA, IL, MA, NC, WA (5 states)

**WA Registration Data**:
- **Years**: 2024 (participation), various (registration)
- **Coverage**: All 39 WA counties

---

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
- `{year: 1, stateFips: 1, fips5: 1}` on `Eavs` (unique) - `year_state_fips_unique`
- `{year: 1, stateFips: 1}` on `Eavs` - `year_state_idx`
- `{year: 1, fips5: 1}` on `Eavs` - `year_fips5_idx`
- `{stateAbbr: 1, fips5: 1}` on `GeoCounties` (unique) - `geo_county_state_fips_unique`
- `{stateAbbr: 1, fips5: 1, cvapCategoryCode: 1}` on `CvapCounty` (unique) - `state_fips_category_unique`
- `{stateAbbr: 1, fips5: 1}` on `PresResultsCounty` (unique)
- `{countyFips: 1, precinctNumber: 1, precinctName: 1}` on `GeoWAPrecincts` (unique) - `geo_wa_precinct_unique`
- `{year: 1, measure: 1}` on `eavs_choropleth` (unique) - `yr_measure_unique`

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
All 20 importers are implemented and **disabled by default** in `application.properties`:
- Enable specific importers by setting `*.enabled=true`
- Large files (`wa_voter`, `wa_voter_history`) are disabled by default
- GeoJSON importers (`GeoStates`, `GeoCounties`, `GeoWAPrecincts`) split features into individual documents

### Data Structure
- `EavsDoc` uses nested categories: `registration`, `voting`, `mailBallots`, `provisional`, `equipment`, `other`
- All EAVS fields organized by category for easier querying
- All nested maps use `Map<String, Integer>` (not `Object`) for type safety
- Sentinel values (`-999999`, `-888888`, negative numbers) stored as `-1`
- `jurisdictionName` stored at top level and in `provisional.jurisdictionName`
- Geographic collections store GeoJSON geometry as `org.bson.Document`
- Geographic collections store individual features (not raw FeatureCollections) for efficient querying

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



