from __future__ import annotations

from pathlib import Path
from typing import Dict, List, Tuple

import math
import pandas as pd

from data.preprocess.utils.paths import RAW, CLEAN

RAW_RESULTS_2024 = RAW / "results_2024"
CLEAN_RESULTS_2024 = CLEAN / "results_2024"
CLEAN_RESULTS_2024.mkdir(parents=True, exist_ok=True)


# -------------------------
# Helper: robust int parser
# -------------------------

def _parse_int(x):
    """
    Parse integers from either a scalar or a pandas Series.

    - If x is a Series: strip commas/whitespace and return a numeric Series.
    - If x is a scalar: return a single numeric value.
    """
    if isinstance(x, pd.Series):
        return pd.to_numeric(
            x.astype(str).str.replace(",", "", regex=False).str.strip(),
            errors="coerce",
        )
    else:
        return pd.to_numeric(str(x).replace(",", "", regex=False).strip(), errors="coerce")


# -------------------------
# MASSACHUSETTS (town level)
# -------------------------

def preprocess_ma() -> pd.DataFrame:
    """
    Parse MA 2024 presidential results by town.

    Input:  data/raw/results_2024/ma/Ma_2024_results.csv
    Output: data_clean/results_2024/pres_2024_ma_by_town.csv
    """
    path = RAW_RESULTS_2024 / "ma" / "Ma_2024_results.csv"
    df = pd.read_csv(path)

    # Standardize name column
    df = df.rename(columns={"City/Town": "city_town"})

    # Drop non-town/header rows
    df = df[df["city_town"].notna()].copy()
    df["city_town"] = df["city_town"].astype(str).str.strip()
    # Drop statewide total row
    df = df[df["city_town"].str.upper() != "TOTALS"].copy()

    # Identify DEM and REP columns by candidate text
    dem_col = [c for c in df.columns if "Harris" in c][0]   # "Harris/ Walz"
    rep_col = [c for c in df.columns if "Trump" in c][0]    # "Trump/ Vance"

    # Other candidate/ballot columns
    ignore_cols = {"city_town", "Total Votes Cast"} | {
        c for c in df.columns if c.startswith("Unnamed")
    }
    candidate_cols = [c for c in df.columns if c not in ignore_cols]
    other_cols = [c for c in candidate_cols if c not in (dem_col, rep_col)]

    # Votes
    df["votes_dem_2024_pres"] = _parse_int(df[dem_col]).fillna(0).astype(int)
    df["votes_rep_2024_pres"] = _parse_int(df[rep_col]).fillna(0).astype(int)

    if other_cols:
        tmp = df[other_cols].apply(lambda col: _parse_int(col)).fillna(0)
        df["votes_other_2024_pres"] = tmp.sum(axis=1).astype(int)
    else:
        df["votes_other_2024_pres"] = 0

    df["total_votes_2024_pres"] = (
        df["votes_dem_2024_pres"]
        + df["votes_rep_2024_pres"]
        + df["votes_other_2024_pres"]
    )

    denom = df["total_votes_2024_pres"].replace(0, pd.NA)
    df["dem_share_2024_pres"] = df["votes_dem_2024_pres"] / denom
    df["rep_share_2024_pres"] = df["votes_rep_2024_pres"] / denom

    out = df[[
        "city_town",
        "votes_dem_2024_pres",
        "votes_rep_2024_pres",
        "votes_other_2024_pres",
        "total_votes_2024_pres",
        "dem_share_2024_pres",
        "rep_share_2024_pres",
    ]].copy()

    out.insert(0, "state_abbr", "MA")

    out_path = CLEAN_RESULTS_2024 / "pres_2024_ma_by_town.csv"
    out.to_csv(out_path, index=False)
    print(f"[Results 2024] wrote MA town results -> {out_path}")

    return out


# -------------------------
# IOWA (PDF-style table CSV)
# -------------------------

