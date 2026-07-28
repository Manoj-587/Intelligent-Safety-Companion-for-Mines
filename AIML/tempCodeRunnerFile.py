# Resolve the dataset from the project folder.
BASE_DIR = Path(__file__).resolve().parent
DATA_DIR = BASE_DIR / "mine-ai" / "dataset"

candidate_files = [
    DATA_DIR / "Methane Data 100.csv",
    DATA_DIR / "mine_risk_dataset.csv",
]

DATA_PATH = next((path for path in candidate_files if path.exists()), None)

if DATA_PATH is None:
    csv_files = sorted(p.name for p in DATA_DIR.glob("*.csv"))
    if csv_files:
        DATA_PATH = DATA_DIR / csv_files[0]
    else:
        raise FileNotFoundError(f"No CSV file found in {DATA_DIR}")

OUTPUT_PATH = DATA_DIR / "mine_risk_dataset.csv"