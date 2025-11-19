# Required EAVS Data Fields by Use Case

This document lists all EAVS question fields required to support the use cases.

## Summary

Based on the use cases, you need the following EAVS fields:

### Core Registration Fields
- **A1A**: Total registered voters (denominator for many calculations)
- **A1B**: Active registered voters
- **A1C**: Inactive registered voters
- **A3A**: Same-day registration (if available)

### Pollbook Deletions
- **A12A**: Total pollbook deletions
- **A12B** through **A12H**: Categories of pollbook deletions

### Mail Ballots
- **C1A**: Mail ballots sent/requested
- **C3A**: Drop box returns (total)
- **C9A**: Total mail ballots rejected
- **C9B** through **C9Q**: Categories of mail ballot rejections

### Early Voting
- **B1A**: Total votes cast (denominator for turnout calculations)
- **B5A**: Total early voting
- **B6A**, **B6B**, **B6C**: Early voting categories
- **B24A**: UOCAVA ballots rejected

### Provisional Ballots
- **E1A**: Total provisional ballots cast
- **E1D**: Provisional ballots rejected
- **E2A** through **E2I**: Categories of provisional ballots

### Derived Fields
- **missingnessScore**: Measure of data completeness (0-1)
- **equipmentQualityScore**: Equipment quality measure (0-1)

---

## Detailed Breakdown by Use Case

### GUI-2: Display State
**Required Fields:**
- `A1A` - Total registered voters (for CVAP percentage calculation)
- `missingnessScore` - Data completeness measure

**Calculation:**
- `(A1A / CVAP) * 100` = Registration rate percentage

---

### GUI-3: Provisional ballot bar chart
**Required Fields:**
- `E2A` - Provisional ballot category 1
- `E2B` - Provisional ballot category 2
- `E2C` - Provisional ballot category 3
- `E2D` - Provisional ballot category 4
- `E2E` - Provisional ballot category 5
- `E2F` - Provisional ballot category 6
- `E2G` - Provisional ballot category 7
- `E2H` - Provisional ballot category 8
- `E2I` - Provisional ballot category 9

**Aggregation:** Sum by state for 2024

---

### GUI-4: Provisional ballot table
**Required Fields:**
- `E2A` through `E2I` - All provisional ballot categories
- `jurisdictionName` - For table rows
- `fips5` - For table rows

**Display:** One row per jurisdiction, columns for each category

---

### GUI-5: Provisional ballot choropleth map
**Required Fields:**
- `E1A` - Total provisional ballots cast

**Display:** Choropleth map colored by `E1A` values

---

### GUI-7: Display 2024 EAVS active voters
**Required Fields:**
- `A1A` - Total registered voters
- `A1B` - Active registered voters
- `A1C` - Inactive registered voters

**Calculations:**
- Bar chart: Show A1B, A1A, A1C
- Map: `(A1B / A1A) * 100` = Active voter percentage

---

### GUI-8: Display 2024 EAVS pollbook deletions
**Required Fields:**
- `A12A` - Total pollbook deletions
- `A12B` - Pollbook deletion category 1
- `A12C` - Pollbook deletion category 2
- `A12D` - Pollbook deletion category 3
- `A12E` - Pollbook deletion category 4
- `A12F` - Pollbook deletion category 5
- `A12G` - Pollbook deletion category 6
- `A12H` - Pollbook deletion category 7
- `A1A` - Total registered (denominator)

**Calculations:**
- Bar chart: Show A12B through A12H
- Map: `(sum(A12B:A12H) / A1A) * 100` = Deletion percentage

---

### GUI-9: Display mail ballots rejected
**Required Fields:**
- `C9A` - Total mail ballots rejected
- `C9B` - Mail rejection reason 1
- `C9C` - Mail rejection reason 2
- `C9D` - Mail rejection reason 3
- `C9E` - Mail rejection reason 4
- `C9F` - Mail rejection reason 5
- `C9G` - Mail rejection reason 6
- `C9H` - Mail rejection reason 7
- `C9I` - Mail rejection reason 8
- `C9J` - Mail rejection reason 9
- `C9K` - Mail rejection reason 10
- `C9L` - Mail rejection reason 11
- `C9M` - Mail rejection reason 12
- `C9N` - Mail rejection reason 13
- `C9O` - Mail rejection reason 14
- `C9P` - Mail rejection reason 15
- `C9Q` - Mail rejection reason 16

**Calculations:**
- Bar chart: Show C9B through C9Q
- Map: `(sum(C9B:C9Q) / C9A) * 100` = Rejection percentage

---

### GUI-15: Compare Republican and Democratic states
**Required Fields:**
- `C1A` - Mail ballots sent/requested
- `B5A` - Total early voting
- `C3A` - Drop box returns
- `B1A` - Total votes cast

**Calculations:**
- Mail ballot percentage: `(C1A / B5A) * 100` (or similar)
- Drop box percentage: `(C3A / total_votes) * 100`
- Turnout: `(B1A / CVAP) * 100`

---

