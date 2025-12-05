from __future__ import annotations

import pandas as pd

from data.preprocess.utils.paths import RAW, CLEAN

RAW_EAVS_POLICY = RAW / "eavs" / "2024" / "2024_policy_survey.csv"
CLEAN_EAVS = CLEAN / "eavs"
CLEAN_EAVS.mkdir(parents=True, exist_ok=True)

STATE_ABBRS = ["MA", "IL", "NC", "IA", "WA"]


def preprocess_felony_policy_from_eavs() -> pd.DataFrame:
    """
    Extract felony voting rights policy indicators from the 2024
    EAVS Policy Survey (Q51 block).

    Output:
        data_clean/eavs/felony_policy_2024_q51.csv
    """
    df = pd.read_csv(RAW_EAVS_POLICY)

    df = df[df["STATE"].isin(STATE_ABBRS)].copy()

    q51_cols = [c for c in df.columns if c.startswith("Q51")]
    out_cols = ["STATE", "STATE_FULL"] + q51_cols

    out = df[out_cols].copy().sort_values("STATE")

    dest = CLEAN_EAVS / "felony_policy_2024_q51.csv"
    out.to_csv(dest, index=False)
    print(f"[Felony Policy] wrote {dest} ({len(out)} rows)")

    return out


def main():
    preprocess_felony_policy_from_eavs()


if __name__ == "__main__":
    main()