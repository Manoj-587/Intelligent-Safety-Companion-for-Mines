"""
Mine Safety Risk Prediction — Prediction Module
================================================
Loads the trained XGBoost model and the fitted StandardScaler,
then exposes predict_risk() — a function that accepts a sensor
reading dictionary and returns LOW / MEDIUM / HIGH.

Why the scaler is required here
--------------------------------
During preprocessing (app.py) all features were standardized with
StandardScaler before the model was trained.  At inference time the
same transformation must be applied to raw sensor values, otherwise
the model receives out-of-distribution inputs and produces wrong
predictions.  The scaler is saved by app.py and loaded here once at
module import time.

Why a named DataFrame must be passed to model.predict()
--------------------------------------------------------
XGBoost stores the feature names it was trained on inside the booster.
If predict() receives a plain numpy array or a DataFrame without column
names it raises:
    "data did not contain feature names, but the following fields are
     expected: ..."
Wrapping the scaled numpy array back into a pd.DataFrame with the
original FEATURE_COLUMNS list before calling predict() eliminates this
warning entirely.

This module is designed to be imported directly into a Flask REST API.
"""

import os
import math
import joblib
import pandas as pd

# ── Paths ──────────────────────────────────────────────────────────────────────
BASE_DIR    = os.path.dirname(os.path.abspath(__file__))
MODEL_PATH  = os.path.join(BASE_DIR, "model", "xgboost_model.pkl")
SCALER_PATH = os.path.join(BASE_DIR, "model", "scaler.pkl")

# ── Feature schema ─────────────────────────────────────────────────────────────
# Exact column order used during training — must never be changed.
FEATURE_COLUMNS = [
    "AN311", "AN422", "AN423",
    "TP1721", "RH1722", "BA1723",
    "TP1711", "RH1712", "BA1713",
    "MM252", "MM261", "MM262", "MM263", "MM264",
    "MM256", "MM211",
    "CM861", "CR863", "P_864", "TC862", "WM868",
    "AMP1_IR", "AMP2_IR", "DMP3_IR", "DMP4_IR", "AMP5_IR",
    "F_SIDE",
]

# Mapping from numeric model output to human-readable risk label.
LABEL_MAP = {0: "LOW", 1: "MEDIUM", 2: "HIGH"}

# ── Artifact loading ───────────────────────────────────────────────────────────

def _load_model():
    """Load the trained XGBoost model. Raises FileNotFoundError if missing."""
    if not os.path.exists(MODEL_PATH):
        raise FileNotFoundError(
            f"Model file not found at: {MODEL_PATH}\n"
            "Run train.py first to generate the model."
        )
    return joblib.load(MODEL_PATH)


def _load_scaler():
    """
    Load the fitted StandardScaler. Raises FileNotFoundError if missing.
    The scaler is produced by app.py — run it before starting the API.
    """
    if not os.path.exists(SCALER_PATH):
        raise FileNotFoundError(
            f"Scaler file not found at: {SCALER_PATH}\n"
            "Run app.py first to generate the scaler."
        )
    return joblib.load(SCALER_PATH)


# Load both artifacts once at module import time so every call to
# predict_risk() reuses the same in-memory objects.
_model  = _load_model()
_scaler = _load_scaler()

# ── Public API ─────────────────────────────────────────────────────────────────

def predict_risk(sensor_data: dict) -> str:
    """
    Predict the mine safety risk level from a single sensor reading.

    Parameters
    ----------
    sensor_data : dict
        Dictionary with exactly the 27 sensor feature keys and their
        corresponding numeric values.  Keys must match FEATURE_COLUMNS.

    Returns
    -------
    str
        Predicted risk level: "LOW", "MEDIUM", or "HIGH".

    Raises
    ------
    TypeError
        If sensor_data is not a dictionary.
    KeyError
        If one or more required feature keys are missing.
    ValueError
        If any feature value is non-numeric or non-finite (NaN / Infinity).
    """
    # ── Input type check ───────────────────────────────────────────────────────
    if not isinstance(sensor_data, dict):
        raise TypeError(
            f"sensor_data must be a dict, got {type(sensor_data).__name__}."
        )

    # ── Missing feature check ──────────────────────────────────────────────────
    missing = [col for col in FEATURE_COLUMNS if col not in sensor_data]
    if missing:
        raise KeyError(
            f"Missing required features: {missing}"
        )

    # ── Numeric and finite value check ─────────────────────────────────────────
    # float() alone silently accepts "NaN" and "Infinity" as valid strings.
    # math.isfinite() rejects both, preventing corrupt values from reaching
    # the model and causing unpredictable behaviour.
    for col in FEATURE_COLUMNS:
        try:
            value = float(sensor_data[col])
        except (TypeError, ValueError):
            raise ValueError(
                f"Feature '{col}' must be numeric, got: {sensor_data[col]!r}"
            )
        if not math.isfinite(value):
            raise ValueError(
                f"Feature '{col}' must be a finite number, got: {sensor_data[col]!r}"
            )

    # ── Build input DataFrame in the exact training column order ───────────────
    # pd.DataFrame([sensor_data], columns=FEATURE_COLUMNS) guarantees:
    #   1. Column names are preserved.
    #   2. Column order matches the training schema exactly.
    # Never use np.array(list(sensor_data.values())) or similar — both strip
    # column names and trigger the XGBoost feature-name warning.
    input_df = pd.DataFrame([sensor_data], columns=FEATURE_COLUMNS)

    # ── Apply the same scaling used during training ────────────────────────────
    # scaler.transform() returns a numpy array, so we immediately wrap it back
    # into a named DataFrame.  This ensures model.predict() always receives a
    # DataFrame with the correct feature names and never raises the warning:
    # "data did not contain feature names, but the following fields are expected"
    scaled_array = _scaler.transform(input_df)
    scaled_df    = pd.DataFrame(scaled_array, columns=FEATURE_COLUMNS)

    # ── Predict ────────────────────────────────────────────────────────────────
    numeric_prediction = int(_model.predict(scaled_df)[0])

    # ── Map to human-readable label ────────────────────────────────────────────
    return LABEL_MAP[numeric_prediction]

# ── Entry point for direct execution / smoke test ─────────────────────────────

def main():
    """
    Runs a sample prediction with representative sensor values.
    Execute this file directly to verify the full inference pipeline
    before integrating with the Flask API.
    """
    sample_sensor_data = {
        "AN311"  : 0.452,
        "AN422"  : 1.873,
        "AN423"  : 0.991,
        "TP1721" : 24.5,
        "RH1722" : 62,
        "BA1723" : 101.3,
        "TP1711" : 23.8,
        "RH1712" : 60,
        "BA1713" : 101.1,
        "MM252"  : 0.012,
        "MM261"  : 0.034,
        "MM262"  : 0.021,
        "MM263"  : 1,
        "MM264"  : 0.045,
        "MM256"  : 0.018,
        "MM211"  : 0.009,
        "CM861"  : 3,
        "CR863"  : 2,
        "P_864"  : 1,
        "TC862"  : 4,
        "WM868"  : 2,
        "AMP1_IR": 5,
        "AMP2_IR": 7,
        "DMP3_IR": 3,
        "DMP4_IR": 4,
        "AMP5_IR": 6,
        "F_SIDE" : 1,
    }

    risk_level = predict_risk(sample_sensor_data)

    print("=" * 50)
    print("Mine Safety Risk Prediction")
    print("=" * 50)
    print(f"Predicted Risk Level : {risk_level}")
    print("=" * 50)


if __name__ == "__main__":
    main()
