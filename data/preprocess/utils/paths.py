from pathlib import Path

DATA = Path(__file__).resolve().parents[2]
ROOT = DATA.parent

RAW   = DATA / "raw"
CLEAN = ROOT / "data_clean"

# Make common output dirs once
for sub in ["results_2024", "registration", "cvap", "eavs", "geo", "gui"]:
    (CLEAN / sub).mkdir(parents=True, exist_ok=True)