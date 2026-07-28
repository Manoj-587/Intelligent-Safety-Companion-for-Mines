"""
Mine Safety Risk Prediction — Preprocessing Pipeline
=====================================================
Loads the labeled dataset, inspects it, encodes the target,
splits into train/test sets, standardizes features, and saves
the processed splits ready for model training.
"""

import os
import joblib
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler

# ── Paths ──────────────────────────────────────────────────────────────────────
BASE_DIR    = os.path.dirname(os.path.abspath(__file__))
DATASET_DIR = os.path.join(BASE_DIR, "dataset")
MODEL_DIR   = os.path.join(BASE_DIR, "model")
DATA_PATH   = os.path.join(DATASET_DIR, "mine_risk_dataset.csv")
SCALER_PATH = os.path.join(MODEL_DIR, "scaler.pkl")

# ── 1. Load dataset ────────────────────────────────────────────────────────────
df = pd.read_csv(DATA_PATH)

# ── 2. Dataset inspection ──────────────────────────────────────────────────────
print("=" * 60)
print("DATASET INSPECTION")
print("=" * 60)

print(f"\nShape            : {df.shape}")
print(f"\nData types:\n{df.dtypes}")
print(f"\nMissing values:\n{df.isna().sum()}")
print(f"\nDuplicate rows   : {df.duplicated().sum()}")

# ── 3. Drop unnecessary columns ────────────────────────────────────────────────
COLS_TO_DROP = ["year", "month", "day", "hour", "minute", "second", "V"]
df.drop(columns=COLS_TO_DROP, inplace=True)

# ── 4. Encode target column ────────────────────────────────────────────────────
LABEL_MAP = {"LOW": 0, "MEDIUM": 1, "HIGH": 2}
df["Risk_Level"] = df["Risk_Level"].map(LABEL_MAP)

# ── 5. Separate features and target ───────────────────────────────────────────
X = df.drop(columns=["Risk_Level"])
y = df["Risk_Level"]

# ── 6. Train / test split (80:20, stratified) ──────────────────────────────────
X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.20, random_state=42, stratify=y
)

# ── 7. Standardize features ────────────────────────────────────────────────────
scaler = StandardScaler()
X_train = pd.DataFrame(scaler.fit_transform(X_train), columns=X.columns)
X_test  = pd.DataFrame(scaler.transform(X_test),      columns=X.columns)

# ── 8. Save processed splits and the fitted scaler ────────────────────────────
# The scaler MUST be saved so that predict.py can apply the identical
# transformation to live sensor data at inference time.  Without this,
# the model receives unscaled values and produces incorrect predictions.
os.makedirs(MODEL_DIR, exist_ok=True)
joblib.dump(scaler, SCALER_PATH)

X_train.to_csv(os.path.join(DATASET_DIR, "X_train.csv"), index=False)
X_test.to_csv( os.path.join(DATASET_DIR, "X_test.csv"),  index=False)
y_train.to_csv(os.path.join(DATASET_DIR, "y_train.csv"), index=False)
y_test.to_csv( os.path.join(DATASET_DIR, "y_test.csv"),  index=False)

# ── 9. Summary ─────────────────────────────────────────────────────────────────
print("\n" + "=" * 60)
print("PREPROCESSING SUMMARY")
print("=" * 60)

print(f"\nX_train shape : {X_train.shape}")
print(f"X_test  shape : {X_test.shape}")

label_names = {0: "LOW", 1: "MEDIUM", 2: "HIGH"}

print("\nClass distribution — Training set:")
for label, count in y_train.value_counts().sort_index().items():
    print(f"  {label_names[label]:<8} ({label}) : {count}")

print("\nClass distribution — Test set:")
for label, count in y_test.value_counts().sort_index().items():
    print(f"  {label_names[label]:<8} ({label}) : {count}")

print("\nPreprocessing completed successfully.")
print(f"  Splits saved to : {DATASET_DIR}")
print(f"  Scaler saved to : {SCALER_PATH}")
