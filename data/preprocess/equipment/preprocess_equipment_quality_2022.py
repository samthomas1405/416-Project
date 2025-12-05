from __future__ import annotations

from pathlib import Path
from typing import List

import pandas as pd

from data.preprocess.utils.paths import CLEAN

CLEAN_EQ = CLEAN / "equipment"
CLEAN_EQ.mkdir(parents=True, exist_ok=True)

CURRENT_YEAR = 2024  # adjust if needed


def _to_yes_flag(val: str) -> int:
    if val is None:
        return 0
    s = str(val).strip().lower()
    if s in {"y", "yes", "true", "t", "1"}:
        return 1
    return 0


def preprocess_equipment_quality_2022() -> None:
    """
    Read cleaned equipment_2022 and jurisdictions_2022 and compute:

      1) equipment_2022_with_quality.csv
           - one row per equipment record (manufacturer/model) with 'quality_score'

      2) equipment_quality_by_jurisdiction_2022.csv
           - one row per jurisdiction with 'avg_quality_score'
    """
    eq_path = CLEAN_EQ / "equipment_2022.csv"
    jur_path = CLEAN_EQ / "jurisdictions_2022.csv"

    if not eq_path.exists() or not jur_path.exists():
        raise FileNotFoundError(
            "Expected data_clean/equipment/equipment_2022.csv and "
            "data_clean/equipment/jurisdictions_2022.csv to exist."
        )

    eq = pd.read_csv(eq_path)
    jur = pd.read_csv(jur_path)

    # Normalize join keys
    for df in (eq, jur):
        if "state_abbr" in df.columns:
            df["state_abbr"] = df["state_abbr"].astype(str).str.upper().str.strip()
        if "FIPS Code" in df.columns:
            df["FIPS Code"] = (
                df["FIPS Code"]
                .astype(str)
                .str.strip()
                .str.replace(".0", "", regex=False)
            )

    # ---- 1) Per-equipment quality score ----

    first_year_col = "First Year in Use"
    if first_year_col in eq.columns:
        fy = pd.to_numeric(eq[first_year_col], errors="coerce")
        age_years = CURRENT_YEAR - fy
        age_years = age_years.clip(lower=0, upper=30)
        age_score = 1 - (age_years / 30)
        age_score = age_score.fillna(0)
    else:
        # If somehow missing, treat as unknown (0.5)
        age_score = pd.Series(0.5, index=eq.index)

    vvp_col = "VVPAT" if "VVPAT" in eq.columns else None
    barcode_col = "Barcode" if "Barcode" in eq.columns else None

    if vvp_col is not None:
        vvp_score = eq[vvp_col].map(_to_yes_flag)
    else:
        vvp_score = pd.Series(0, index=eq.index)

    if barcode_col is not None:
        barcode_score = eq[barcode_col].map(_to_yes_flag)
    else:
        barcode_score = pd.Series(0, index=eq.index)

    quality_score = 0.6 * age_score + 0.2 * vvp_score + 0.2 * barcode_score
    quality_score = quality_score.clip(lower=0.0, upper=1.0)

    eq["quality_score"] = quality_score

    eq_out_path = CLEAN_EQ / "equipment_2022_with_quality.csv"
    eq.to_csv(eq_out_path, index=False)
    print(f"[Equipment] wrote per-equipment quality -> {eq_out_path}")

    # ---- 2) Jurisdiction-level average quality ----

    # We aggregate by (state_abbr, FIPS Code)
    group_cols = ["state_abbr", "FIPS Code"]
    juris_key_cols = group_cols + ["State", "Jurisdiction"]

    avg_quality = (
        eq.groupby(group_cols, as_index=False)["quality_score"].mean()
        .rename(columns={"quality_score": "avg_quality_score"})
    )

    # Bring in jurisdiction metadata
    jur_small = jur[juris_key_cols].drop_duplicates()
    merged = pd.merge(
        jur_small,
        avg_quality,
        on=group_cols,
        how="left",
    )

    # If any jurisdiction lacks equipment rows, avg_quality_score will be NaN -> set to 0
    merged["avg_quality_score"] = merged["avg_quality_score"].fillna(0.0)

    jur_out_path = CLEAN_EQ / "equipment_quality_by_jurisdiction_2022.csv"
    merged.to_csv(jur_out_path, index=False)
    print(f"[Equipment] wrote jurisdiction-level quality -> {jur_out_path}")


def main():
    preprocess_equipment_quality_2022()


if __name__ == "__main__":
    main()