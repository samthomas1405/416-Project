from __future__ import annotations
from pathlib import Path

# File is: 416-PROJECT/data/preprocess/utils/paths.py
# parents[3] = 416-PROJECT
ROOT = Path(__file__).resolve().parents[3]

DATA = ROOT / "data"          # 416-PROJECT/data
RAW = DATA / "raw"            # 416-PROJECT/data/raw
CLEAN = ROOT / "data_clean"   # 416-PROJECT/data_clean

# Ensure standard cleaned folders exist
(CLEAN / "eavs").mkdir(parents=True, exist_ok=True)
(CLEAN / "results_2024").mkdir(parents=True, exist_ok=True)
(CLEAN / "registration").mkdir(parents=True, exist_ok=True)
(CLEAN / "cvap").mkdir(parents=True, exist_ok=True)
(CLEAN / "geo").mkdir(parents=True, exist_ok=True)
(CLEAN / "gui").mkdir(parents=True, exist_ok=True)
(CLEAN / "equipment").mkdir(parents=True, exist_ok=True)