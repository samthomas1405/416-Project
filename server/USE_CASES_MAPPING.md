# Use Cases to Database Schema Mapping

This document maps all use cases to the database collections and identifies implementation requirements.

## Use Case Categories

- **GUI (22 required)**: User interface displays and interactions
- **Preprocessing (11 required)**: Data preparation and database population
- **Server Processing (2 required)**: Query formulation and JSON generation

> **Note:** EAVS field references now use the structured schema paths (e.g., `registration.totalRegistered.A1A`, `mailBallots.mailBallotsRejected.C9A`) instead of the legacy `questions.*` map.

---

## GUI Use Cases → Database Collections

### GUI-1: Display map of US on splash page (required)
**Collections**: `geo_states`
- Query: All states with geometry
- Fields: `stateAbbr`, `stateName`, `geometry`, `centroidLon`, `centroidLat`
- Filter: Highlight detailed states (IA, IL, MA, NC, WA)

---

### GUI-2: Display State (required) (SD)
**Collections**: `geo_states`, `geo_counties`, `eavs_all`, `cvap_county_2023_long`
- Query: State geometry, county/town boundaries, EAVS 2024 data, CVAP data
- Fields: 
  - State: `geometry`, `centroidLon`, `centroidLat`
  - Counties: `fips5`, `countyName`, `geometry` (for detailed states)
  - EAVS: `registration.totalRegistered.A1A` (total registered), `missingnessScore`
  - CVAP: `cvapEstimate` (for percentage calculation)
- Calculation: `(EAVS A1A / CVAP) * 100` for Political Party states

---

### GUI-3: Provisional ballot bar chart (required) (SD)
**Collections**: `eavs_all`
- Query: Aggregate E2A-E2I by state for 2024
- Fields: `provisional.provisionalBallotCategories.E2A`, `provisional.provisionalBallotCategories.E2B`, ..., `provisional.provisionalBallotCategories.E2I`
- Aggregation: Sum by `stateAbbr` where `year = 2024`

---

### GUI-4: Provisional ballot table (required) (SD)
**Collections**: `eavs_all`
- Query: All EAVS regions for selected state, year 2024
- Fields: `jurisdictionName`, `fips5`, `provisional.provisionalBallotCategories.E2A` through `provisional.provisionalBallotCategories.E2I`
- Filter: `stateAbbr = selectedState`, `year = 2024`
- Sort: By `jurisdictionName` or `fips5`

---

### GUI-5: Provisional ballot choropleth map (required) (SD)
**Collections**: `eavs_all`, `geo_counties` (or town boundaries for MA)
- Query: E1A (Total Provisional Ballots Cast) by region
- Fields: `fips5`, `provisional.provisionalBallotsCast.E1A`, `geometry`
- Filter: `stateAbbr = selectedState`, `year = 2024`
- Join: `eavs_all.fips5` → `geo_counties.fips5`

---

### GUI-6: State voting equipment summary (required) (SD)
**Collections**: `equipment_device_2022`
- Query: All equipment for selected state
- Fields: `manufacturer`, `model`, `equipmentType`, `firstYearInUse`, `qualityScore`, and all equipment metadata
- Filter: `stateAbbr = selectedState`
- Group: By `manufacturer`, `model`
- Calculate: Age = `2024 - firstYearInUse`
- Highlight: Discontinued models (if `isDiscontinued = true`)

---

### GUI-7: Display 2024 EAVS active voters (required)
**Collections**: `eavs_all`
- Query: Active voters data by region
- Fields: 
  - Bar chart: `registration.totalRegistered.A1B` (active), `registration.totalRegistered.A1A` (total), `registration.totalRegistered.A1C` (inactive)
  - Table: Same fields by `jurisdictionName`
  - Map: `(registration.totalRegistered.A1B / registration.totalRegistered.A1A) * 100` as percentage
- Filter: `stateAbbr = selectedState`, `year = 2024`

---

