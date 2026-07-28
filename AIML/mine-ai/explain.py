"""
Mine Safety Risk Prediction — Rule-Based Explanation Engine
============================================================
Prototype placeholder for the Bayesian Network explanation module.

Architecture note — future compatibility
-----------------------------------------
This file is the ONLY module that needs to change when the rule-based
engine is replaced with a real Bayesian Network.  The public contract:

    generate_explanation(sensor_data: dict, predicted_risk: str) -> dict

must remain identical.  Flask API (api.py), Spring Boot, and React
consume only the returned dict and are completely unaware of how the
explanations are produced internally.

Sensor channel mapping (based on dataset column names)
-------------------------------------------------------
AN311, AN422, AN423   — Airborne gas / air quality sensors
TP1721, TP1711        — Temperature sensors
RH1722, RH1712        — Relative humidity sensors
BA1723, BA1713        — Barometric pressure sensors
MM252..MM264, MM256,
MM211                 — Mine monitoring sensors (vibration / movement)
CM861, CR863, P_864,
TC862, WM868          — Control / environmental monitoring channels
AMP1_IR..AMP5_IR,
DMP3_IR, DMP4_IR      — Infrared / current amplitude sensors
F_SIDE                — Zone / side indicator
"""

# ── Safety threshold constants ─────────────────────────────────────────────────
# These values represent domain-informed safe operating limits.
# Adjust them to match actual mine safety standards for your deployment.

# Gas / air quality thresholds
AN311_HIGH   = 1.5    # Elevated gas reading on channel AN311
AN422_HIGH   = 2.0    # Elevated gas reading on channel AN422
AN423_HIGH   = 1.5    # Elevated gas reading on channel AN423

# Temperature thresholds (degrees Celsius)
TP1721_HIGH  = 30.0   # High temperature at sensor TP1721
TP1711_HIGH  = 30.0   # High temperature at sensor TP1711

# Relative humidity thresholds (%)
RH1722_HIGH  = 85     # Excessively high humidity at RH1722
RH1722_LOW   = 30     # Excessively low humidity at RH1722
RH1712_HIGH  = 85     # Excessively high humidity at RH1712
RH1712_LOW   = 30     # Excessively low humidity at RH1712

# Barometric pressure thresholds (hPa)
BA1723_LOW   = 99.0   # Abnormally low pressure at BA1723
BA1713_LOW   = 99.0   # Abnormally low pressure at BA1713

# Mine monitoring thresholds (vibration / movement units)
MM264_HIGH   = 0.08   # Elevated mine movement on MM264
MM252_HIGH   = 0.05   # Elevated mine movement on MM252

# Infrared / current amplitude thresholds
AMP2_IR_HIGH = 10     # High current reading on AMP2_IR
DMP3_IR_HIGH = 8      # High current reading on DMP3_IR
AMP1_IR_HIGH = 10     # High current reading on AMP1_IR
AMP5_IR_HIGH = 10     # High current reading on AMP5_IR

# ── Rule definitions ───────────────────────────────────────────────────────────
# Each rule is a tuple of:
#   (condition_callable, explanation_string, applicable_risk_levels)
#
# condition_callable : receives sensor_data dict, returns True if rule fires.
# explanation_string : human-readable reason shown to the operator.
# applicable_risk_levels : set of risk levels for which this rule is active.
#
# Design principle: rules are data, not code.  Adding a new rule requires
# only a new tuple entry — no changes to generate_explanation() logic.

