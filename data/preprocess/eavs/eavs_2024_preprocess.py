from __future__ import annotations
import argparse
import json
import os
from pathlib import Path
from typing import List, Optional

import pandas as pd

# -------------------- Paths --------------------
REPO = Path(__file__).resolve().parents[3]
RAW_BASE  = REPO / "data" / "raw"
RAW_EAVS  = RAW_BASE / "eavs"
RAW_2024  = RAW_EAVS / "2024"

CLEAN_DIR = REPO / "data_clean"
CLEAN_EAVS_DIR = CLEAN_DIR / "eavs"
RESULTS_DIR = CLEAN_DIR / "results_2024"

# -------------------- Utils --------------------
def ensure_dir(p: Path) -> None:
    p.mkdir(parents=True, exist_ok=True)

def normalize_columns(df: pd.DataFrame) -> pd.DataFrame:
    d = df.copy()
    d.columns = (
        d.columns
        .str.strip()
        .str.replace(r"\s+", "_", regex=True)
        .str.replace(r"[^0-9A-Za-z_]+", "", regex=True)
        .str.upper()
    )
    return d

def clean_numeric(s: pd.Series) -> pd.Series:
    """Convert to numeric, mark -88/-99 and negatives as NaN."""
    x = pd.to_numeric(s, errors="coerce")
    x = x.mask(x.isin([-88, -99]))
    x = x.mask(x < 0)
    return x

def find_first_csv(folder: Path, name_snippets: List[str]) -> Optional[Path]:
    if not folder.exists():
        return None
    for root, _, files in os.walk(folder):
        for f in files:
            low = f.lower()
            if f.endswith(".csv") and any(sn in low for sn in name_snippets):
                return Path(root) / f
    return None

def autodetect_eavs() -> Optional[Path]:
    for folder, pats in [
        (RAW_2024, ["eavs_2024", "eavs 2024"]),
        (RAW_EAVS, ["eavs_2024", "eavs"]),
        (RAW_BASE, ["eavs_2024", "eavs"]),
    ]:
        p = find_first_csv(folder, pats)
        if p:
            return p
    return None

def fix_fips_val(v) -> Optional[str]:
    """Turn 1001, '01001', '1001.0', NaN -> '01001' or None if invalid."""
    try:
        # Cast via float->int to handle '1001.0' and numeric types
        i = int(float(v))
        if i <= 0:
            return None
        return str(i).zfill(5)
    except Exception:
        return None

def quantile_bins_nonneg(series: pd.Series, k: int) -> list[float]:
    vals = pd.to_numeric(series, errors="coerce").dropna()
    vals = vals[vals >= 0]
    if vals.empty:
        return []
    edges = [float(vals.min())]
    for i in range(1, k):
        edges.append(float(vals.quantile(i / k)))
    edges.append(float(vals.max()))
    # Deduplicate nearly identical edges
    dedup = [edges[0]]
    for e in edges[1:]:
        if abs(e - dedup[-1]) > 1e-12:
            dedup.append(e)
    return dedup

def payload_choropleth(df: pd.DataFrame, value_col: str, src: str, bins=7) -> dict:
    return {
        "region_col": "FIPSCODE",
        "value_col": value_col,
        "bins": quantile_bins_nonneg(df[value_col], bins),
        "data": df[["FIPSCODE", value_col]].to_dict(orient="records"),
        "meta": {
            "source": src,
            "generated_at": pd.Timestamp.utcnow().isoformat(),
            "rows": int(len(df)),
        },
    }

def payload_table(df: pd.DataFrame, component_cols: List[str], src: str, include_total: Optional[str] = None) -> dict:
    use = [c for c in component_cols if c in df.columns]
    table = df[["FIPSCODE"] + use + ([include_total] if include_total and include_total in df.columns else [])].copy()
    # Add a final totals row (sum of numeric columns)
    totals = table[use + ([include_total] if include_total and include_total in table.columns else [])].sum(numeric_only=True)
    total_row = {"FIPSCODE": "TOTALS", **{c: float(totals.get(c, 0.0)) for c in totals.index}}
    return {
        "columns": list(table.columns),
        "rows": table.to_dict(orient="records") + [total_row],
        "meta": {
            "source": src,
            "generated_at": pd.Timestamp.utcnow().isoformat(),
            "columns_used": use + ([include_total] if include_total else []),
        },
    }