### GUI-8: Display 2024 EAVS pollbook deletions (preferred)
**Collections**: `eavs_all`
- Query: Pollbook deletion categories by region
- Fields: 
  - Bar chart: `registration.pollbookDeletions.A12B` through `registration.pollbookDeletions.A12H`
  - Table: Same fields by `jurisdictionName`
  - Map: `(sum(A12B:A12H) / registration.totalRegistered.A1A) * 100` as percentage
- Filter: `stateAbbr = selectedState`, `year = 2024`

---

### GUI-9: Display mail ballots rejected (preferred)
**Collections**: `eavs_all`
- Query: Mail ballot rejection categories by region
- Fields:
  - Bar chart: `mailBallots.mailBallotsRejected.C9B` through `mailBallots.mailBallotsRejected.C9Q`
  - Table: Same fields by `jurisdictionName`
  - Map: `(sum(C9B:C9Q) / mailBallots.mailBallotsRejected.C9A) * 100` as percentage
- Filter: `stateAbbr = selectedState`, `year = 2024`

---

### GUI-10: Display type of voting equipment (preferred)
**Collections**: `equipment_device_2022`, `geo_counties`
- Query: Equipment type by jurisdiction
- Fields: `fipsCode`, `equipmentType`, `geometry`
- Filter: `stateAbbr = selectedState`
- Group: By `fipsCode`, determine dominant `equipmentType`
- Categories: DRE_no_VVPAT, DRE_with_VVPAT, BMD, scanner

---

### GUI-11: Display relative age of voting equipment (preferred)
**Collections**: `equipment_device_2022`
- Query: Average age of equipment by state
- Fields: `stateAbbr`, `firstYearInUse`
- Calculate: `avg(2024 - firstYearInUse)` grouped by `stateAbbr`
- Filter: All states
- Bins: 1-10 years, >10 years

---

### GUI-12: Display voting equipment in US (required)
**Collections**: `equipment_device_2022`
- Query: Equipment counts by state and type
- Fields: `stateAbbr`, `equipmentType`
- Group: By `stateAbbr`, `equipmentType`
- Count: Number of devices per category
- Filter: Year 2024 (or most recent)

---

### GUI-13: Display of US voting equipment summary (required) (SD)
**Collections**: `equipment_device_2022`
- Query: All equipment with details
- Fields: `manufacturer`, `model`, `equipmentType`, `firstYearInUse`, `qualityScore`, and all metadata
- Sort: By `manufacturer`, then `model`
- Filter: All states, year 2024

---

### GUI-14: Display voting equipment history for a state (required)
**Collections**: `equipment_device_2022` (or historical equipment data if available)
- Query: Equipment counts by category and year
- Fields: `equipmentType`, `firstYearInUse`, `quantityByYear` (if available)
- Filter: `stateAbbr = selectedState`
- Group: By `equipmentType`, aggregate by year (2016-2024)

**Note**: May need to derive from `firstYearInUse` or use historical EAVS data if available.

---

### GUI-15: Compare Republican and Democratic states (required) (SD)
**Collections**: `felony_policy_2024`, `eavs_all`, `pres_results_2024_county`
- Query: Comparison data for two states
- Fields:
  - Felony rights: `felony_policy_2024.Q51_*` fields
  - Mail ballots: `eavs_all.mailBallots.mailBallotsSent.C1A` / `eavs_all.voting.earlyVotingTotals.B5A` (percentage)
  - Drop box: `eavs_all.mailBallots.dropBoxReturns.C3A` (if available)
  - Turnout: `eavs_all.voting.totalVotes.B1A` / CVAP
- Filter: `stateAbbr IN [repState, demState]`, `year = 2024`

---

### GUI-16: Compare changes in voter registration (preferred)
**Collections**: `eavs_all`
- Query: Registered voters by region for 2016, 2020, 2024
- Fields: `fips5`, `year`, `registration.totalRegistered.A1A` (total registered)
- Filter: `stateAbbr = selectedState`, `year IN [2016, 2020, 2024]`
- Sort: By `registration.totalRegistered.A1A` for 2024 (ascending)
- Group: By `fips5`, extract values for each year