### GUI-16: Compare changes in voter registration
**Required Fields:**
- `A1A` - Total registered voters

**Years Needed:** 2016, 2020, 2024

**Display:** Line graph showing A1A for each year, sorted by 2024 values

---

### GUI-21: Compare voter registration data for opt-in and opt-out
**Required Fields:**
- `A1A` - Total registered voters
- `A3A` - Same-day registration (if available)
- `B1A` - Total votes cast

**Calculations:**
- Registration rate: `(A1A / CVAP) * 100`
- Turnout rate: `(B1A / CVAP) * 100`
- Same-day registration: `(A3A / A1A) * 100` (if available)

---

### GUI-22: Compare Republican and Democratic states
**Required Fields:**
- `A1A` - Total registered voters
- `B1A` - Total votes cast

**Calculations:**
- Registration rate: `(A1A / CVAP) * 100`
- Turnout rate: `(B1A / CVAP) * 100`

---

### GUI-23: Compare Republican and Democratic states early voting
**Required Fields:**
- `B5A` - Total early voting
- `B6A` - Early voting category 1
- `B6B` - Early voting category 2
- `B6C` - Early voting category 3
- `B1A` - Total votes cast (denominator)

**Calculations:**
- `(B5A / B1A) * 100` = Total early voting percentage
- `(B6A / B1A) * 100` = Category 1 percentage
- `(B6B / B1A) * 100` = Category 2 percentage
- `(B6C / B1A) * 100` = Category 3 percentage

---

### GUI-24: Drop box voting bubble chart
**Required Fields:**
- `C3A` - Total drop box votes
- `B1A` - Total votes cast (denominator)

**Calculations:**
- Y-axis: `(C3A / B1A) * 100` = Drop box voting percentage
- X-axis: Republican vote share (from pres_results_2024_county)

---

### GUI-25: Bubble chart for voting equipment quality and rejected ballots
**Required Fields:**
- `C9A` - Mail ballots rejected
- `E1D` - Provisional ballots rejected
- `B24A` - UOCAVA ballots rejected
- `B5A` - Early voting (mail)
- `B6A` - Early voting (in-person)
- `B1A` - Election day votes
- `E1A` - Provisional ballots cast

**Calculations:**
- Y-axis: `((C9A + E1D + B24A) / (B5A + B6A + B1A + E1A)) * 100` = Rejection rate
- X-axis: Equipment quality score (from equipment_jurisdiction_2022)

---

## Complete Field List

### Section A: Registration
- **A1A**: Total registered voters ⭐ (critical - used as denominator)
- **A1B**: Active registered voters
- **A1C**: Inactive registered voters
- **A3A**: Same-day registration (optional)

### Section A12: Pollbook Deletions
- **A12A**: Total pollbook deletions
- **A12B**: Deletion category 1
- **A12C**: Deletion category 2
- **A12D**: Deletion category 3
- **A12E**: Deletion category 4
- **A12F**: Deletion category 5
- **A12G**: Deletion category 6
- **A12H**: Deletion category 7

### Section B: Voting
- **B1A**: Total votes cast ⭐ (critical - used as denominator)
- **B5A**: Total early voting
- **B6A**: Early voting category 1
- **B6B**: Early voting category 2
- **B6C**: Early voting category 3
- **B24A**: UOCAVA ballots rejected

### Section C: Mail Ballots
- **C1A**: Mail ballots sent/requested
- **C3A**: Drop box returns
- **C9A**: Total mail ballots rejected
- **C9B** through **C9Q**: Mail rejection reasons (16 categories)

### Section E: Provisional Ballots
- **E1A**: Total provisional ballots cast
- **E1D**: Provisional ballots rejected
- **E2A** through **E2I**: Provisional ballot categories (9 categories)

### Derived/Calculated Fields
- **missingnessScore**: Data completeness (0-1)
- **equipmentQualityScore**: Equipment quality (0-1)

---

## Years Required

- **2016**: For GUI-16 (registration trends)
- **2020**: For GUI-16 (registration trends)
- **2024**: For all other use cases (primary year)

---

## Notes

1. **A1A** and **B1A** are critical denominators used in many calculations
2. Some fields may be optional (marked with "if available")
3. The exact field names may vary slightly in the CSV (e.g., "A1a" vs "A1A")
4. All fields should be stored in the `questions` map in `eavs_all` collection
5. Missing values should be handled gracefully (null/0)

---

## Verification

To verify you have all required fields, check that your `eavs_all` collection contains documents with these fields in the `questions` map:

```javascript
// MongoDB query to check for required fields
db.eavs_all.findOne({
  year: 2024,
  stateAbbr: "MA"
}, {
  "questions.A1A": 1,
  "questions.A1B": 1,
  "questions.A1C": 1,
  "questions.B1A": 1,
  "questions.B5A": 1,
  "questions.C3A": 1,
  "questions.C9A": 1,
  "questions.E1A": 1,
  "questions.E1D": 1,
  "questions.E2A": 1,
  "questions.A12A": 1,
  "questions.A12B": 1
})
```



