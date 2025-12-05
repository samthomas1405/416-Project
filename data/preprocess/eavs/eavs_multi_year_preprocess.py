from __future__ import annotations

from pathlib import Path
import zipfile
from typing import List

import pandas as pd

from data.preprocess.utils.paths import RAW, CLEAN

RAW_EAVS = RAW / "eavs"
CLEAN_EAVS = CLEAN / "eavs"
CLEAN_EAVS.mkdir(parents=True, exist_ok=True)


def _read_csv_fallback(path: Path) -> pd.DataFrame:
    """Read a CSV, falling back to latin-1 if utf-8 fails."""
    try:
        return pd.read_csv(path)
    except UnicodeDecodeError:
        return pd.read_csv(path, encoding="latin1")


def _load_eavs_year(year: int) -> pd.DataFrame:
    """Load the raw EAVS file for the given year from data/raw/eavs/<year>/."""
    ydir = RAW_EAVS / str(year)

    if year == 2016:
        path = ydir / "eavs_2016.csv"
        df = _read_csv_fallback(path)

    elif year == 2018:
        path = ydir / "EAVS_2018_for_Public_Release_Updates3.csv"
        df = _read_csv_fallback(path)

    elif year == 2020:
        path = ydir / "eavs_2020.csv"
        df = _read_csv_fallback(path)

    elif year == 2022:
        zpath = ydir / "2022_EAVS_for_Public_Release_nolabel_V1.1_CSV.zip"
        with zipfile.ZipFile(zpath) as z:
            csv_name = [n for n in z.namelist() if n.lower().endswith(".csv")][0]
            with z.open(csv_name) as f:
                try:
                    df = pd.read_csv(f)
                except UnicodeDecodeError:
                    f.seek(0)
                    df = pd.read_csv(f, encoding="latin1")

    elif year == 2024:
        path = ydir / "eavs_2024.csv"
        df = pd.read_csv(path)

    else:
        raise ValueError(f"Unsupported EAVS year: {year}")

    return df


def _derive_fips5(series: pd.Series) -> pd.Series:
    """
    EAVS FIPSCode is a 9-digit code:
        <state_fips(2 digits)><county_fips(3 digits)>000

    Example: FIPSCode = 100100000 (Autauga County, AL)
             -> '01' (state) + '001' (county) -> '01001'

    Returns a string column of length 5 (zero-padded).
    """
    vals = pd.to_numeric(series, errors="coerce")

    state_fips = (vals // 100_000_000).astype("Int64")
    county_fips = ((vals % 100_000_000) // 100_000).astype("Int64")

    out = []
    for s, c in zip(state_fips, county_fips):
        if pd.isna(s) or pd.isna(c):
            out.append(pd.NA)
        else:
            out.append(f"{int(s):02d}{int(c):03d}")

    return pd.Series(out, index=series.index, name="fips5", dtype="string")


def _normalize_year(df: pd.DataFrame, year: int) -> pd.DataFrame:
    """
    Normalise core identifiers for a single EAVS year.

    We keep all original columns and add:

        - year
        - state_abbr
        - jurisdiction_name
        - fips5
    """
    out = df.copy()
    out["year"] = year

    # State abbreviation
    if "State_Abbr" in out.columns:
        out["state_abbr"] = out["State_Abbr"].astype(str).str.strip().str.upper()
    elif "State" in out.columns:
        out["state_abbr"] = out["State"].astype(str).str.strip().str.upper()
    else:
        out["state_abbr"] = pd.NA

    # Jurisdiction name
    if "Jurisdiction_Name" in out.columns:
        out["jurisdiction_name"] = out["Jurisdiction_Name"].astype(str).str.strip()
    elif "JurisdictionName" in out.columns:
        out["jurisdiction_name"] = out["JurisdictionName"].astype(str).str.strip()
    else:
        out["jurisdiction_name"] = pd.NA

    # 9-digit FIPSCode -> 5-digit county FIPS (string)
    if "FIPSCode" in out.columns:
        out["fips5"] = _derive_fips5(out["FIPSCode"])
    else:
        out["fips5"] = pd.Series(pd.NA, index=out.index, dtype="string")

    return out


def preprocess_eavs_multi_year(years: List[int] | None = None) -> pd.DataFrame:
    """
    Load and normalise EAVS 2016–2024 into a single tidy table.

    Output:
        data_clean/eavs/eavs_2016_2024_normalized.csv
    """
    if years is None:
        years = [2016, 2018, 2020, 2022, 2024]

    frames: List[pd.DataFrame] = []
    for y in years:
        print(f"[EAVS] Loading {y}...")
        raw = _load_eavs_year(y)
        print(f"[EAVS]   {y}: {len(raw)} rows, {len(raw.columns)} columns")
        norm = _normalize_year(raw, y)
        frames.append(norm)

    combined = pd.concat(frames, ignore_index=True)
    out_path = CLEAN_EAVS / "eavs_2016_2024_normalized.csv"
    combined.to_csv(out_path, index=False)
    print(f"[EAVS] wrote {out_path} ({len(combined)} rows)")

    return combined


def main():
    preprocess_eavs_multi_year()


if __name__ == "__main__":
    main()