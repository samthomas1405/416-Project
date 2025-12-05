"""
Build data for:
    GUI-25 – equipment quality vs rejected ballots (bubble chart)
    GUI-26 – regression lines (quadratic per majority party)

Outputs:
    data_clean/gui/gui25_equipment_rejection_bubbles_2024.csv
    data_clean/gui/gui26_equipment_rejection_regression_2024.csv

Bubble columns:
    state_abbr
    fips5
    county_name
    avg_quality_score    (x-axis)
    rejection_rate       (y-axis)
    total_ballots
    total_rejected
    majority_party       ("DEM", "REP", "TIE", "UNK")
    bubble_size          (total_ballots)

Regression columns:
    party   ("DEM", "REP")
    a, b, c  for y = a + b*x + c*x^2
"""

from __future__ import annotations

from typing import List, Dict
from pathlib import Path
import json

import numpy as np
import pandas as pd

from data.preprocess.utils.paths import CLEAN

CLEAN_EAVS = CLEAN / "eavs"
CLEAN_EQUIP = CLEAN / "equipment"
CLEAN_RESULTS = CLEAN / "results_2024"
CLEAN_GEO = CLEAN / "geo"
CLEAN_GUI = CLEAN / "gui"
CLEAN_GUI.mkdir(exist_ok=True)

DETAILED_STATES = ["IA", "IL", "MA", "NC", "WA"]


# ---------------------- helpers ---------------------- #

def _load_eavs_2024() -> pd.DataFrame:
    path = CLEAN_EAVS / "eavs_2016_2024_normalized.csv"
    df = pd.read_csv(path, low_memory=False)
    df = df[df["year"] == 2024].copy()
    df["fips5"] = df["fips5"].astype(str).str.zfill(5)
    return df


def _build_eavs_rejection_summary_2024() -> pd.DataFrame:
    df = _load_eavs_2024()

    # Total ballots counted
    ballot_cols = ["F1a", "F1b", "F1c", "E1c"]
    existing_ballot = [c for c in ballot_cols if c in df.columns]
    for c in existing_ballot:
        df[c] = pd.to_numeric(df[c], errors="coerce")
        df.loc[df[c] < 0, c] = 0  # handle sentinel codes
        df[c] = df[c].fillna(0)
    df["total_ballots"] = df[existing_ballot].sum(axis=1)

    # Mail rejections – C9a if present, else sum of all C9* fields
    c9_cols = [c for c in df.columns if c.startswith("C9")]
    if "C9a" in df.columns:
        df["mail_rejected"] = pd.to_numeric(df["C9a"], errors="coerce")
        df.loc[df["mail_rejected"] < 0, "mail_rejected"] = 0
        df["mail_rejected"] = df["mail_rejected"].fillna(0)
    elif c9_cols:
        for c in c9_cols:
            df[c] = pd.to_numeric(df[c], errors="coerce")
            df.loc[df[c] < 0, c] = 0
            df[c] = df[c].fillna(0)
        df["mail_rejected"] = df[c9_cols].sum(axis=1)
    else:
        df["mail_rejected"] = 0

    # Provisional not counted (E1d) and UOCAVA uncounted (B24a)
    for col in ["E1d", "B24a"]:
        if col in df.columns:
            df[col] = pd.to_numeric(df[col], errors="coerce")
            df.loc[df[col] < 0, col] = 0
            df[col] = df[col].fillna(0)
        else:
            df[col] = 0

    df["total_rejected"] = df["mail_rejected"] + df["E1d"] + df["B24a"]

    grouped = (
        df.groupby(["state_abbr", "fips5"], as_index=False)[["total_ballots", "total_rejected"]]
          .sum()
    )
    grouped["rejection_rate"] = grouped["total_rejected"] / grouped["total_ballots"].replace(0, pd.NA)
    return grouped