---

### GUI-17: Display voter registration data (required)
**Collections**: `wa_registration_county_age`, `wa_registration_county_gender`, `eavs_all`
- Query: Registration data by region
- Fields:
  - Map: `(registeredVoters / CVAP) * 100` as percentage
  - Table: `countyName`, `registeredVoters`, party breakdowns (if available)
- Filter: `stateAbbr = 'WA'` (or other registration state)
- Join: `wa_registration_county_age.countyName` → `eavs_all.jurisdictionName`

**Note**: WA doesn't have party registration. Other states may have it.

---

### GUI-18: Display voter registration bubble chart (preferred) (SD)
**Collections**: `wa_voter` (or aggregated data), `geo_counties`
- Query: Party dominance by census block
- Fields: `precinctCode`, `countyCode`, `centroidLon`, `centroidLat`, party counts
- Filter: `stateAbbr = 'WA'`
- Calculate: Dominant party per census block
- **Note**: Requires census block data. May need to aggregate from `wa_voter` or use `wa_vrdb_precinct_demo`.

---

### GUI-19: Display registered voters (required)
**Collections**: `wa_voter`
- Query: Voter names by region
- Fields: `stateVoterId`, `countyName`, `precinctCode`, party (if available)
- Filter: `countyName = selectedRegion`, optionally `party = selectedParty`
- **Note**: This requires voter names, which may not be in `data_clean`. Check if available in raw VRDB.

---

### GUI-20: Display 2024 EAVS voting regions when the state is selected (required) (SD)
**Collections**: `geo_counties` (or town boundaries for MA), `eavs_all`
- Query: EAVS region boundaries for selected state
- Fields: `fips5`, `geometry`, `jurisdictionName`
- Filter: `stateAbbr = selectedState`
- Join: `geo_counties.fips5` → `eavs_all.fips5` where `year = 2024`

---

### GUI-21: Compare voter registration data for opt-in and opt-out (required)
**Collections**: `eavs_all`, `felony_policy_2024`
- Query: Registration and turnout rates for three states
- Fields:
  - Registration rate: `registration.totalRegistered.A1A` / CVAP
  - Turnout rate: `voting.totalVotes.B1A` / CVAP
  - Same-day registration: `registration.sameDayRegistration.A3A` (if available)
- Filter: `stateAbbr IN [optInState, optOutSameDayState, optOutNoSameDayState]`, `year = 2024`

---

### GUI-22: Compare Republican and Democratic states (required)
**Collections**: `eavs_all`, `cvap_county_2023_long`
- Query: Registration and turnout data for two states
- Fields:
  - Registration: `registration.totalRegistered.A1A` (absolute), `(registration.totalRegistered.A1A / cvapEstimate) * 100` (percentage)
  - Turnout: `voting.totalVotes.B1A` (absolute), `(voting.totalVotes.B1A / cvapEstimate) * 100` (percentage)
- Filter: `stateAbbr IN [repState, demState]`, `year = 2024`
- Join: `eavs_all.fips5` → `cvap_county_2023_long.fips5` where `cvapCategoryCode = '1'` (Total)

---

### GUI-23: Compare Republican and Democratic states early voting (required)
**Collections**: `eavs_all`
- Query: Early voting categories for two states
- Fields:
  - `voting.earlyVotingTotals.B5A` (total early voting)
  - `voting.earlyVotingCategories.B6A`, `voting.earlyVotingCategories.B6B`, `voting.earlyVotingCategories.B6C` (early voting categories)
  - `voting.totalVotes.B1A` (total votes) for percentage calculation
- Filter: `stateAbbr IN [repState, demState]`, `year = 2024`
- Calculate: `(earlyVotingCategory / voting.totalVotes.B1A) * 100` for each category

