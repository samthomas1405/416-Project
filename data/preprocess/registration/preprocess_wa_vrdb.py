from __future__ import annotations

import datetime as _dt
from pathlib import Path
from typing import Dict, List, Tuple

import pandas as pd

from data.preprocess.utils.paths import RAW, CLEAN

RAW_WA_REG = RAW / "registration" / "wa"
CLEAN_REG = CLEAN / "registration"
CLEAN_REG.mkdir(parents=True, exist_ok=True)

# Raw input files (place these under data/raw/registration/wa/)
VRDB_FILE = RAW_WA_REG / "20251103_VRDB_Extract.txt"
DISTRICTS_FILE = RAW_WA_REG / "2025-11-03_Districts-Precincts.xlsx"
HIST_2023_2024_FILE = RAW_WA_REG / "2023-2024 Voting History Extract.txt"
HIST_2025_2026_FILE = RAW_WA_REG / "2025-2026_Voting_History_update..txt"

# Reference year for computing age from Birthyear
REF_YEAR_FOR_AGE = 2024


def _load_county_mapping() -> Dict[str, str]:
    """
    Build a CountyCode -> CountyName mapping from the Districts-Precincts file.
    """
    df = pd.read_excel(DISTRICTS_FILE)
    mapping = (
        df[["CountyCode", "County"]]
        .dropna()
        .drop_duplicates()
        .set_index("CountyCode")["County"]
        .to_dict()
    )
    return mapping


def _compute_age_and_group(birthyear_series: pd.Series) -> Tuple[pd.Series, pd.Series]:
    """
    Given a birthyear series (string or numeric), compute:
      - age_2024
      - age_group_2024 using WA official bins:
        18-24, 25-34, 35-44, 45-54, 55-64, 65 and over, <18, Unknown
    """
    by = pd.to_numeric(birthyear_series, errors="coerce")

# Reasonable adult bounds in 2024 (16–120 yrs)
    mask_valid = (by >= 1904) & (by <= 2008)

# Start with float NaNs, then fill valid ages
    age = pd.Series(float("nan"), index=by.index, dtype="float")
    age.loc[mask_valid] = REF_YEAR_FOR_AGE - by.loc[mask_valid]

    def classify(a):
        if pd.isna(a):
            return "Unknown"
        a = int(a)
        if a < 18:
            return "<18"
        if 18 <= a <= 24:
            return "18-24"
        if 25 <= a <= 34:
            return "25-34"
        if 35 <= a <= 44:
            return "35-44"
        if 45 <= a <= 54:
            return "45-54"
        if 55 <= a <= 64:
            return "55-64"
        if a >= 65:
            return "65 and over"
        return "Unknown"

    age_group = age.map(classify)
    return age, age_group


