# Use Cases → Required Collections Mapping

## GUI Use Cases

| Use Case | Collections Required |
|----------|---------------------|
| **GUI-1**: Display map of US on splash page | `geo_states` |
| **GUI-2**: Display State | `geo_states`, `geo_counties`, `eavs_all`, `cvap_county_2023_long` |
| **GUI-3**: Provisional ballot bar chart | `eavs_all` |
| **GUI-4**: Provisional ballot table | `eavs_all` |
| **GUI-5**: Provisional ballot choropleth map | `eavs_all`, `geo_counties` |
| **GUI-6**: State voting equipment summary | `equipment_device_2022` |
| **GUI-7**: Display 2024 EAVS active voters | `eavs_all` |
| **GUI-8**: Display 2024 EAVS pollbook deletions | `eavs_all` |
| **GUI-9**: Display mail ballots rejected | `eavs_all` |
| **GUI-10**: Display type of voting equipment | `equipment_device_2022`, `geo_counties` |
| **GUI-11**: Display relative age of voting equipment | `equipment_device_2022` |
| **GUI-12**: Display voting equipment in US | `equipment_device_2022` |
| **GUI-13**: Display of US voting equipment summary | `equipment_device_2022` |
| **GUI-14**: Display voting equipment history for a state | `equipment_device_2022` |
| **GUI-15**: Compare Republican and Democratic states | `felony_policy_2024`, `eavs_all`, `pres_results_2024_county` |
| **GUI-16**: Compare changes in voter registration | `eavs_all` |
| **GUI-17**: Display voter registration data | `wa_registration_county_age`, `wa_registration_county_gender`, `eavs_all` |
| **GUI-18**: Display voter registration bubble chart | `wa_voter` (or `wa_vrdb_precinct_demo`), `geo_counties` |
| **GUI-19**: Display registered voters | `wa_voter` |
| **GUI-20**: Display 2024 EAVS voting regions when the state is selected | `geo_counties`, `eavs_all` |
| **GUI-21**: Compare voter registration data for opt-in and opt-out | `eavs_all`, `felony_policy_2024` |
| **GUI-22**: Compare Republican and Democratic states | `eavs_all`, `cvap_county_2023_long` |
| **GUI-23**: Compare Republican and Democratic states early voting | `eavs_all` |
| **GUI-24**: Drop box voting bubble chart | `eavs_all`, `pres_results_2024_county` (or `pres_results_2024_ma_town`) |
| **GUI-25**: Bubble chart for voting equipment quality and rejected ballots | `equipment_jurisdiction_2022`, `eavs_all` |
| **GUI-26**: Bubble chart regression line | `equipment_jurisdiction_2022`, `eavs_all`, `pres_results_2024_county` |
| **GUI-27**: Display Gingles Chart | `pres_results_2024_ma_town` (or precinct-level results), `cvap_county_2023_long` |
| **GUI-28**: Ecological Inference analysis of voting equipment | `equipment_jurisdiction_2022`, `cvap_county_2023_long`, `eavs_all` |
| **GUI-29**: Ecological Inference analysis of rejected ballots | `eavs_all`, `cvap_county_2023_long` |
| **GUI-30**: Reset page | None (client-side only) |

## Preprocessing Use Cases

| Use Case | Collections Required |
|----------|---------------------|
| **Prepro-1**: Add boundary data to your DB | `geo_states`, `geo_counties` |
| **Prepro-2**: DB Design for EAVS Data | `eavs_all` |
| **Prepro-3**: Populate your DB with EAVS data | `eavs_all` |
| **Prepro-4**: Add geographic data to your DB | `geo_counties`, `geo_wa_precincts` |
| **Prepro-5**: Develop a measure of missing EAVS data | `eavs_all` |
| **Prepro-6**: Develop a measure of voting equipment quality | `equipment_device_2022`, `equipment_jurisdiction_2022` |
| **Prepro-7**: Analyze voter registration data for one state | `wa_registration_county_age`, `wa_registration_county_gender` |
| **Prepro-8**: Analyze voter registration data using an automated service | `wa_voter` |
| **Prepro-9**: Determine census block for each voter in the registration dataset | `wa_voter` |
| **Prepro-10**: Determine EAVS region for each voter in registration dataset | `wa_voter` |
| **Prepro-11**: Calculate the Republican/Democratic vote split | `pres_results_2024_county`, `pres_results_2024_ma_town` |
| **Prepro-12**: Add citizen voting age population (CVAP) to your DB | `cvap_county_2023_long` |
| **Prepro-13**: Add felony voting data to your DB | `felony_policy_2024` |

## Server Processing Use Cases

| Use Case | Collections Required |
|----------|---------------------|
| **Server-1**: Formulate DB query | All collections (via repositories) |
| **Server-2**: Generate a JSON response | All collections (via controllers) |

---

## Collection Usage Summary

### Most Frequently Used Collections

1. **`eavs_all`** - Used in 20+ GUI use cases
   - GUI-2, GUI-3, GUI-4, GUI-5, GUI-7, GUI-8, GUI-9, GUI-15, GUI-16, GUI-17, GUI-20, GUI-21, GUI-22, GUI-23, GUI-24, GUI-25, GUI-26, GUI-28, GUI-29

2. **`geo_counties`** - Used in 8 GUI use cases
   - GUI-2, GUI-5, GUI-10, GUI-18, GUI-20

3. **`equipment_device_2022`** - Used in 6 GUI use cases
   - GUI-6, GUI-10, GUI-11, GUI-12, GUI-13, GUI-14

4. **`equipment_jurisdiction_2022`** - Used in 3 GUI use cases
   - GUI-25, GUI-26, GUI-28

5. **`cvap_county_2023_long`** - Used in 5 GUI use cases
   - GUI-2, GUI-22, GUI-27, GUI-28, GUI-29

6. **`pres_results_2024_county`** / **`pres_results_2024_ma_town`** - Used in 4 GUI use cases
   - GUI-15, GUI-24, GUI-26, GUI-27

7. **`wa_registration_county_age`** / **`wa_registration_county_gender`** - Used in 1 GUI use case
   - GUI-17

8. **`wa_voter`** - Used in 2 GUI use cases
   - GUI-18, GUI-19

9. **`felony_policy_2024`** - Used in 2 GUI use cases
   - GUI-15, GUI-21

10. **`geo_states`** - Used in 2 GUI use cases
    - GUI-1, GUI-2

11. **`geo_wa_precincts`** - Used in preprocessing
    - Prepro-4

---

## Complete Collection List (Alphabetical)

1. `cvap_county_2023_long`
2. `eavs_all`
3. `equipment_device_2022`
4. `equipment_jurisdiction_2022`
5. `felony_policy_2024`
6. `geo_counties`
7. `geo_states`
8. `geo_wa_precincts`
9. `pres_results_2024_county`
10. `pres_results_2024_ma_town`
11. `wa_participation_2024_county_age`
12. `wa_registration_county_age`
13. `wa_registration_county_gender`
14. `wa_voter`
15. `wa_vrdb_county_age_demo`
16. `wa_vrdb_precinct_demo`

---

## Notes

- **`wa_voter_history`**: Large file, currently disabled. May be needed for historical analysis.
- **Census blocks**: Not explicitly listed but may be needed for GUI-18 (bubble chart by census block).
- Some use cases may require additional collections not yet identified (e.g., historical equipment data for GUI-14).

