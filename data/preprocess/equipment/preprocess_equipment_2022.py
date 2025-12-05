from __future__ import annotations

from pathlib import Path
from typing import Tuple, List

import pandas as pd

from data.preprocess.utils.paths import RAW, CLEAN

RAW_EQ = RAW / "equipment"
CLEAN_EQ = CLEAN / "equipment"
CLEAN_EQ.mkdir(parents=True, exist_ok=True)

STATES = ["ma", "il", "nc", "ia", "wa"]


def _load_sheet(path: Path, sheet_name: str) -> pd.DataFrame:
    """
    All state equipment workbooks share the same structure:
        - Row 0: a long title string
        - Row 1: real column headings
        - Row 2+: data
    """
    xls = pd.ExcelFile(path)
    df = pd.read_excel(xls, sheet_name, header=None)
    header = df.iloc[1].tolist()
    df = df.iloc[2:].copy()
    df.columns = header
    df = df.dropna(how="all")
    return df


def preprocess_equipment_2022() -> Tuple[pd.DataFrame, pd.DataFrame]:
    """
    Clean the Voting Equipment "Verifier" datasets for MA, IL, NC, IA, WA.

    Inputs:
        data/raw/equipment/<state>.xlsx

    Outputs:
        data_clean/equipment/jurisdictions_2022.csv
        data_clean/equipment/equipment_2022.csv
    """
    juris_frames: List[pd.DataFrame] = []
    equip_frames: List[pd.DataFrame] = []

    for st in STATES:
        path = RAW_EQ / f"{st}.xlsx"
        print(f"[Equipment] loading {path}...")

        juris = _load_sheet(path, "Jurisdictions")
        equip = _load_sheet(path, "Equipment")

        juris["state_abbr"] = st.upper()
        equip["state_abbr"] = st.upper()

        for df in (juris, equip):
            if "FIPS Code" in df.columns:
                df["FIPS Code"] = (
                    df["FIPS Code"]
                    .astype(str)
                    .str.strip()
                    .str.replace(".0", "", regex=False)
                )

        juris_frames.append(juris)
        equip_frames.append(equip)

    juris_all = pd.concat(juris_frames, ignore_index=True)
    equip_all = pd.concat(equip_frames, ignore_index=True)

    juris_cols = [
        "state_abbr",
        "FIPS Code",
        "State",
        "Jurisdiction",
        "Registered Voters",
        "Precincts",
        "Election Day Polling Places",
        "Voting Location",
        "All Mail Ballot?",
        "Election Day Marking Method",
        "Election Day Tabulation",
        "Election Day Equipment",
        "Election Day Poll Books",
        "Mail Ballot/Absentee Equipment",
    ]
    juris_cols = [c for c in juris_cols if c in juris_all.columns]
    juris_out = juris_all[juris_cols].copy()

    equip_cols = [
        "state_abbr",
        "FIPS Code",
        "State",
        "Jurisdiction",
        "Equipment Type",
        "Manufacturer",
        "Model",
        "First Year in Use",
        "Barcode",
        "VVPAT",
        "Election Day Standard",
        "Election Day Accessible",
        "Early Voting Standard",
        "Early Voting Accessible",
        "Mail Ballot Equipment",
        "Extra Text",
    ]
    equip_cols = [c for c in equip_cols if c in equip_all.columns]
    equip_out = equip_all[equip_cols].copy()

    juris_dest = CLEAN_EQ / "jurisdictions_2022.csv"
    equip_dest = CLEAN_EQ / "equipment_2022.csv"

    juris_out.to_csv(juris_dest, index=False)
    equip_out.to_csv(equip_dest, index=False)

    print(f"[Equipment] wrote {juris_dest} ({len(juris_out)} rows)")
    print(f"[Equipment] wrote {equip_dest} ({len(equip_out)} rows)")

    return juris_out, equip_out


def main():
    preprocess_equipment_2022()


if __name__ == "__main__":
    main()