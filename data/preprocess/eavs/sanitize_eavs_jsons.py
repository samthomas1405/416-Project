from __future__ import annotations
import json
from pathlib import Path
import pandas as pd

REPO = Path(__file__).resolve().parents[3]
DIR = REPO / "data_clean" / "results_2024"

CHORO = [
    "provisional_choropleth.json",
    "pollbook_deletions_choropleth.json",
    "mail_rejections_choropleth.json",
]
TABLE = [
    "provisional_table.json",
    "pollbook_deletions_table.json",
    "mail_rejections_table.json",
]

def fix_fips(val):
    s = pd.Series([val]).astype(str).str.extract(r"(\d{5})")[0]
    return s.iloc[0] if pd.notna(s.iloc[0]) else None

def clean_choropleth(path: Path):
    obj = json.loads(path.read_text(encoding="utf-8"))
    rows = obj.get("data", [])
    out = []
    for r in rows:
        fips = fix_fips(r.get("FIPSCODE"))
        if fips:
            r["FIPSCODE"] = fips
            out.append(r)
    obj["data"] = out
    path.write_text(json.dumps(obj), encoding="utf-8")

def clean_table(path: Path):
    obj = json.loads(path.read_text(encoding="utf-8"))
    rows = obj.get("rows", [])
    out = []
    for r in rows:
        fips = r.get("FIPSCODE")
        if isinstance(fips, str) and fips.upper() == "TOTALS":
            continue
        fips = fix_fips(fips)
        if fips:
            r["FIPSCODE"] = fips
            out.append(r)
    obj["rows"] = out
    path.write_text(json.dumps(obj), encoding="utf-8")

def main():
    for name in CHORO:
        p = DIR / name
        if p.exists(): clean_choropleth(p)
    for name in TABLE:
        p = DIR / name
        if p.exists(): clean_table(p)
    print("Sanitized JSONs in", DIR)

if __name__ == "__main__":
    main()