# -------------------- Scoring (optional, retained) --------------------
def missingness_score(df: pd.DataFrame, required_cols: List[str]) -> pd.Series:
    cols = [c for c in required_cols if c in df.columns]
    if not cols:
        return pd.Series([None] * len(df), index=df.index, name="MISSINGNESS_SCORE")
    frac = df[cols].isna().sum(axis=1) / len(cols)
    return frac.rename("MISSINGNESS_SCORE")

def equipment_quality_score(df: pd.DataFrame) -> pd.Series:
    d = df.copy()
    for c in ["AGE_YEARS", "OS_SCORE", "CERT_CATEGORY", "SCAN_RATE", "ERROR_RATE", "RELIABILITY"]:
        if c not in d.columns:
            d[c] = pd.NA
    cert_w = {
        "VVSG_20_CERT": 1.0,
        "VVSG_20_APPLIED": 0.8,
        "VVSG_11": 0.6,
        "VVSG_10": 0.4,
        "NONE": 0.2,
        None: 0.2,
        "": 0.2,
    }
    d["CERT_W"] = d["CERT_CATEGORY"].map(cert_w).fillna(0.2)

    def mm_pos(x: pd.Series) -> pd.Series:
        x = clean_numeric(x)
        if not x.notna().any():
            return pd.Series([0.5] * len(x), index=x.index)
        xmax = x.quantile(0.95)
        return (x.clip(lower=0, upper=xmax) / (xmax if xmax else 1)).fillna(0.5)

    def mm_inv(x: pd.Series) -> pd.Series:
        z = mm_pos(x)
        return (1 - z).fillna(0.5)

    age_n  = mm_inv(d["AGE_YEARS"])
    scan_n = mm_pos(d["SCAN_RATE"])
    err_n  = mm_inv(d["ERROR_RATE"])
    os_s   = pd.to_numeric(d["OS_SCORE"], errors="coerce").fillna(0.5)
    rel    = pd.to_numeric(d["RELIABILITY"], errors="coerce").fillna(0.5)
    cert   = pd.to_numeric(d["CERT_W"], errors="coerce").fillna(0.2)

    w = {"age": 0.20, "os": 0.15, "cert": 0.20, "scan": 0.15, "error": 0.15, "reliab": 0.15}
    score = (w["age"]*age_n + w["os"]*os_s + w["cert"]*cert + w["scan"]*scan_n + w["error"]*err_n + w["reliab"]*rel).clip(0, 1)
    return score.rename("EQUIPMENT_QUALITY_SCORE")