def _build_bucket_from_header_row_ia(hdr: pd.Series) -> Tuple[Dict[str, str], bool]:
    """
    Examine the header row (col_3..col_13) to determine which columns are DEM, REP, OTHER, or IGNORE.
    Returns (bucket_map, is_presidential_contest).
    """
    cand_cols = [c for c in hdr.index if c.startswith("col_")]
    buckets: Dict[str, str] = {}
    is_pres = False

    for c in cand_cols:
        label = str(hdr[c] or "")
        u = label.upper()
        if not u or u == "NAN":
            bucket = "IGNORE"
        elif any(k in u for k in ["UNDER", "OVER", "TOTAL"]):
            bucket = "IGNORE"
        elif "HARRIS" in u or " DEM" in u or "DEMOCRAT" in u:
            bucket = "DEM"
            is_pres = True
        elif "TRUMP" in u or "VANCE" in u or " REP" in u or "REPUBLICAN" in u:
            bucket = "REP"
            is_pres = True
        elif "WRITE" in u:
            bucket = "OTHER"
        else:
            bucket = "OTHER"

        buckets[c] = bucket

    return buckets, is_pres


def _preprocess_long_state_generic(
    state_abbr: str,
    csv_name: str,
    county_name_col: str,
    vote_cols: List[str] = None,
) -> pd.DataFrame:
    """
    Generic helper for states where we already have a "long" style CSV with one row
    per county and columns for presidential candidates.

    This is NOT used for IA, but is kept as a utility.
    """
    path = RAW_RESULTS_2024 / state_abbr.lower() / csv_name
    df = pd.read_csv(path)

    if vote_cols is None:
        raise ValueError("vote_cols must be provided for generic state preprocessing")

    for col in vote_cols:
        df[col] = _parse_int(df[col]).fillna(0).astype(int)

    df["votes_dem_2024_pres"] = df[vote_cols[0]]
    df["votes_rep_2024_pres"] = df[vote_cols[1]]
    df["votes_other_2024_pres"] = df[vote_cols[2:]].sum(axis=1) if len(vote_cols) > 2 else 0

    df["total_votes_2024_pres"] = (
        df["votes_dem_2024_pres"]
        + df["votes_rep_2024_pres"]
        + df["votes_other_2024_pres"]
    )

    denom = df["total_votes_2024_pres"].replace(0, pd.NA)
    df["dem_share_2024_pres"] = df["votes_dem_2024_pres"] / denom
    df["rep_share_2024_pres"] = df["votes_rep_2024_pres"] / denom

    out = df[[
        county_name_col,
        "votes_dem_2024_pres",
        "votes_rep_2024_pres",
        "votes_other_2024_pres",
        "total_votes_2024_pres",
        "dem_share_2024_pres",
        "rep_share_2024_pres",
    ]].copy()

    out.insert(0, "state_abbr", state_abbr)

    return out


