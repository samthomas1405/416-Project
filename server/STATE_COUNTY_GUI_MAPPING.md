# State and County Schema → GUI Mapping

## Overview

This document explains how the state and county database collections map to GUI use cases and how they're queried.

---

## Collections Used

### State Collections

1. **`GeoStates`** (Model: `GeoStateDoc`)
   - **Purpose**: Geographic boundaries and metadata for states
   - **ID Field**: `stateFips` (e.g., "25" for Massachusetts, "53" for Washington)
   - **Key Fields**: `stateFips`, `stateAbbr`, `stateName`, `geometry`, `centroidLon`, `centroidLat`

2. **`states`** (Model: `StateDoc`)
   - **Purpose**: State metadata lookup (optional, can use GeoStates instead)
   - **ID Field**: `stateAbbr` (e.g., "MA", "WA")
   - **Key Fields**: `stateAbbr`, `stateName`, `stateFips`

### County Collections

1. **`GeoCounties`** (Model: `GeoCountyDoc`)
   - **Purpose**: Geographic boundaries and metadata for counties
   - **ID Field**: `stateAbbr-fips5` (e.g., "MA-25001")
   - **Key Fields**: `stateAbbr`, `fips5`, `countyName`, `geometry`, `centroidLon`, `centroidLat`

---

## GUI Use Cases → Collections Mapping

### GUI-1: Display map of US on splash page
**Collection**: `GeoStates`
- **Query**: Get all states with geometry
- **Fields Used**: `stateAbbr`, `stateName`, `geometry`, `centroidLon`, `centroidLat`
- **Filter**: Highlight detailed states (IA=19, IL=17, MA=25, NC=37, WA=53)
- **How to Query**: 
  ```java
  List<GeoStateDoc> allStates = geoStateRepo.findAll();
  // Filter by stateFips for detailed states: ["19", "17", "25", "37", "53"]
  ```

---

### GUI-2: Display State (Main State View)
**Collections**: `GeoStates`, `GeoCounties`, `Eavs`, `CvapCounty`
- **State Geometry**:
  - **Collection**: `GeoStates`
  - **Query**: `findById(stateFips)` or `findByStateFips(stateFips)`
  - **Fields**: `geometry`, `centroidLon`, `centroidLat`, `stateName`
  
- **County Boundaries** (for detailed states only):
  - **Collection**: `GeoCounties`
  - **Query**: `findByStateAbbr(stateAbbr)` or `findByStateFips(stateFips)`
  - **Fields**: `fips5`, `countyName`, `geometry`
  - **Join Key**: `fips5` (5-digit county FIPS code)
  
- **EAVS Data**:
  - **Collection**: `Eavs`
  - **Query**: `findByStateAbbrAndYear(stateAbbr, 2024)`
  - **Fields**: `registration.totalRegistered.A1A`, `missingnessScore`, `fips5`
  
- **CVAP Data**:
  - **Collection**: `CvapCounty`
  - **Query**: `findByStateAbbrAndFips5AndCvapCategoryCode(stateAbbr, fips5, "1")`
  - **Fields**: `cvapEstimate`
  - **Join**: `Eavs.fips5` → `CvapCounty.fips5`

**Example Flow**:
1. User clicks state on map → Frontend has `stateFips` (e.g., "25")
2. Backend queries `GeoStates` by `stateFips` → Get state geometry
3. Backend queries `GeoCounties` by `stateAbbr` (derived from `stateFips`) → Get county boundaries
4. Backend queries `Eavs` by `stateAbbr` and `year=2024` → Get EAVS data
5. Backend joins EAVS with CVAP using `fips5` → Calculate registration percentages

---

### GUI-5: Provisional ballot choropleth map
**Collections**: `Eavs`, `GeoCounties`
- **EAVS Data**:
  - **Collection**: `Eavs`
  - **Query**: `findByStateAbbrAndYear(stateAbbr, 2024)`
  - **Fields**: `fips5`, `provisional.provisionalBallotsCast.E1A`
  
- **County Geometry**:
  - **Collection**: `GeoCounties`
  - **Query**: `findByStateAbbr(stateAbbr)`
  - **Fields**: `fips5`, `geometry`
  
- **Join**: `Eavs.fips5` → `GeoCounties.fips5`
- **Result**: Color counties by provisional ballot count

---

### GUI-10: Display type of voting equipment
**Collections**: `EquipmentDevice`, `GeoCounties`
- **Equipment Data**:
  - **Collection**: `EquipmentDevice`
  - **Query**: `findByStateAbbr(stateAbbr)`
  - **Fields**: `fipsCode`, `equipmentType`
  
- **County Geometry**:
  - **Collection**: `GeoCounties`
  - **Query**: `findByStateAbbr(stateAbbr)`
  - **Fields**: `fips5`, `geometry`
  