# -------------------- Main --------------------
def main():
    ensure_dir(CLEAN_EAVS_DIR)
    ensure_dir(RESULTS_DIR)

    ap = argparse.ArgumentParser(description="EAVS 2024 preprocessing")
    ap.add_argument("--eavs", help="Path to EAVS 2024 CSV (raw or already cleaned)")
    args = ap.parse_args()

    # 1) Load
    eavs_path = Path(args.eavs) if args.eavs else autodetect_eavs()
    if not eavs_path or not eavs_path.exists():
        raise SystemExit("EAVS 2024 CSV not found. Put under data/raw/eavs/2024/eavs_2024.csv or pass --eavs /path/to.csv")

    df = pd.read_csv(eavs_path, low_memory=False)
    df = normalize_columns(df)

    # 2) Clean numeric-like columns across the board
    for c in df.columns:
        maybe = pd.to_numeric(df[c], errors="coerce")
        if maybe.notna().sum() > 0:
            df[c] = clean_numeric(df[c])

    # 3) Build FIPSCODE and DROP rows without one
    # Prefer these potential source cols in order:
    fips_source = next((c for c in ["FIPSCODE", "FIPS", "FIPS_CODE", "GEOID", "COUNTYFIPS"] if c in df.columns), None)
    if not fips_source:
        # fallback: if nothing exists, we can't produce map-ready outputs
        raise SystemExit("No FIPS-like column found (FIPSCODE/FIPS/FIPS_CODE/GEOID/COUNTYFIPS). Add one and re-run.")
    df["FIPSCODE"] = df[fips_source].apply(fix_fips_val)
    rows_before = len(df)
    df = df.dropna(subset=["FIPSCODE"]).copy()
    rows_after = len(df)

    # 4) Optional scores (kept; harmless if inputs missing)
    required = [c for c in ["E1A", "E2A", "E2B", "E2C", "C3A", "A12B", "A12C"] if c in df.columns]
    df["MISSINGNESS_SCORE"] = missingness_score(df, required)
    df["EQUIPMENT_QUALITY_SCORE"] = equipment_quality_score(df)

    src_rel = str(eavs_path.relative_to(REPO)) if eavs_path.is_relative_to(REPO) else str(eavs_path)

    # ---------- Provisional ----------
    if "E1A" in df.columns:
        prov = df.groupby("FIPSCODE", as_index=False)[["E1A"]].sum(min_count=1)
        prov["E1A"] = prov["E1A"].fillna(0).clip(lower=0)
        (RESULTS_DIR / "provisional_choropleth.json").write_text(
            json.dumps(payload_choropleth(prov, "E1A", src_rel)), encoding="utf-8"
        )

    e2_cols = [c for c in df.columns if c.startswith("E2") and len(c) == 3]
    if e2_cols:
        prov_tbl = df.groupby("FIPSCODE", as_index=False)[e2_cols].sum(min_count=1)
        for c in e2_cols:
            prov_tbl[c] = prov_tbl[c].fillna(0).clip(lower=0)
        (RESULTS_DIR / "provisional_table.json").write_text(
            json.dumps(payload_table(prov_tbl, e2_cols, src_rel)), encoding="utf-8"
        )

    # ---------- Pollbook deletions (A12*) ----------
    a12_cols = [c for c in df.columns if c.startswith("A12") and len(c) <= 4]
    if a12_cols:
        deletions = df.groupby("FIPSCODE", as_index=False)[a12_cols].sum(min_count=1)
        for c in a12_cols:
            deletions[c] = deletions[c].fillna(0).clip(lower=0)
        deletions["TOTAL_DELETIONS"] = deletions[a12_cols].sum(axis=1, numeric_only=True)
        (RESULTS_DIR / "pollbook_deletions_choropleth.json").write_text(
            json.dumps(payload_choropleth(deletions, "TOTAL_DELETIONS", src_rel)), encoding="utf-8"
        )
        (RESULTS_DIR / "pollbook_deletions_table.json").write_text(
            json.dumps(payload_table(deletions, a12_cols, src_rel, include_total="TOTAL_DELETIONS")), encoding="utf-8"
        )

    # ---------- Mail ballot rejections (prefer D*-series; fallback to C9*) ----------
    d_cols = [c for c in df.columns if c.startswith("D") and len(c) == 2]   # e.g., D1..D9 by instrument
    c9_cols = [c for c in df.columns if c.startswith("C9") and len(c) == 3]
    mail_cols = d_cols if d_cols else c9_cols
    total_mail_label = "TOTAL_REJECTIONS" if d_cols else ("TOTAL_C9" if c9_cols else None)
    if mail_cols:
        mail = df.groupby("FIPSCODE", as_index=False)[mail_cols].sum(min_count=1)
        for c in mail_cols:
            mail[c] = mail[c].fillna(0).clip(lower=0)
        mail[total_mail_label] = mail[mail_cols].sum(axis=1, numeric_only=True)
        # table
        (RESULTS_DIR / "mail_rejections_table.json").write_text(
            json.dumps(payload_table(mail, mail_cols, src_rel, include_total=total_mail_label)), encoding="utf-8"
        )
        # choropleth
        (RESULTS_DIR / "mail_rejections_choropleth.json").write_text(
            json.dumps(payload_choropleth(mail.rename(columns={total_mail_label: "TOTAL_REJECTIONS"}), "TOTAL_REJECTIONS", src_rel)),
            encoding="utf-8",
        )

    # 5) Save cleaned table (with fixed FIPSCODE)
    ensure_dir(CLEAN_EAVS_DIR)
    out_parquet = CLEAN_EAVS_DIR / "eavs_2024_cleaned.parquet"
    try:
        df.to_parquet(out_parquet, index=False)
        print(f"[ok] Saved: {out_parquet}")
    except Exception:
        out_csv = CLEAN_EAVS_DIR / "eavs_2024_cleaned.csv"
        df.to_csv(out_csv, index=False)
        print(f"[ok] Saved: {out_csv}")

    # Status print
    print(f"[info] Source: {src_rel}")
    print(f"[info] Dropped rows without valid FIPSCODE: {rows_before - rows_after}")
    print(f"[ok] JSON payloads -> {RESULTS_DIR}")

if __name__ == "__main__":
    main()