def preprocess_long_state(state_abbr: str) -> pd.DataFrame:
    """
    Parse "long" style CSV exported from a PDF table (for IA and WA).
    The files are expected to have generic col_0, col_1, ... columns.

    For IA we use a special header-row-based bucket detection.
    For WA we assume there is already explicit candidate labeling; however,
    this function currently supports IA and a generic structure for WA.
    """
    path = RAW_RESULTS_2024 / state_abbr.lower() / f"{state_abbr}_2024_table.csv"
    df = pd.read_csv(path)

    # Standardize column names: col_0, col_1, ...
    df = df.rename(columns={c: f"col_{i}" for i, c in enumerate(df.columns)})

    records: List[Dict[str, object]] = []
    current_county = None
    current_buckets: Dict[str, str] = {}
    current_is_pres = False

    for _, row in df.iterrows():
        # Header rows signaled by "Total" or candidate labels in col_3..col_13
        hdr = row
        cand_region = hdr[[c for c in hdr.index if c.startswith("col_3") or c.startswith("col_4")]]

        if any(isinstance(x, str) and "HARRIS" in x.upper() for x in cand_region):
            # New header row for IA/WA presidential contest
            current_buckets, current_is_pres = _build_bucket_from_header_row_ia(hdr)
            continue

        if not current_is_pres:
            continue

        # County name lives primarily in col_1
        c1_raw = row.get("col_1")
        if c1_raw is not None and not (isinstance(c1_raw, float) and math.isnan(c1_raw)):
            c1 = str(c1_raw)
        else:
            c1 = ""

        c2 = str(row.get("col_2") or "")

        # Update county when a new name appears
        if c1.strip() and c1.strip().lower() != "nan":
            current_county = c1.strip()

        # Skip if we don't have a county yet
        if not current_county:
            continue

        # Sum over candidate columns using current_buckets
        dem_votes = 0
        rep_votes = 0
        other_votes = 0

        for col_name, bucket in current_buckets.items():
            if bucket == "IGNORE":
                continue

            val = row.get(col_name)
            if val is None or (isinstance(val, float) and math.isnan(val)):
                v = 0
            else:
                v = _parse_int(val)

            if bucket == "DEM":
                dem_votes += int(v or 0)
            elif bucket == "REP":
                rep_votes += int(v or 0)
            elif bucket == "OTHER":
                other_votes += int(v or 0)

        total = dem_votes + rep_votes + other_votes
        if total == 0:
            continue

        records.append(
            {
                "state_abbr": state_abbr,
                "county_name": current_county,
                "votes_dem_2024_pres": dem_votes,
                "votes_rep_2024_pres": rep_votes,
                "votes_other_2024_pres": other_votes,
                "total_votes_2024_pres": total,
                "dem_share_2024_pres": dem_votes / total if total else None,
                "rep_share_2024_pres": rep_votes / total if total else None,
            }
        )

    result = pd.DataFrame(records)

    # Keep only rows with both DEM and REP > 0
    mask = (result["votes_dem_2024_pres"] > 0) & (
        result["votes_rep_2024_pres"] > 0
    )
    result = result[mask].copy()

    return result


def preprocess_ia() -> pd.DataFrame:
    """
    Wrapper for IA using the long-state generic parser.
    Expects data/raw/results_2024/ia/IA_2024_table.csv
    """
    df = preprocess_long_state("IA")
    out_path = CLEAN_RESULTS_2024 / "pres_2024_ia_by_county.csv"
    df.to_csv(out_path, index=False)
    print(f"[Results 2024] wrote IA county results -> {out_path}")
    return df


def preprocess_wa() -> pd.DataFrame:
    """
    Wrapper for WA using the long-state generic parser.
    Expects data/raw/results_2024/wa/WA_2024_table.csv
    """
    df = preprocess_long_state("WA")
    out_path = CLEAN_RESULTS_2024 / "pres_2024_wa_by_county.csv"
    df.to_csv(out_path, index=False)
    print(f"[Results 2024] wrote WA county results -> {out_path}")
    return df


# -------------------------
# WASHINGTON (precinct-level presidential results, 2024)
# -------------------------