_RULES = [
    # ── Gas / air quality ──────────────────────────────────────────────────────
    (
        lambda s: float(s["AN422"]) >= AN422_HIGH,
        "Gas concentration on channel AN422 has exceeded the safe operating limit.",
        {"MEDIUM", "HIGH"},
    ),
    (
        lambda s: float(s["AN311"]) >= AN311_HIGH,
        "Gas concentration on channel AN311 has exceeded the safe operating limit.",
        {"MEDIUM", "HIGH"},
    ),
    (
        lambda s: float(s["AN423"]) >= AN423_HIGH,
        "Gas concentration on channel AN423 has exceeded the safe operating limit.",
        {"HIGH"},
    ),

    # ── Temperature ────────────────────────────────────────────────────────────
    (
        lambda s: float(s["TP1721"]) >= TP1721_HIGH,
        "Temperature at sensor TP1721 is above the normal operating range.",
        {"MEDIUM", "HIGH"},
    ),
    (
        lambda s: float(s["TP1711"]) >= TP1711_HIGH,
        "Temperature at sensor TP1711 is above the normal operating range.",
        {"MEDIUM", "HIGH"},
    ),

    # ── Humidity ───────────────────────────────────────────────────────────────
    (
        lambda s: float(s["RH1722"]) >= RH1722_HIGH,
        "Relative humidity at RH1722 is excessively high, increasing risk of equipment failure.",
        {"MEDIUM", "HIGH"},
    ),
    (
        lambda s: float(s["RH1722"]) <= RH1722_LOW,
        "Relative humidity at RH1722 is critically low, increasing risk of dust ignition.",
        {"MEDIUM", "HIGH"},
    ),
    (
        lambda s: float(s["RH1712"]) >= RH1712_HIGH,
        "Relative humidity at RH1712 is excessively high, increasing risk of equipment failure.",
        {"HIGH"},
    ),
    (
        lambda s: float(s["RH1712"]) <= RH1712_LOW,
        "Relative humidity at RH1712 is critically low, increasing risk of dust ignition.",
        {"HIGH"},
    ),

    # ── Barometric pressure ────────────────────────────────────────────────────
    (
        lambda s: float(s["BA1713"]) <= BA1713_LOW,
        "Barometric pressure at BA1713 has dropped below the safe threshold, indicating potential ventilation issues.",
        {"MEDIUM", "HIGH"},
    ),
    (
        lambda s: float(s["BA1723"]) <= BA1723_LOW,
        "Barometric pressure at BA1723 has dropped below the safe threshold.",
        {"HIGH"},
    ),

    # ── Mine movement / vibration ──────────────────────────────────────────────
    (
        lambda s: float(s["MM264"]) >= MM264_HIGH,
        "Mine movement sensor MM264 has detected abnormal vibration levels.",
        {"MEDIUM", "HIGH"},
    ),
    (
        lambda s: float(s["MM252"]) >= MM252_HIGH,
        "Mine movement sensor MM252 has detected abnormal activity.",
        {"HIGH"},
    ),

    # ── Infrared / current amplitude ───────────────────────────────────────────
    (
        lambda s: float(s["AMP2_IR"]) >= AMP2_IR_HIGH,
        "Infrared current sensor AMP2_IR is reading above the safe threshold.",
        {"MEDIUM", "HIGH"},
    ),
    (
        lambda s: float(s["DMP3_IR"]) >= DMP3_IR_HIGH,
        "Infrared current sensor DMP3_IR is reading above the safe threshold.",
        {"HIGH"},
    ),
    (
        lambda s: float(s["AMP1_IR"]) >= AMP1_IR_HIGH,
        "Infrared current sensor AMP1_IR is reading above the safe threshold.",
        {"HIGH"},
    ),

    # ── LOW risk informational rules ───────────────────────────────────────────
    (
        lambda s: float(s["AN422"]) < AN422_HIGH and float(s["AN311"]) < AN311_HIGH,
        "Gas concentrations are within safe limits.",
        {"LOW"},
    ),
    (
        lambda s: float(s["TP1721"]) < TP1721_HIGH and float(s["TP1711"]) < TP1711_HIGH,
        "Temperature readings are within the normal operating range.",
        {"LOW"},
    ),
    (
        lambda s: RH1722_LOW < float(s["RH1722"]) < RH1722_HIGH,
        "Humidity levels are within the acceptable range.",
        {"LOW"},
    ),
]

# ── Public API ─────────────────────────────────────────────────────────────────

def generate_explanation(sensor_data: dict, predicted_risk: str) -> dict:
    """
    Generate a human-readable explanation for a predicted risk level.

    Evaluates each rule against the sensor readings and collects the
    explanation strings for all rules that fire and are applicable to
    the predicted risk level.

    Parameters
    ----------
    sensor_data : dict
        Dictionary of sensor feature names to their numeric values.
        Must contain all 27 keys defined in FEATURE_COLUMNS (predict.py).
    predicted_risk : str
        The risk level predicted by the XGBoost model: "LOW", "MEDIUM",
        or "HIGH".

    Returns
    -------
    dict
        {
            "predicted_risk": "HIGH",
            "reasons": [
                "Gas concentration on channel AN422 has exceeded ...",
                "Temperature at sensor TP1721 is above ..."
            ]
        }

    Future replacement
    ------------------
    To swap this engine for a real Bayesian Network, replace only the
    body of this function.  The return schema must remain unchanged.
    """
    reasons = []

    for condition, explanation, applicable_levels in _RULES:
        # Only evaluate rules that are relevant to the predicted risk level.
        if predicted_risk not in applicable_levels:
            continue
        try:
            if condition(sensor_data):
                reasons.append(explanation)
        except (KeyError, TypeError, ValueError):
            # Skip any rule that cannot be evaluated due to a missing or
            # malformed sensor value — do not let a single bad rule crash
            # the entire explanation.
            continue

    # If no rule fired, return a generic fallback message so the response
    # always contains at least one reason.
    if not reasons:
        reasons.append("No significant contributing factor identified based on current sensor readings.")

    return {
        "predicted_risk": predicted_risk,
        "reasons": reasons,
    }
