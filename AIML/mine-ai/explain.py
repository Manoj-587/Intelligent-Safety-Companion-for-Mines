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
AN311_HIGH   = 4.0    # Elevated gas reading on channel AN311 (calibrated: dataset max 3.7, threshold set above max)
AN422_HIGH   = 2.0    # Elevated gas reading on channel AN422
AN423_HIGH   = 1.6    # Elevated gas reading on channel AN423 (calibrated: dataset max 1.5, threshold set above max)

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
MM264_HIGH   = 0.25   # Elevated mine movement on MM264 (calibrated: dataset max 0.2, threshold set above max)
MM252_HIGH   = 0.15   # Elevated mine movement on MM252 (calibrated: dataset constant 0.1, threshold set above constant)
MM261_HIGH   = 0.15   # Elevated vibration / movement on MM261 (calibrated: dataset constant 0.1, threshold set above constant)
MM262_HIGH   = 0.15   # Elevated vibration / movement on MM262 (calibrated: dataset constant 0.1, threshold set above constant)
MM263_HIGH   = 0.06   # Elevated vibration / movement on MM263
MM256_HIGH   = 0.40   # Elevated vibration / movement on MM256 (calibrated: dataset constant 0.3, threshold set above constant)
MM211_HIGH   = 0.90   # Elevated vibration / movement on MM211 (calibrated: dataset constant 0.7, threshold set above constant)

# Control / equipment monitoring thresholds
CM861_HIGH   = 5.0    # Elevated reading on control monitor CM861
CR863_HIGH   = 5.0    # Elevated reading on crusher / conveyor monitor CR863
P_864_HIGH   = 6.0    # Elevated pump pressure reading on P_864
TC862_HIGH   = 5.0    # Elevated reading on conveyor / equipment monitor TC862
WM868_HIGH   = 5.0    # Elevated reading on winder / motor monitor WM868

# Infrared / current amplitude thresholds
AMP2_IR_HIGH = 10     # High current reading on AMP2_IR
DMP3_IR_HIGH = 8      # High current reading on DMP3_IR
AMP1_IR_HIGH = 10     # High current reading on AMP1_IR
AMP5_IR_HIGH = 10     # High current reading on AMP5_IR
DMP4_IR_HIGH = 8      # High current reading on DMP4_IR