def preprocess_wa_precinct_pres_2024() -> pd.DataFrame:
    """
    Build WA 2024 presidential results by precinct for the bubble chart.

    Input:
        data/raw/results_2024/2024Gen_Precinct_Results_GIS-Ready_Federal.csv

    Output:
        data_clean/results_2024/pres_2024_wa_by_precinct.csv

    Columns in output:
        state_abbr         - 'WA'
        st_code            - precinct code from St_Code (e.g. 'AD00000111')
        county_code        - first two letters of st_code (e.g. 'AD')
        precinct_number    - last three characters of st_code (e.g. '111')
        votes_dem_2024_pres
        votes_rep_2024_pres
        votes_other_2024_pres
        total_votes_2024_pres
        dem_share_2024_pres
        rep_share_2024_pres
        dominant_party     - 'DEM', 'REP', or 'TIE'
        margin             - dem_share - rep_share
    """
    path = RAW_RESULTS_2024 / "2024Gen_Precinct_Results_GIS-Ready_Federal.csv"
    if not path.exists():
        raise FileNotFoundError(f"Expected WA precinct federal results at {path}")

    df = pd.read_csv(path, dtype=str)

    # Core identifier: precinct code used by WA SOS
    if "St_Code" not in df.columns:
        raise KeyError("Expected column 'St_Code' in WA precinct results file")

    df["st_code"] = df["St_Code"].astype(str).str.strip()
    df["state_abbr"] = "WA"

    # Extract approximate county/precinct from st_code pattern like 'AD00000111'
    # First two characters are county code; last three characters precinct number.
    df["county_code"] = df["st_code"].str[:2]
    df["precinct_number"] = df["st_code"].str[-3:]

    # Presidential candidate columns
    pres_cols = [
        "G24PRSHARR",  # Harris / Walz (Democratic)
        "G24PRSTRUM",  # Trump / Vance (Republican)
        "G24PRSKENN",  # Kennedy
        "G24PRSSTEI",  # Stein
        "G24PRSDELA",  # De La Fuente
        "G24PRSFRUI",  # Fruin
        "G24PRSTANN",  # Tanner
        "G24PRSOLIV",  # Oliver
        "G24PRSWEST",  # West
        "G24PRSAYYA",  # Ayya
        "G24PRSW-I",   # Write-ins
    ]

    # Ensure all columns exist; if not, fill with 0
    for col in pres_cols:
        if col not in df.columns:
            df[col] = 0

    # Convert to numeric
    for col in pres_cols:
        df[col] = pd.to_numeric(df[col], errors="coerce").fillna(0).astype(int)

    cand_dem = "G24PRSHARR"
    cand_rep = "G24PRSTRUM"
    other_cols = [c for c in pres_cols if c not in (cand_dem, cand_rep)]

    df["votes_dem_2024_pres"] = df[cand_dem]
    df["votes_rep_2024_pres"] = df[cand_rep]
    df["votes_other_2024_pres"] = df[other_cols].sum(axis=1)

    df["total_votes_2024_pres"] = (
        df["votes_dem_2024_pres"]
        + df["votes_rep_2024_pres"]
        + df["votes_other_2024_pres"]
    )

    # Avoid divide-by-zero
    denom = df["total_votes_2024_pres"].replace(0, pd.NA)
    df["dem_share_2024_pres"] = df["votes_dem_2024_pres"] / denom
    df["rep_share_2024_pres"] = df["votes_rep_2024_pres"] / denom

    def _dominant(row):
        ds = row["dem_share_2024_pres"]
        rs = row["rep_share_2024_pres"]
        if pd.isna(ds) or pd.isna(rs):
            return "TIE"
        if ds > rs:
            return "DEM"
        if rs > ds:
            return "REP"
        return "TIE"

    df["dominant_party"] = df.apply(_dominant, axis=1)
    df["margin"] = df["dem_share_2024_pres"] - df["rep_share_2024_pres"]

    out = df[
        [
            "state_abbr",
            "st_code",
            "county_code",
            "precinct_number",
            "votes_dem_2024_pres",
            "votes_rep_2024_pres",
            "votes_other_2024_pres",
            "total_votes_2024_pres",
            "dem_share_2024_pres",
            "rep_share_2024_pres",
            "dominant_party",
            "margin",
        ]
    ].copy()

    out_path = CLEAN_RESULTS_2024 / "pres_2024_wa_by_precinct.csv"
    out.to_csv(out_path, index=False)
    print(f"[Results 2024] wrote WA precinct presidential results -> {out_path}")

    return out


def main():
    print("=== Preprocessing MA 2024 presidential results (town level) ===")
    ma = preprocess_ma()

    print("=== Preprocessing IA 2024 presidential results (county level) ===")
    ia = preprocess_ia()

    print("=== Preprocessing WA 2024 presidential results (county level) ===")
    wa = preprocess_wa()

    # Combine IA + WA into a single county-level file
    pres_by_county = pd.concat([ia, wa], ignore_index=True)
    out_path_county = CLEAN_RESULTS_2024 / "pres_2024_by_county.csv"
    pres_by_county.to_csv(out_path_county, index=False)
    print(f"[ok] IA+WA county results -> {out_path_county}")

    print("=== Preprocessing WA 2024 presidential results (precinct level) ===")
    wa_precinct = preprocess_wa_precinct_pres_2024()


if __name__ == "__main__":
    main()