def preprocess_wa_vrdb_voters_and_precinct_summary() -> Tuple[str, str, str]:
    """
    Stream the WA VRDB extract and produce:

      1) data_clean/registration/wa_vrdb_voters.csv
         - one row per voter (minimal fields, no names)

      2) data_clean/registration/wa_vrdb_precinct_age_gender_summary_2024.csv
         - one row per (county, precinct, age_group, gender, status_code)

      3) data_clean/registration/wa_vrdb_county_age_summary_2024.csv
         - one row per (county, age_group), summing over gender + status

    Returns:
        tuple of output file paths (as strings).
    """
    county_map = _load_county_mapping()

    # Output paths
    voters_out_path = CLEAN_REG / "wa_vrdb_voters.csv"
    precinct_age_gender_out_path = CLEAN_REG / "wa_vrdb_precinct_age_gender_summary_2024.csv"
    county_age_out_path = CLEAN_REG / "wa_vrdb_county_age_summary_2024.csv"

    # Clean any existing outputs
    for p in [voters_out_path, precinct_age_gender_out_path, county_age_out_path]:
        if p.exists():
            p.unlink()

    precinct_agg_frames: List[pd.DataFrame] = []

    chunk_iter = pd.read_csv(
        VRDB_FILE,
        sep="|",
        dtype=str,
        encoding="latin1",
        chunksize=250_000,
    )

    first_chunk = True

    for chunk_idx, chunk in enumerate(chunk_iter, start=1):
        df = chunk.copy()

        # Keep only fields we need downstream (no names / street)
        keep_cols = [
            "StateVoterID",
            "Birthyear",
            "Gender",
            "CountyCode",
            "PrecinctCode",
            "PrecinctPart",
            "LegislativeDistrict",
            "CongressionalDistrict",
            "Registrationdate",
            "LastVoted",
            "StatusCode",
        ]
        df = df[keep_cols]

        # Normalize simple strings
        for col in ["Gender", "CountyCode", "StatusCode"]:
            df[col] = df[col].astype(str).str.strip()

        # County names
        df["CountyName"] = df["CountyCode"].map(county_map)

        # Age + age group
        age, age_group = _compute_age_and_group(df["Birthyear"])
        df["Age_2024"] = age
        df["AgeGroup_2024"] = age_group

        # Normalize gender values
        def norm_gender(g):
            g = (g or "").strip().upper()
            if g in ("M", "MALE"):
                return "M"
            if g in ("F", "FEMALE"):
                return "F"
            return "Unknown"

        df["GenderNorm"] = df["Gender"].map(norm_gender)

        # Normalize status values
        def norm_status(s):
            s = (s or "").strip()
            return s if s else "Unknown"

        df["StatusNorm"] = df["StatusCode"].map(norm_status)

        # Per-voter cleaned output
        voters_clean = df.rename(
            columns={
                "StateVoterID": "state_voter_id",
                "Birthyear": "birthyear",
                "GenderNorm": "gender",
                "CountyCode": "county_code",
                "CountyName": "county_name",
                "PrecinctCode": "precinct_code",
                "PrecinctPart": "precinct_part",
                "LegislativeDistrict": "legislative_district",
                "CongressionalDistrict": "congressional_district",
                "Registrationdate": "registration_date",
                "LastVoted": "last_voted",
                "StatusNorm": "status_code",
                "Age_2024": "age_2024",
                "AgeGroup_2024": "age_group_2024",
            }
        )[
            [
                "state_voter_id",
                "birthyear",
                "age_2024",
                "age_group_2024",
                "gender",
                "county_code",
                "county_name",
                "precinct_code",
                "precinct_part",
                "legislative_district",
                "congressional_district",
                "registration_date",
                "last_voted",
                "status_code",
            ]
        ]

        # Stream-write per-voter CSV
        if first_chunk:
            voters_clean.to_csv(voters_out_path, index=False, mode="w")
            first_chunk = False
        else:
            voters_clean.to_csv(voters_out_path, index=False, mode="a", header=False)

        # Precinct-level aggregate for this chunk
        agg = (
            df.groupby(
                [
                    "CountyCode",
                    "CountyName",
                    "PrecinctCode",
                    "PrecinctPart",
                    "LegislativeDistrict",
                    "CongressionalDistrict",
                    "AgeGroup_2024",
                    "GenderNorm",
                    "StatusNorm",
                ],
                dropna=False,
            )["StateVoterID"]
            .size()
            .reset_index(name="registered_voters")
        )
        precinct_agg_frames.append(agg)

        print(f"[WA VRDB] processed chunk {chunk_idx} ({len(df)} rows)")

    # Combine all precinct-level aggregates
    if precinct_agg_frames:
        agg_all = pd.concat(precinct_agg_frames, ignore_index=True)
        agg_all = (
            agg_all.groupby(
                [
                    "CountyCode",
                    "CountyName",
                    "PrecinctCode",
                    "PrecinctPart",
                    "LegislativeDistrict",
                    "CongressionalDistrict",
                    "AgeGroup_2024",
                    "GenderNorm",
                    "StatusNorm",
                ],
                as_index=False,
            )["registered_voters"]
            .sum()
        )
    else:
        agg_all = pd.DataFrame(
            columns=[
                "CountyCode",
                "CountyName",
                "PrecinctCode",
                "PrecinctPart",
                "LegislativeDistrict",
                "CongressionalDistrict",
                "AgeGroup_2024",
                "GenderNorm",
                "StatusNorm",
                "registered_voters",
            ]
        )

    # Write precinct age/gender summary
    precinct_clean = agg_all.rename(
        columns={
            "CountyCode": "county_code",
            "CountyName": "county_name",
            "PrecinctCode": "precinct_code",
            "PrecinctPart": "precinct_part",
            "LegislativeDistrict": "legislative_district",
            "CongressionalDistrict": "congressional_district",
            "AgeGroup_2024": "age_group_2024",
            "GenderNorm": "gender",
            "StatusNorm": "status_code",
        }
    )
    precinct_clean.insert(0, "state_abbr", "WA")
    precinct_clean.to_csv(precinct_age_gender_out_path, index=False)

    # County-level age summary
    county_age = (
        precinct_clean.groupby(
            ["state_abbr", "county_code", "county_name", "age_group_2024"],
            as_index=False,
        )["registered_voters"]
        .sum()
    )
    county_age.to_csv(county_age_out_path, index=False)

    print(f"[WA VRDB] wrote voters -> {voters_out_path}")
    print(f"[WA VRDB] wrote precinct age/gender summary -> {precinct_age_gender_out_path}")
    print(f"[WA VRDB] wrote county age summary -> {county_age_out_path}")

    return (
        str(voters_out_path),
        str(precinct_age_gender_out_path),
        str(county_age_out_path),
    )