# ── Categorical feature — intentionally excluded from explainability ───────────
# F_SIDE is a categorical mine zone / side identifier (integer-encoded).
# It carries no physical measurement and has no meaningful safety threshold.
# It is passed to XGBoost as a positional feature but must never appear in
# explanation rules or recommendation conditions.
# Do NOT add threshold constants or rules for F_SIDE.

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
    (
        lambda s: float(s["MM261"]) >= MM261_HIGH,
        "Mine movement sensor MM261 has detected abnormal vibration, indicating possible bearing or structural wear.",
        {"MEDIUM", "HIGH"},
    ),
    (
        lambda s: float(s["MM262"]) >= MM262_HIGH,
        "Mine movement sensor MM262 has detected abnormal vibration, indicating possible bearing or structural wear.",
        {"MEDIUM", "HIGH"},
    ),
    (
        lambda s: float(s["MM263"]) >= MM263_HIGH,
        "Mine movement sensor MM263 has detected abnormal vibration, indicating possible bearing or structural wear.",
        {"HIGH"},
    ),
    (
        lambda s: float(s["MM256"]) >= MM256_HIGH,
        "Mine movement sensor MM256 has detected elevated activity, suggesting abnormal ground or equipment movement.",
        {"MEDIUM", "HIGH"},
    ),
    (
        lambda s: float(s["MM211"]) >= MM211_HIGH,
        "Mine movement sensor MM211 has detected elevated activity, suggesting abnormal ground or equipment movement.",
        {"HIGH"},
    ),

    # ── Control / equipment monitoring ────────────────────────────────────────
    (
        lambda s: float(s["CM861"]) >= CM861_HIGH,
        "Control monitor CM861 is reading above the safe operating limit, indicating abnormal equipment behaviour.",
        {"MEDIUM", "HIGH"},
    ),
    (
        lambda s: float(s["CR863"]) >= CR863_HIGH,
        "Crusher / conveyor monitor CR863 is reading above the safe threshold, indicating possible mechanical overload.",
        {"MEDIUM", "HIGH"},
    ),
    (
        lambda s: float(s["P_864"]) >= P_864_HIGH,
        "Pump pressure monitor P_864 has exceeded the safe operating limit, indicating abnormal pump pressure.",
        {"MEDIUM", "HIGH"},
    ),
    (
        lambda s: float(s["TC862"]) >= TC862_HIGH,
        "Conveyor / equipment monitor TC862 is reading above the safe threshold, indicating possible conveyor misalignment or overload.",
        {"HIGH"},
    ),
    (
        lambda s: float(s["WM868"]) >= WM868_HIGH,
        "Winder / motor monitor WM868 has exceeded the safe operating limit, indicating motor overload or mechanical fault.",
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
    (
        lambda s: float(s["AMP5_IR"]) >= AMP5_IR_HIGH,
        "Infrared current sensor AMP5_IR is reading above the safe threshold, indicating possible equipment overheating.",
        {"MEDIUM", "HIGH"},
    ),
    (
        lambda s: float(s["DMP4_IR"]) >= DMP4_IR_HIGH,
        "Infrared current sensor DMP4_IR is reading above the safe threshold, indicating possible hotspot or electrical fault.",
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

# ── Presentation helpers ───────────────────────────────────────────────────────

# Severity order for reason categories — critical hazards first.
_CATEGORY_ORDER = ["Gas", "Infrared", "Equipment", "Temperature", "Pressure", "Humidity", "Mechanical", "Normal"]

# Maps each raw explanation keyword to its sensor category.
# Used by _categorise() to bucket fired reasons before grouping.
_CATEGORY_KEYWORDS = {
    "Gas":         ["AN311", "AN422", "AN423", "gas concentration", "gas concentrations"],
    "Infrared":    ["AMP1_IR", "AMP2_IR", "AMP5_IR", "DMP3_IR", "DMP4_IR", "infrared"],
    "Equipment":   ["CM861", "CR863", "P_864", "TC862", "WM868", "control monitor", "crusher", "pump", "conveyor", "winder"],
    "Temperature": ["TP1721", "TP1711", "temperature"],
    "Pressure":    ["BA1723", "BA1713", "barometric", "pressure"],
    "Humidity":    ["RH1722", "RH1712", "humidity"],
    "Mechanical":  ["MM252", "MM261", "MM262", "MM263", "MM264", "MM256", "MM211", "mine movement", "vibration"],
    "Normal":      ["within safe", "within the normal", "within the acceptable"],
}


def _categorise(reason: str) -> str:
    """Return the category name for a single reason string."""
    lower = reason.lower()
    for category, keywords in _CATEGORY_KEYWORDS.items():
        if any(kw.lower() in lower for kw in keywords):
            return category
    return "Normal"


def _list_sensors(reason: str, candidates: list) -> list:
    """Return every sensor name from candidates that appears in reason."""
    return [s for s in candidates if s in reason]


def _join_sensors(sensors: list) -> str:
    """Format a sensor list as 'A, B and C'."""
    if len(sensors) == 1:
        return sensors[0]
    return ", ".join(sensors[:-1]) + " and " + sensors[-1]


def _group_and_sort_reasons(raw_reasons: list) -> list:
    """
    Group raw per-sensor reason strings into concise category-level sentences
    and return them sorted by severity (critical hazards first).

    This function is the only place that controls presentation.  The _RULES
    list and generate_explanation() logic are completely untouched.
    """
    # Bucket raw reasons by category.
    buckets: dict = {cat: [] for cat in _CATEGORY_ORDER}
    for reason in raw_reasons:
        buckets[_categorise(reason)].append(reason)

    grouped = []

    # ── Gas ───────────────────────────────────────────────────────────────────
    gas = buckets["Gas"]
    if gas:
        # Separate abnormal from normal informational messages.
        abnormal = [r for r in gas if "within" not in r.lower()]
        normal   = [r for r in gas if "within" in r.lower()]
        if abnormal:
            sensors = _list_sensors(" ".join(abnormal), ["AN311", "AN422", "AN423"])
            if sensors:
                grouped.append(
                    f"High gas concentrations detected on sensor{'s' if len(sensors) > 1 else ''} "
                    f"{_join_sensors(sensors)}, indicating a potential gas hazard."
                )
            else:
                grouped.extend(abnormal)
        grouped.extend(normal)

    # ── Infrared ──────────────────────────────────────────────────────────────
    ir = buckets["Infrared"]
    if ir:
        sensors = _list_sensors(" ".join(ir), ["AMP1_IR", "AMP2_IR", "AMP5_IR", "DMP3_IR", "DMP4_IR"])
        if sensors:
            grouped.append(
                f"Abnormal infrared / current readings detected on sensor{'s' if len(sensors) > 1 else ''} "
                f"{_join_sensors(sensors)}, indicating possible overheating or electrical fault."
            )
        else:
            grouped.extend(ir)

    # ── Equipment ─────────────────────────────────────────────────────────────
    eq = buckets["Equipment"]
    if eq:
        sensors = _list_sensors(" ".join(eq), ["CM861", "CR863", "P_864", "TC862", "WM868"])
        if sensors:
            grouped.append(
                f"Equipment monitor{'s' if len(sensors) > 1 else ''} {_join_sensors(sensors)} "
                f"exceeded safe operating limits, indicating possible mechanical overload or fault."
            )
        else:
            grouped.extend(eq)

    # ── Temperature ───────────────────────────────────────────────────────────
    temp = buckets["Temperature"]
    if temp:
        abnormal = [r for r in temp if "within" not in r.lower()]
        normal   = [r for r in temp if "within" in r.lower()]
        if abnormal:
            sensors = _list_sensors(" ".join(abnormal), ["TP1721", "TP1711"])
            if sensors:
                grouped.append(
                    f"Elevated temperature detected on sensor{'s' if len(sensors) > 1 else ''} "
                    f"{_join_sensors(sensors)}, exceeding the safe operating range."
                )
            else:
                grouped.extend(abnormal)
        grouped.extend(normal)

    # ── Pressure ──────────────────────────────────────────────────────────────
    pres = buckets["Pressure"]
    if pres:
        sensors = _list_sensors(" ".join(pres), ["BA1723", "BA1713"])
        if sensors:
            grouped.append(
                f"Barometric pressure has dropped below the safe threshold on sensor{'s' if len(sensors) > 1 else ''} "
                f"{_join_sensors(sensors)}, indicating potential ventilation issues."
            )
        else:
            grouped.extend(pres)

    # ── Humidity ──────────────────────────────────────────────────────────────
    hum = buckets["Humidity"]
    if hum:
        abnormal = [r for r in hum if "within" not in r.lower()]
        normal   = [r for r in hum if "within" in r.lower()]
        if abnormal:
            high_sensors = _list_sensors(" ".join(abnormal), ["RH1722", "RH1712"])
            high = [r for r in abnormal if "high" in r.lower() or "excessively" in r.lower()]
            low  = [r for r in abnormal if "low" in r.lower() or "critically" in r.lower()]
            if high and high_sensors:
                grouped.append(
                    f"Excessively high humidity detected on sensor{'s' if len(high_sensors) > 1 else ''} "
                    f"{_join_sensors(high_sensors)}, increasing risk of equipment failure."
                )
            if low:
                low_sensors = _list_sensors(" ".join(low), ["RH1722", "RH1712"])
                if low_sensors:
                    grouped.append(
                        f"Critically low humidity detected on sensor{'s' if len(low_sensors) > 1 else ''} "
                        f"{_join_sensors(low_sensors)}, increasing risk of dust ignition."
                    )
        grouped.extend(normal)

    # ── Mechanical ────────────────────────────────────────────────────────────
    mech = buckets["Mechanical"]
    if mech:
        sensors = _list_sensors(" ".join(mech), ["MM252", "MM261", "MM262", "MM263", "MM264", "MM256", "MM211"])
        if sensors:
            grouped.append(
                f"Abnormal vibration detected by mine movement sensor{'s' if len(sensors) > 1 else ''} "
                f"({_join_sensors(sensors)}), indicating possible bearing wear or structural instability."
            )
        else:
            grouped.extend(mech)

    # ── Normal informational ──────────────────────────────────────────────────
    grouped.extend(buckets["Normal"])

    return grouped if grouped else ["No significant contributing factor identified based on current sensor readings."]


# ── Public API ─────────────────────────────────────────────────────────────────

def generate_explanation(sensor_data: dict, predicted_risk: str) -> dict:
    """
    Generate a human-readable explanation for a predicted risk level.

    Evaluates each rule against the sensor readings and collects the
    explanation strings for all rules that fire and are applicable to
    the predicted risk level.  Raw per-sensor strings are then grouped
    into concise category-level sentences and sorted by severity.

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
                "High gas concentrations detected on sensors AN311 and AN422 ...",
                "Elevated temperature detected on sensor TP1721 ..."
            ]
        }

    Future replacement
    ------------------
    To swap this engine for a real Bayesian Network, replace only the
    body of this function.  The return schema must remain unchanged.
    """
    raw_reasons = []

    for condition, explanation, applicable_levels in _RULES:
        # Only evaluate rules that are relevant to the predicted risk level.
        if predicted_risk not in applicable_levels:
            continue
        try:
            if condition(sensor_data):
                raw_reasons.append(explanation)
        except (KeyError, TypeError, ValueError):
            # Skip any rule that cannot be evaluated due to a missing or
            # malformed sensor value — do not let a single bad rule crash
            # the entire explanation.
            continue

    return {
        "predicted_risk": predicted_risk,
        "reasons": _group_and_sort_reasons(raw_reasons),
    }