- **Join**: `EquipmentDevice.fipsCode` → `GeoCounties.fips5`
- **Result**: Color counties by dominant equipment type

---

### GUI-20: Display 2024 EAVS voting regions
**Collections**: `GeoCounties`, `Eavs`
- **County Boundaries**:
  - **Collection**: `GeoCounties`
  - **Query**: `findByStateAbbr(stateAbbr)`
  - **Fields**: `fips5`, `countyName`, `geometry`
  
- **EAVS Regions**:
  - **Collection**: `Eavs`
  - **Query**: `findByStateAbbrAndYear(stateAbbr, 2024)`
  - **Fields**: `fips5`, `jurisdictionName`
  
- **Join**: `Eavs.fips5` → `GeoCounties.fips5`
- **Result**: Show EAVS jurisdiction boundaries on map

---

## Key Query Patterns

### Querying by State

**Option 1: Using stateFips (Recommended)**
```java
// Get state by FIPS code (e.g., "25" for MA)
Optional<GeoStateDoc> state = geoStateRepo.findById("25");
// or
Optional<GeoStateDoc> state = geoStateRepo.findByStateFips("25");
```

**Option 2: Using stateAbbr**
```java
// Get state by abbreviation (e.g., "MA")
Optional<GeoStateDoc> state = geoStateRepo.findByStateAbbr("MA");
```

### Querying Counties by State

```java
// Get all counties for a state
List<GeoCountyDoc> counties = geoCountyRepo.findByStateAbbr("MA");
// or if you have stateFips, first get stateAbbr, then query counties
```

### Joining Counties with EAVS Data

```java
// 1. Get counties for state
List<GeoCountyDoc> counties = geoCountyRepo.findByStateAbbr("MA");

// 2. Get EAVS data for state
List<EavsDoc> eavsData = eavsRepo.findByStateAbbrAndYear("MA", 2024);

// 3. Join in application code
Map<String, EavsDoc> eavsByFips = eavsData.stream()
    .collect(Collectors.toMap(EavsDoc::getFips5, Function.identity()));

for (GeoCountyDoc county : counties) {
    EavsDoc eavs = eavsByFips.get(county.getFips5());
    // Combine county geometry with EAVS data
}
```

---

## Frontend → Backend ID Mapping

### How Frontend Identifies States

The frontend uses **stateFips** (numeric string) to identify states:
- `"19"` = Iowa (IA)
- `"17"` = Illinois (IL)
- `"25"` = Massachusetts (MA)
- `"37"` = North Carolina (NC)
- `"53"` = Washington (WA)

### API Endpoints

**Get State by FIPS**:
```
GET /api/state/{stateFips}
```
- Uses `StateDoc` collection
- Returns state metadata

**Get State Geometry** (for maps):
```
GET /api/geostate/{stateFips}
```
- Uses `GeoStates` collection
- Returns state geometry for rendering

**Get Counties for State**:
```
GET /api/geocounty/state/{stateAbbr}
```
- Uses `GeoCounties` collection
- Returns all counties with geometry for a state

---

## Important Notes

1. **ID Changes**: 
   - `GeoStates` now uses `stateFips` as `_id` (not `stateAbbr`)
   - `FelonyPolicy` now uses `stateFips` as `_id` (not `stateAbbr`)
   - This allows consistent querying by FIPS code across collections

2. **Joining Strategy**:
   - Counties join with EAVS using `fips5` (5-digit county FIPS)
   - Counties join with CVAP using `fips5`
   - Counties join with Equipment using `fipsCode` (should match `fips5`)

3. **State Identification**:
   - Frontend primarily uses `stateFips` (numeric)
   - Backend can query by either `stateFips` or `stateAbbr`
   - `stateFips` is more consistent across data sources

4. **Detailed States**:
   - Only 5 states have detailed county-level data: IA, IL, MA, NC, WA
   - Other states show only state-level boundaries
   - Filter by `stateFips IN ["19", "17", "25", "37", "53"]` for detailed states

---

## Summary Table

| Collection | ID Field | Query By | Used In GUI |
|------------|----------|----------|-------------|
| `GeoStates` | `stateFips` | `findById(stateFips)` or `findByStateFips()` | GUI-1, GUI-2 |
| `GeoCounties` | `stateAbbr-fips5` | `findByStateAbbr(stateAbbr)` | GUI-2, GUI-5, GUI-10, GUI-20 |
| `Eavs` | `year\|stateAbbr\|fips5` | `findByStateAbbrAndYear()` | GUI-2, GUI-3, GUI-4, GUI-5, GUI-7, GUI-8, GUI-9 |
| `CvapCounty` | `stateAbbr\|fips5\|cvapCategoryCode` | `findByStateAbbrAndFips5()` | GUI-2, GUI-22 |
| `FelonyPolicy` | `stateFips` | `findById(stateFips)` or `findByStateFips()` | GUI-15, GUI-21 |