def preprocess_wa_vrdb_history() -> str:
    """
    Clean and combine WA voting history from:

        - 2023-2024 Voting History Extract.txt
        - 2025-2026_Voting_History_update..txt

    Output:
        data_clean/registration/wa_vrdb_voter_history.csv
    """
    frames: List[pd.DataFrame] = []

    for path in [HIST_2023_2024_FILE, HIST_2025_2026_FILE]:
        df = pd.read_csv(path, sep="|", dtype=str, encoding="latin1")
        frames.append(df)

    if not frames:
        hist_all = pd.DataFrame(
            columns=[
                "VoterHistoryID",
                "CountyCode",
                "CountyCode_Voting",
                "StateVoterID",
                "ElectionDate",
            ]
        )
    else:
        hist_all = pd.concat(frames, ignore_index=True)

    # Drop exact duplicates
    hist_all = hist_all.drop_duplicates()

    # Parse election date
    hist_all["ElectionDate"] = pd.to_datetime(hist_all["ElectionDate"], errors="coerce")

    hist_clean = hist_all.rename(
        columns={
            "VoterHistoryID": "voter_history_id",
            "CountyCode": "county_code",
            "CountyCode_Voting": "county_code_voting",
            "StateVoterID": "state_voter_id",
            "ElectionDate": "election_date",
        }
    )
    hist_clean["state_abbr"] = "WA"
    hist_clean["election_year"] = hist_clean["election_date"].dt.year
    hist_clean["election_date_str"] = hist_clean["election_date"].dt.strftime("%Y-%m-%d")

    cols = [
        "state_abbr",
        "voter_history_id",
        "state_voter_id",
        "county_code",
        "county_code_voting",
        "election_date",
        "election_date_str",
        "election_year",
    ]
    hist_clean = hist_clean[cols]

    out_path = CLEAN_REG / "wa_vrdb_voter_history.csv"
    hist_clean.to_csv(out_path, index=False)
    print(f"[WA VRDB] wrote voter history -> {out_path}")
    return str(out_path)


def main():
    preprocess_wa_vrdb_voters_and_precinct_summary()
    preprocess_wa_vrdb_history()


if __name__ == "__main__":
    main()