---

### GUI-24: Drop box voting bubble chart (required) (SD)
**Collections**: `eavs_all`, `pres_results_2024_county` (or `pres_results_2024_ma_town`)
- Query: Drop box usage and party vote split by region
- Fields:
  - X-axis: `(votesRep2024Pres / totalVotes2024Pres) * 100`
  - Y-axis: `(mailBallots.dropBoxReturns.C3A / voting.totalVotes.B1A) * 100`
  - Color: Dominant party (red if rep > dem, blue if dem > rep)
- Filter: `stateAbbr IN [repState, demState]`, `year = 2024`
- Join: `eavs_all.fips5` → `pres_results_2024_county.fips5`

---

### GUI-25: Bubble chart for voting equipment quality and rejected ballots (required)
**Collections**: `equipment_jurisdiction_2022`, `eavs_all`
- Query: Equipment quality vs rejection rate by region
- Fields:
  - X-axis: `avgQualityScore`
  - Y-axis: `((mailBallots.mailBallotsRejected.C9A + provisional.provisionalBallotsCast.E1D + voting.uocavaBallots.B24A) / (voting.earlyVotingTotals.B5A + voting.earlyVotingCategories.B6A + voting.totalVotes.B1A + provisional.provisionalBallotsCast.E1A)) * 100`
- Filter: `stateAbbr = selectedState`, `year = 2024`
- Join: `equipment_jurisdiction_2022.fipsCode` → `eavs_all.fips5`

---

### GUI-26: Bubble chart regression line (preferred) (SD)
**Collections**: Same as GUI-25, plus `pres_results_2024_county`
- Query: Same as GUI-25, plus party affiliation
- Fields: Same as GUI-25, plus `votesDem2024Pres`, `votesRep2024Pres`
- Calculate: Non-linear regression for Democratic and Republican bubbles separately
- **Note**: Regression calculation should be done server-side or preprocessed.

---

### GUI-27: Display Gingles Chart (required) (SD)
**Collections**: `pres_results_2024_ma_town` (or precinct-level results), `cvap_county_2023_long`
- Query: Vote percentages by demographic group by precinct
- Fields:
  - X-axis: `(demographicCVAP / totalCVAP) * 100` per precinct
  - Y-axis: `(votesDem2024Pres / totalVotes2024Pres) * 100` and `(votesRep2024Pres / totalVotes2024Pres) * 100`
- Filter: `stateAbbr = preclearanceState`
- Join: Precinct → CVAP data (may need aggregation from census blocks)
- **Note**: Requires precinct-level results and demographic data.

---

### GUI-28: Ecological Inference analysis of voting equipment (required) (SD)
**Collections**: `equipment_jurisdiction_2022`, `cvap_county_2023_long`, `eavs_all`
- Query: Equipment quality by demographic group
- Fields: `avgQualityScore`, `cvapCategory`, `cvapEstimate`
- Filter: `stateAbbr = preclearanceState`
- Calculate: EI probability curves for each demographic group
- **Note**: EI calculation is complex and should be done server-side or preprocessed.

---

### GUI-29: Ecological Inference analysis of rejected ballots (required)
**Collections**: `eavs_all`, `cvap_county_2023_long`
- Query: Rejection rates by demographic group
- Fields: Rejection rate, `cvapCategory`, `cvapEstimate`
- Filter: `stateAbbr = preclearanceState`, `year = 2024`
- Calculate: EI probability curves for each demographic group
- **Note**: EI calculation is complex and should be done server-side or preprocessed.

---

### GUI-30: Reset page (preferred)
**Collections**: None (client-side only)

---

## Preprocessing Use Cases → Database Collections

### Prepro-1: Add boundary data to your DB (required) (AD)
**Collections**: `geo_states`, `geo_counties`
- Source: `data_clean/geo/us_states.geojson`, `data_clean/geo/us_counties_selected.geojson`
- Extract: Geometry, centroid, zoom level
- Store: GeoJSON format in MongoDB

