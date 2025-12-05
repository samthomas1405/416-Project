"""
Build equipment history by state, year, and equipment category from EAVS.

Output:
    data_clean/gui/equipment_history_state_year_category.csv

Columns:
    state_abbr, year, equipment_category, device_count

This powers GUI-14 (bar graphs by category with a bar for each federal year).
"""

from __future__ import annotations

from pathlib import Path
from typing import Dict, List

import pandas as pd

from data.preprocess.utils.paths import CLEAN

CLEAN_EAVS = CLEAN / "eavs"
CLEAN_GUI = CLEAN / "gui"
CLEAN_GUI.mkdir(exist_ok=True)

# Years we care about; can be made dynamic if you add more cycles later.
YEARS = [2016, 2018, 2020, 2022, 2024]

# ---------------------------------------------------------------------
# IMPORTANT:
# Map your EAVS F-series columns to the four categories used in the GUI:
#   - DRE_NO_VVPAT
#   - DRE_WITH_VVPAT
#   - BMD
#   - SCANNER
#
# Fill this from the 2016/2018/2020/2022/2024 EAVS codebooks so the
# columns correspond to "number of devices" for each type.
# ---------------------------------------------------------------------
EQUIP_FIELD_MAP: Dict[str, List[str]] = {
    # EXAMPLES ONLY – you must adjust to your actual column names.
    # "DRE_NO_VVPAT": ["F7a"],
    # "DRE_WITH_VVPAT": ["F7b"],
    # "BMD": ["F7c", "F7d"],
    # "SCANNER": ["F7e", "F7f"],
}


def load_eavs_all_years() -> pd.DataFrame:
    """Load the multi-year normalized EAVS table."""
    path = CLEAN_EAVS / "eavs_2016_2024_normalized.csv"
    df = pd.read_csv(path, low_memory=False)
    # Basic sanity: ensure these columns exist
    required = {"year", "state_abbr"}
    missing = required.difference(df.columns)
    if missing:
        raise RuntimeError(f"Missing required columns in EAVS normalized file: {missing}")
    return df


def melt_equipment_for_year(df: pd.DataFrame, year: int) -> pd.DataFrame:
    """
    For a single year, aggregate device counts for each equipment category by state.
    """
    df_year = df[df["year"] == year].copy()
    out_frames: List[pd.DataFrame] = []

    for category, cols in EQUIP_FIELD_MAP.items():
        existing = [c for c in cols if c in df_year.columns]
        if not existing:
            # Nothing for this category in this year; skip.
            continue

        # Sum all relevant F-series "number of devices" columns.
        tmp = df_year[["state_abbr"] + existing].copy()
        for c in existing:
            tmp[c] = pd.to_numeric(tmp[c], errors="coerce").fillna(0)
        tmp["device_count"] = tmp[existing].sum(axis=1)

        grouped = (
            tmp.groupby("state_abbr", as_index=False)["device_count"]
            .sum()
        )
        grouped["year"] = year
        grouped["equipment_category"] = category
        out_frames.append(grouped)

    if not out_frames:
        # Return empty frame with correct schema
        return pd.DataFrame(
            columns=["state_abbr", "year", "equipment_category", "device_count"]
        )

    return pd.concat(out_frames, ignore_index=True)


def main() -> None:
    df = load_eavs_all_years()

    frames: List[pd.DataFrame] = []
    for year in YEARS:
        if year not in df["year"].unique():
            # Skip missing years gracefully
            continue
        frames.append(melt_equipment_for_year(df, year))

    if not frames:
        raise RuntimeError("No equipment data could be aggregated; check EQUIP_FIELD_MAP and EAVS columns.")

    hist = pd.concat(frames, ignore_index=True)

    out_path = CLEAN_GUI / "equipment_history_state_year_category.csv"
    hist.to_csv(out_path, index=False)
    print(f"Wrote equipment history to {out_path}")


if __name__ == "__main__":
    main()
