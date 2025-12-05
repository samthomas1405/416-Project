"""
Build bubble-chart data for GUI-24 (drop box usage vs partisanship) for 2024.

Output:
    data_clean/gui/gui24_dropbox_bubbles_2024.csv

Columns:
    state_abbr
    fips5
    county_name
    rep_share       (x-axis)
    dem_share
    majority_party  ("DEM", "REP", "TIE", "UNK")
    dropbox_share   (y-axis)
    bubble_size     (total presidential votes)

Designed for your two Political Party states (IA & WA).
"""

from __future__ import annotations

from typing import List, Dict
from pathlib import Path
import json

import pandas as pd

from data.preprocess.utils.paths import CLEAN

CLEAN_EAVS = CLEAN / "eavs"
CLEAN_RESULTS = CLEAN / "results_2024"
CLEAN_GEO = CLEAN / "geo"
CLEAN_GUI = CLEAN / "gui"
CLEAN_GUI.mkdir(exist_ok=True)

POLITICAL_PARTY_STATES = ["IA", "WA"]


# ---------------------- helpers ---------------------- #

def _load_eavs_2024() -> pd.DataFrame:
    path = CLEAN_EAVS / "eavs_2016_2024_normalized.csv"
    df = pd.read_csv(path, low_memory=False)
    df = df[df["year"] == 2024].copy()
    df["fips5"] = df["fips5"].astype(str).str.zfill(5)
    return df


def _build_eavs_county_summary_2024() -> pd.DataFrame:
    df = _load_eavs_2024()

    # total ballots: absentee + early in-person + election day + provisional counted
    ballot_cols = ["F1a", "F1b", "F1c", "E1c"]
    existing_ballot = [c for c in ballot_cols if c in df.columns]
    for c in existing_ballot:
        df[c] = pd.to_numeric(df[c], errors="coerce")
        # EAVS sentinel (e.g., -99) -> 0
        df.loc[df[c] < 0, c] = 0
        df[c] = df[c].fillna(0)
    df["total_ballots"] = df[existing_ballot].sum(axis=1)

    # drop-box returns (C3a), with sentinel handling
    if "C3a" in df.columns:
        df["dropbox_ballots"] = pd.to_numeric(df["C3a"], errors="coerce")
        df.loc[df["dropbox_ballots"] < 0, "dropbox_ballots"] = 0
        df["dropbox_ballots"] = df["dropbox_ballots"].fillna(0)
    else:
        df["dropbox_ballots"] = 0

    grouped = (
        df.groupby(["state_abbr", "fips5"], as_index=False)[["total_ballots", "dropbox_ballots"]]
          .sum()
    )
    grouped["dropbox_share"] = grouped["dropbox_ballots"] / grouped["total_ballots"].replace(0, pd.NA)
    return grouped


def _load_county_geo_map() -> pd.DataFrame:
    """
    Map (state_abbr, fips5) -> county_name, plus a 'stripped' version
    for joining with results (e.g., 'Adams County' -> 'Adams').
    """
    geo_path = CLEAN_GEO / "us_counties_selected.geojson"
    with open(geo_path, "r") as f:
        geo = json.load(f)

    rows: List[Dict[str, str]] = []
    for feat in geo["features"]:
        props = feat["properties"]
        rows.append(
            {
                "state_abbr": props["state_abbr"],
                "fips5": str(props["fips5"]).zfill(5),
                "county_name_geo": props["county_name"],
            }
        )
    df = pd.DataFrame(rows)
    # stripped version: remove a trailing " County" if present
    df["county_name_stripped"] = df["county_name_geo"].str.replace(r"\s+County$", "", regex=True)
    return df


def _load_pres_results_by_county_with_fips5(geo_map: pd.DataFrame) -> pd.DataFrame:
    """
    Load pres_2024_by_county.csv and attach fips5 by joining to the geo map.

    - For IA, county names already match (e.g. 'Adair').
    - For WA, results use 'Adams', geo uses 'Adams County', so we join on
      `county_name_stripped`.
    """
    path = CLEAN_RESULTS / "pres_2024_by_county.csv"
    res = pd.read_csv(path)

    # Join to geo map on stripped county names
    merged = res.merge(
        geo_map[["state_abbr", "county_name_stripped", "fips5"]],
        left_on=["state_abbr", "county_name"],
        right_on=["state_abbr", "county_name_stripped"],
        how="left",
    )

    # Sanity: ensure we do have fips5 for IA & WA
    # (if some were missing, they'd show up as NaN here)
    merged["fips5"] = merged["fips5"].astype(str).str.zfill(5)

    return merged


def build_dropbox_bubbles() -> pd.DataFrame:
    eavs_county = _build_eavs_county_summary_2024()
    geo_map = _load_county_geo_map()
    res = _load_pres_results_by_county_with_fips5(geo_map)

    # attach official county_name from geo_map to the EAVS summary
    eavs_geo = eavs_county.merge(
        geo_map[["state_abbr", "fips5", "county_name_geo"]],
        on=["state_abbr", "fips5"],
        how="left",
    ).rename(columns={"county_name_geo": "county_name"})

    # merge in presidential results via fips5
    merged = eavs_geo.merge(
        res[
            [
                "state_abbr",
                "fips5",
                "total_votes_2024_pres",
                "dem_share_2024_pres",
                "rep_share_2024_pres",
            ]
        ],
        on=["state_abbr", "fips5"],
        how="left",
    )

    # Filter to Political Party states
    merged = merged[merged["state_abbr"].isin(POLITICAL_PARTY_STATES)].copy()

    # majority party
    def majority_party(row) -> str:
        dem = row["dem_share_2024_pres"]
        rep = row["rep_share_2024_pres"]
        if pd.isna(dem) or pd.isna(rep):
            return "UNK"
        if rep > dem:
            return "REP"
        if dem > rep:
            return "DEM"
        return "TIE"

    merged["majority_party"] = merged.apply(majority_party, axis=1)
    merged = merged.rename(
        columns={
            "dem_share_2024_pres": "dem_share",
            "rep_share_2024_pres": "rep_share",
        }
    )
    merged["bubble_size"] = merged["total_votes_2024_pres"]

    bubbles = merged[
        [
            "state_abbr",
            "fips5",
            "county_name",
            "rep_share",
            "dem_share",
            "majority_party",
            "dropbox_share",
            "bubble_size",
        ]
    ].copy()

    return bubbles


# ---------------------- main ---------------------- #

def main() -> None:
    bubbles = build_dropbox_bubbles()
    out_path = CLEAN_GUI / "gui24_dropbox_bubbles_2024.csv"
    bubbles.to_csv(out_path, index=False)
    print(f"Wrote GUI-24 bubble data to {out_path}")


if __name__ == "__main__":
    main()