---

### Prepro-2: DB Design for EAVS Data (required) (schema)
**Collections**: `eavs_all`
- Design: See `DATABASE_SCHEMA.md`
- Fields: All EAVS question columns (A1A, A1B, ..., E1A, E1B, etc.)
- Indexes: `year`, `stateAbbr`, `fips5`

---

### Prepro-3: Populate your DB with EAVS data (required)
**Collections**: `eavs_all`
- Source: `data_clean/eavs/eavs_2016_2024_normalized.csv`
- Years: 2016, 2018, 2020, 2022, 2024
- Import: All question fields into `questions` map

---

### Prepro-4: Add geographic data to your DB (required)
**Collections**: `geo_counties`, `geo_wa_precincts`
- Source: 
  - `data_clean/geo/us_counties_selected.geojson` (for all detailed states)
  - `data_clean/geo/wa_precincts_2024.geojson` (for WA)
- Convert: To GeoJSON if needed
- Store: Geometry in MongoDB

---

### Prepro-5: Develop a measure of missing EAVS data (required)
**Collections**: `eavs_all`
- Field: `missingnessScore` (0-1 scale)
- Calculate: Based on missing values in key EAVS fields
- Source: Already in `eavs_2024_cleaned.csv` as `MISSINGNESS_SCORE`
- Import: Include in `eavs_all` documents

---

### Prepro-6: Develop a measure of voting equipment quality (required)
**Collections**: `equipment_device_2022`, `equipment_jurisdiction_2022`
- Field: `qualityScore` (0-1 scale)
- Factors: Age, OS, certification, scan rate, error rate, reliability
- Source: Already in `data_clean/equipment/equipment_2022_with_quality.csv`
- Import: Include in equipment documents

---

### Prepro-7: Analyze voter registration data for one state (required) (AD)
**Collections**: `wa_registration_county_age`, `wa_registration_county_gender`
- Source: `data_clean/registration/wa_registration_by_county_age.csv`, `wa_registration_by_county_gender.csv`
- Calculate: Total registered, party breakdowns (if available)
- **Note**: WA doesn't have party registration. Check other states.

---

### Prepro-8: Analyze voter registration data using an automated service (preferred)
**Collections**: `wa_voter`
- Source: `data_clean/registration/wa_vrdb_voters.csv`
- Service: External service (if available)
- Sample: At least 1% of voters

---

### Prepro-9: Determine census block for each voter in the registration dataset (preferred)
**Collections**: `wa_voter`
- Source: `data_clean/registration/wa_vrdb_voters.csv`
- Add: `censusBlock` field
- Method: Geocoding or spatial join

---

### Prepro-10: Determine EAVS region for each voter in registration dataset (required) (AD)
**Collections**: `wa_voter`
- Source: `data_clean/registration/wa_vrdb_voters.csv`
- Add: `eavsRegion` or `fips5` field
- Method: Spatial join with `geo_counties` or `geo_wa_precincts`

---

### Prepro-11: Calculate the Republican/Democratic vote split (required) (AD)
**Collections**: `pres_results_2024_county`, `pres_results_2024_ma_town`
- Source: 
  - `data_clean/results_2024/pres_2024_by_county.csv` (IA, WA)
  - `data_clean/results_2024/pres_2024_ma_by_town.csv` (MA)
- Calculate: `demShare2024Pres`, `repShare2024Pres`
- Aggregate: From precincts if needed

---

### Prepro-12: Add citizen voting age population (CVAP) to your DB (required)
**Collections**: `cvap_county_2023_long`
- Source: `data_clean/cvap/cvap_2019_2023_county_long.csv`
- Fields: Total CVAP, CVAP by demographic categories
- Aggregate: From census blocks to EAVS regions if needed
- Year: 2023 ACS data

---

