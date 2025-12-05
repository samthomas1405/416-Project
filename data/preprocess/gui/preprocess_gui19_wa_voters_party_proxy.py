"""
Build an aggregated voter-level table for GUI-19, attaching an approximate
"party" field to each WA county based on 2024 presidential election results.

We DO NOT have party registration.
Instead, we:
  - Use county-level 2024 presidential results for WA
  - Compute a county_majority_party ("DEM", "REP", "TIE", "UNK")
  - Aggregate WA VRDB voters by county + demographics + status

Inputs:
    data_clean/registration/wa_vrdb_voters.csv
        (one row per voter, no names)

    data_clean/results_2024/pres_2024_by_county.csv
        (county-level presidential results for IA + WA)

Output:
    data_clean/gui/gui19_wa_voters_party_aggregated.csv

Columns:
    county_code
    county_name
    county_majority_party   ("DEM", "REP", "TIE", "UNK")
    age_group_2024
    gender
    status_code
    voter_count

This is compact and GUI-friendly. GUI-19 can:
    - Filter by county (code/name)
    - Filter by county_majority_party
    - Show distributions by age_group_2024, gender, status_code
"""

from __future__ import annotations

from pathlib import Path
from typing import Dict, List

import pandas as pd

from data.preprocess.utils.paths import CLEAN

CLEAN_REG = CLEAN / "registration"
CLEAN_RESULTS_2024 = CLEAN / "results_2024"
CLEAN_GUI = CLEAN / "gui"
CLEAN_GUI.mkdir(parents=True, exist_ok=True)


def _load_wa_voters() -> pd.DataFrame:
    """
    Load the cleaned WA VRDB voters file.

    Actual columns (from your data_clean):
        ['state_voter_id',
         'birthyear',
         'age_2024',
         'age_group_2024',
         'gender',
         'county_code',
         'county_name',
         'precinct_code',
         'precinct_part',
         'legislative_district',
         'congressional_district',
         'registration_date',
         'last_voted',
         'status_code']
    """
    path = CLEAN_REG / "wa_vrdb_voters.csv"
    df = pd.read_csv(path, low_memory=False)

    required = {
        "county_code",
        "county_name",
        "age_group_2024",
        "gender",
        "status_code",
    }
    missing = required.difference(df.columns)
    if missing:
        raise RuntimeError(f"[GUI-19] wa_vrdb_voters.csv is missing columns: {missing}")

    return df


def _load_wa_pres_results_by_county() -> pd.DataFrame:
    """
    Load county-level 2024 presidential results and restrict to WA only.

    Expected columns:
        state_abbr, county_name, dem_share_2024_pres, rep_share_2024_pres
    """
    path = CLEAN_RESULTS_2024 / "pres_2024_by_county.csv"
    res = pd.read_csv(path, low_memory=False)

    required = {"state_abbr", "county_name", "dem_share_2024_pres", "rep_share_2024_pres"}
    missing = required.difference(res.columns)
    if missing:
        raise RuntimeError(f"[GUI-19] pres_2024_by_county.csv is missing columns: {missing}")

    wa = res[res["state_abbr"] == "WA"].copy()
    return wa


def _compute_majority_party(df: pd.DataFrame) -> pd.DataFrame:
    """
    Given dem_share_2024_pres and rep_share_2024_pres, add county_majority_party.
    """

    def majority(row) -> str:
        dem = row.get("dem_share_2024_pres")
        rep = row.get("rep_share_2024_pres")
        if pd.isna(dem) or pd.isna(rep):
            return "UNK"
        if rep > dem:
            return "REP"
        if dem > rep:
            return "DEM"
        return "TIE"

    df["county_majority_party"] = df.apply(majority, axis=1)
    return df


def build_voters_aggregated() -> pd.DataFrame:
    voters = _load_wa_voters()
    wa_res = _load_wa_pres_results_by_county().copy()

    # Normalize county names for robust join
    voters["county_name_norm"] = (
        voters["county_name"].astype(str).str.strip().str.lower()
    )
    wa_res["county_name_norm"] = (
        wa_res["county_name"].astype(str).str.strip().str.lower()
    )

    # Attach presidential shares to each county
    county_with_party = wa_res[["county_name_norm", "dem_share_2024_pres", "rep_share_2024_pres"]].copy()
    county_with_party = _compute_majority_party(county_with_party)

    # Merge voters with county-level party info
    merged = voters.merge(
        county_with_party[["county_name_norm", "county_majority_party"]],
        on="county_name_norm",
        how="left",
    )

    # Aggregate by county + demographics + status
    group_cols = [
        "county_code",
        "county_name",
        "county_majority_party",
        "age_group_2024",
        "gender",
        "status_code",
    ]
    for col in group_cols:
        if col not in merged.columns:
            raise RuntimeError(f"[GUI-19] expected column missing after merge: {col}")

    agg = (
        merged.groupby(group_cols, dropna=False)
              .size()
              .reset_index(name="voter_count")
    )

    return agg


def main() -> None:
    df = build_voters_aggregated()
    out_path = CLEAN_GUI / "gui19_wa_voters_party_aggregated.csv"
    df.to_csv(out_path, index=False)
    print(f"[GUI-19] wrote aggregated voter party data -> {out_path} ({len(df)} rows)")


if __name__ == "__main__":
    main()