def _load_equipment_quality_by_county() -> pd.DataFrame:
    """
    Convert jurisdiction-level equipment quality to county-level by:
      - FIPS Code: 10-digit like 1900100000 -> fips5 = 19001
      - Average avg_quality_score within each (state_abbr, fips5)
    """
    path = CLEAN_EQUIP / "equipment_quality_by_jurisdiction_2022.csv"
    eq = pd.read_csv(path)

    # Convert FIPS Code (e.g. 1900100000) to 5-digit county FIPS
    fips_numeric = pd.to_numeric(eq["FIPS Code"], errors="coerce")
    # floor-divide by 100000 to drop the trailing local-jurisdiction digits
    eq["fips5"] = (fips_numeric // 100000).astype("Int64").astype(str).str.zfill(5)

    grouped = (
        eq.groupby(["state_abbr", "fips5"], as_index=False)["avg_quality_score"]
          .mean()
    )
    return grouped


def _load_geo_map() -> pd.DataFrame:
    """
    Map (state_abbr, fips5) -> county_name, plus stripped version to join to results.
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
    df["county_name_stripped"] = df["county_name_geo"].str.replace(r"\s+County$", "", regex=True)
    return df


def _load_pres_results_by_county_with_fips5(geo_map: pd.DataFrame) -> pd.DataFrame:
    """
    Attach fips5 to pres_2024_by_county via stripped county names, as in GUI-24.
    """
    path = CLEAN_RESULTS / "pres_2024_by_county.csv"
    res = pd.read_csv(path)

    merged = res.merge(
        geo_map[["state_abbr", "county_name_stripped", "fips5"]],
        left_on=["state_abbr", "county_name"],
        right_on=["state_abbr", "county_name_stripped"],
        how="left",
    )
    merged["fips5"] = merged["fips5"].astype(str).str.zfill(5)
    return merged


def _add_majority_party(df: pd.DataFrame) -> pd.DataFrame:
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

    df["majority_party"] = df.apply(majority, axis=1)
    df = df.rename(
        columns={
            "dem_share_2024_pres": "dem_share",
            "rep_share_2024_pres": "rep_share",
        }
    )
    return df


def build_bubbles() -> pd.DataFrame:
    eavs_sum = _build_eavs_rejection_summary_2024()
    quality = _load_equipment_quality_by_county()
    geo_map = _load_geo_map()
    res = _load_pres_results_by_county_with_fips5(geo_map)

    # Merge EAVS summary with quality scores
    bubbles = eavs_sum.merge(quality, on=["state_abbr", "fips5"], how="inner")

    # Attach county_name from geo
    bubbles = bubbles.merge(
        geo_map[["state_abbr", "fips5", "county_name_geo"]],
        on=["state_abbr", "fips5"],
        how="left",
    ).rename(columns={"county_name_geo": "county_name"})

    # Attach results
    bubbles = bubbles.merge(
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

    # Only detailed states
    bubbles = bubbles[bubbles["state_abbr"].isin(DETAILED_STATES)].copy()

    # Majority party + bubble size
    bubbles = _add_majority_party(bubbles)
    bubbles["bubble_size"] = bubbles["total_ballots"]

    out = bubbles[
        [
            "state_abbr",
            "fips5",
            "county_name",
            "avg_quality_score",
            "rejection_rate",
            "total_ballots",
            "total_rejected",
            "majority_party",
            "bubble_size",
        ]
    ].copy()

    return out


def fit_quadratic_regression(df: pd.DataFrame, party: str) -> Dict[str, float]:
    """
    Fit y = a + b*x + c*x^2 for DEM or REP, using numeric avg_quality_score and rejection_rate.

    Returns dict with 'party', 'a', 'b', 'c'. If there are fewer than 3 usable points,
    returns NaNs for the coefficients.
    """
    # Filter by party
    sub = df[df["majority_party"] == party].copy()

    # Force numeric types and drop anything that can't be coerced
    sub["avg_quality_score"] = pd.to_numeric(sub["avg_quality_score"], errors="coerce")
    sub["rejection_rate"] = pd.to_numeric(sub["rejection_rate"], errors="coerce")

    sub = sub[sub["avg_quality_score"].notna() & sub["rejection_rate"].notna()]

    # Need at least 3 points to fit a quadratic
    if len(sub) < 3:
        return {"party": party, "a": np.nan, "b": np.nan, "c": np.nan}

    # Convert explicitly to float arrays for np.polyfit
    x = sub["avg_quality_score"].to_numpy(dtype=float)
    y = sub["rejection_rate"].to_numpy(dtype=float)

    # polyfit returns [c, b, a] for degree 2
    c, b, a = np.polyfit(x, y, deg=2)
    return {"party": party, "a": a, "b": b, "c": c}


# ---------------------- main ---------------------- #

def main() -> None:
    bubbles = build_bubbles()

    out_bubbles = CLEAN_GUI / "gui25_equipment_rejection_bubbles_2024.csv"
    bubbles.to_csv(out_bubbles, index=False)
    print(f"Wrote GUI-25 bubble data to {out_bubbles}")

    # Regression coefficients for GUI-26
    regs = [fit_quadratic_regression(bubbles, party) for party in ["DEM", "REP"]]
    reg_df = pd.DataFrame(regs)
    out_regs = CLEAN_GUI / "gui26_equipment_rejection_regression_2024.csv"
    reg_df.to_csv(out_regs, index=False)
    print(f"Wrote GUI-26 regression data to {out_regs}")


if __name__ == "__main__":
    main()