from __future__ import annotations

import pandas as pd

from data.preprocess.utils.paths import RAW, CLEAN

RAW_REG = RAW / "registration" / "wa"
CLEAN_REG = CLEAN / "registration"
CLEAN_REG.mkdir(parents=True, exist_ok=True)


def _load_demographics() -> tuple[pd.DataFrame, pd.DataFrame]:
    xls = pd.ExcelFile(RAW_REG / "voter_demographics.xlsx")
    age = pd.read_excel(xls, "County and Age Group")
    gender = pd.read_excel(xls, "County and Gender")
    return age, gender


def preprocess_wa_registration_demographics() -> tuple[pd.DataFrame, pd.DataFrame]:
    """
    County-level registration by age + by gender.
    """
    age, gender = _load_demographics()

    # Age sheet: wide -> long
    age_long = age.melt(
        id_vars=["County"],
        var_name="age_group",
        value_name="registered_voters",
    )
    age_long["state_abbr"] = "WA"
    age_long["registered_voters"] = (
        pd.to_numeric(age_long["registered_voters"], errors="coerce")
        .fillna(0)
        .astype(int)
    )

    # Gender sheet: wide -> long
    gender_long = gender.melt(
        id_vars=["County"],
        var_name="gender",
        value_name="registered_voters",
    )
    gender_long["state_abbr"] = "WA"
    gender_long["registered_voters"] = (
        pd.to_numeric(gender_long["registered_voters"], errors="coerce")
        .fillna(0)
        .astype(int)
    )

    age_out = age_long[
        ["state_abbr", "County", "age_group", "registered_voters"]
    ].rename(columns={"County": "county_name"})

    gender_out = gender_long[
        ["state_abbr", "County", "gender", "registered_voters"]
    ].rename(columns={"County": "county_name"})

    dest_age = CLEAN_REG / "wa_registration_by_county_age.csv"
    dest_gender = CLEAN_REG / "wa_registration_by_county_gender.csv"

    age_out.to_csv(dest_age, index=False)
    gender_out.to_csv(dest_gender, index=False)

    print(f"[WA registration] wrote {dest_age} ({len(age_out)} rows)")
    print(f"[WA registration] wrote {dest_gender} ({len(gender_out)} rows)")

    return age_out, gender_out


def preprocess_wa_participation_2024() -> pd.DataFrame:
    """
    Clean the WA turnout / participation table for the 2024 General election.

    Input:
        data/raw/registration/wa/VoterParticipation.xlsx ('Table')
    """
    xls = pd.ExcelFile(RAW_REG / "VoterParticipation.xlsx")
    tbl = pd.read_excel(xls, "Table")

    # Keep 2024 General rows only
    mask = (tbl["Year"] == 2024) & (tbl["Election Type"] == "General")
    tbl = tbl[mask].copy()

    tbl["state_abbr"] = "WA"
    tbl["Age"] = tbl["Age"].astype(str).str.strip()
    tbl["Age"] = tbl["Age"].replace({"0TOTAL": "TOTAL"})

    # Convert numeric-ish columns
    num_cols = [
        "Total Population",
        "Total Voters",
        "Total Registered Population",
        "Total Voter Turnout",
    ]
    for col in num_cols:
        if col not in tbl.columns:
            continue
        tbl[col] = (
            tbl[col]
            .astype(str)
            .str.replace("%", "", regex=False)
        )
        tbl[col] = pd.to_numeric(tbl[col], errors="coerce")

    out = tbl.rename(
        columns={
            "County": "county_name",
            "Age": "age_group",
            "Total Population": "total_population",
            "Total Voters": "total_voters",
            # In the actual table these are FRACTIONS / SHARES, not counts.
            "Total Registered Population": "registered_population_share",
            "Total Voter Turnout": "voter_turnout_share",
        }
    )

    out_cols = [
        "state_abbr",
        "county_name",
        "age_group",
        "Year",
        "Election Type",
        "total_population",
        "total_voters",
        "registered_population_share",
        "voter_turnout_share",
    ]
    out_cols = [c for c in out_cols if c in out.columns]
    out = out[out_cols].copy().sort_values(["county_name", "age_group"])

    dest = CLEAN_REG / "wa_participation_2024_by_county_age.csv"
    out.to_csv(dest, index=False)
    print(f"[WA participation] wrote {dest} ({len(out)} rows)")

    return out


def main():
    preprocess_wa_registration_demographics()
    preprocess_wa_participation_2024()


if __name__ == "__main__":
    main()