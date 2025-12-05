from __future__ import annotations

from pathlib import Path

import pandas as pd

from data.preprocess.utils.paths import RAW, CLEAN

RAW_CVAP = RAW / "cvap" / "2023" / "CVAP_2023"
CLEAN_CVAP = CLEAN / "cvap"
CLEAN_CVAP.mkdir(parents=True, exist_ok=True)

# For this project we only need Iowa (19) and Massachusetts (25)
STATE_FIPS_TO_ABBR = {
    "19": "IA",
    "25": "MA",
}


def preprocess_cvap_2023_county_long() -> pd.DataFrame:
    """
    Clean the county-level CVAP 2019-2023 estimates.

    Input:
        data/raw/cvap/2023/CVAP_2023/County.csv

    Output:
        data_clean/cvap/cvap_2019_2023_county_long.csv
    """
    src = RAW_CVAP / "County.csv"
    df = pd.read_csv(src, encoding="latin1")

    # geoid: "0500000US01001" -> last 5 chars are county FIPS.
    df["fips5"] = df["geoid"].astype(str).str[-5:]
    df["state_fips"] = df["fips5"].str[:2]

    # Restrict to IA + MA (your states using CVAP)
    df = df[df["state_fips"].isin(STATE_FIPS_TO_ABBR.keys())].copy()
    df["state_abbr"] = df["state_fips"].map(STATE_FIPS_TO_ABBR)

    # "Autauga County, Alabama" -> county + state text
    parts = df["geoname"].str.split(",", n=1, expand=True)
    df["county_name"] = parts[0].str.strip()
    df["state_name"] = parts[1].str.strip() if parts.shape[1] > 1 else pd.NA

    # Ensure numeric
    for col in [
        "tot_est",
        "tot_moe",
        "adu_est",
        "adu_moe",
        "cit_est",
        "cit_moe",
        "cvap_est",
        "cvap_moe",
    ]:
        if col in df.columns:
            df[col] = pd.to_numeric(df[col], errors="coerce")

    # Clearer names
    df = df.rename(
        columns={
            "lntitle": "cvap_category",
            "lnnumber": "cvap_category_code",
            "tot_est": "total_population_est",
            "adu_est": "adult_population_est",
            "cit_est": "citizen_population_est",
            "cvap_est": "cvap_estimate",
            "tot_moe": "total_population_moe",
            "adu_moe": "adult_population_moe",
            "cit_moe": "citizen_population_moe",
            "cvap_moe": "cvap_moe",
        }
    )

    out_cols = [
        "state_abbr",
        "state_fips",
        "state_name",
        "fips5",
        "county_name",
        "geoid",
        "cvap_category_code",
        "cvap_category",
        "total_population_est",
        "adult_population_est",
        "citizen_population_est",
        "cvap_estimate",
        "total_population_moe",
        "adult_population_moe",
        "citizen_population_moe",
        "cvap_moe",
    ]
    out_cols = [c for c in out_cols if c in df.columns]

    out = df[out_cols].copy().sort_values(["state_abbr", "fips5", "cvap_category_code"])

    dest = CLEAN_CVAP / "cvap_2019_2023_county_long.csv"
    out.to_csv(dest, index=False)
    print(f"[CVAP] wrote {dest} ({len(out)} rows)")

    return out


def main():
    preprocess_cvap_2023_county_long()


if __name__ == "__main__":
    main()