### Prepro-13: Add felony voting data to your DB (required)
**Collections**: `felony_policy_2024`
- Source: `data_clean/eavs/felony_policy_2024_q51.csv`
- Categorize: 
  1. No denial of voting
  2. Automatic restoration upon release
  3. Restoration after completing parole/probation
  4. Additional action required
- States: IA, IL, MA, NC, WA

---

## Server Processing Use Cases

### Server-1: Formulate DB query (required)
**Implementation**: Spring Data MongoDB repositories
- Create: Repository interfaces for each collection
- Methods: Custom query methods for each use case
- Examples:
  - `findByStateAbbrAndYear(String stateAbbr, int year)`
  - `findByFips5AndYear(String fips5, int year)`
  - Aggregation pipelines for complex queries

---

### Server-2: Generate a JSON response (required)
**Implementation**: Spring REST controllers
- Create: Controller endpoints for each GUI use case
- Response: JSON with only required fields
- Format: Consistent structure across endpoints
- Examples:
  - `/api/states/{stateAbbr}/eavs/2024`
  - `/api/states/{stateAbbr}/equipment`
  - `/api/comparison/rep-vs-dem`

---

## Implementation Priority

### Phase 1: Core Infrastructure (Week 1-2)
1. ✅ Database schema design (Prepro-2)
2. ✅ MongoDB models for all collections
3. ✅ Importers for core data (Prepro-3, Prepro-4)
4. ✅ Basic repositories and controllers

### Phase 2: EAVS Data & Maps (Week 3-4)
1. EAVS data import (Prepro-3)
2. Geographic boundaries (Prepro-1, Prepro-4)
3. GUI-1, GUI-2, GUI-3, GUI-4, GUI-5 (splash page, state display, provisional ballots)
4. GUI-7, GUI-8, GUI-9 (active voters, pollbook deletions, mail rejections)

### Phase 3: Equipment & Quality (Week 5-6)
1. Equipment data import (Prepro-6)
2. GUI-6, GUI-10, GUI-11, GUI-12, GUI-13, GUI-14 (equipment displays)
3. GUI-25, GUI-26 (equipment quality vs rejections)

### Phase 4: Registration & Turnout (Week 7-8)
1. WA registration data import (Prepro-7, Prepro-10)
2. CVAP data import (Prepro-12)
3. GUI-16, GUI-17, GUI-18, GUI-19 (registration displays)
4. GUI-21, GUI-22, GUI-23 (comparisons)

### Phase 5: Advanced Analysis (Week 9-10)
1. Election results import (Prepro-11)
2. Felony policy import (Prepro-13)
3. GUI-15, GUI-24 (comparisons with results)
4. GUI-27, GUI-28, GUI-29 (Gingles, EI analysis)

### Phase 6: Polish & Testing (Week 11-12)
1. Missingness score (Prepro-5)
2. GUI-30 (reset)
3. Performance optimization
4. Testing and bug fixes

---

## Data Dependencies

```
geo_states, geo_counties
    ↓
eavs_all (requires fips5 mapping)
    ↓
cvap_county_2023_long (joins on fips5)
    ↓
pres_results_2024_county (joins on fips5)
    ↓
equipment_jurisdiction_2022 (joins on fipsCode → fips5)
    ↓
wa_registration_* (joins on countyName)
    ↓
wa_voter (for detailed analysis)
```

---

## Missing Data Considerations

1. **Party Registration**: Only available for some states (not WA)
2. **Voter Names**: May not be in `data_clean` (check raw VRDB)
3. **Census Blocks**: May need geocoding or spatial joins
4. **Historical Equipment**: May need to derive from `firstYearInUse`
5. **Precinct-Level Results**: May need aggregation from smaller units

---

## Next Steps

1. Review and confirm schema supports all use cases
2. Create MongoDB models for all collections
3. Implement importers for each data source
4. Create repository interfaces with custom queries
5. Implement REST controllers for each GUI use case
6. Test data imports and query performance
7. Implement frontend components



