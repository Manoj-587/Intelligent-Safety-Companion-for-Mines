"""
Mine Safety Risk Prediction — Recommendation Engine
====================================================
Generates context-aware safety recommendations driven by sensor threshold
violations, not by keyword matching on reason strings.

Public contract (unchanged):
    generate_recommendations(predicted_risk, reasons, sensor_data) -> dict

The `reasons` parameter is kept for API compatibility and future use by the
Bayesian Network module.  Recommendations are generated solely from
`sensor_data` thresholds so they never contradict the explanations.
"""

from explain import (
    AN311_HIGH, AN422_HIGH, AN423_HIGH,
    TP1721_HIGH, TP1711_HIGH,
    RH1722_HIGH, RH1722_LOW, RH1712_HIGH, RH1712_LOW,
    BA1723_LOW, BA1713_LOW,
    MM264_HIGH, MM252_HIGH, MM261_HIGH, MM262_HIGH, MM263_HIGH, MM256_HIGH, MM211_HIGH,
    CM861_HIGH, CR863_HIGH, P_864_HIGH, TC862_HIGH, WM868_HIGH,
    AMP2_IR_HIGH, DMP3_IR_HIGH, AMP1_IR_HIGH, AMP5_IR_HIGH, DMP4_IR_HIGH,
)

# ── Sensor-triggered recommendation rules ─────────────────────────────────────
# Each entry: (condition_callable, list_of_recommendations)
# condition receives the sensor_data dict and returns True when abnormal.
# Add new rules here without touching generate_recommendations().

_SENSOR_RULES = [
    (
        lambda s: s["AN422"] >= AN422_HIGH or s["AN311"] >= AN311_HIGH or s["AN423"] >= AN423_HIGH,
        [
            "Stop all ignition sources immediately.",
            "Increase mine ventilation to dilute gas concentration.",
            "Check gas extraction and monitoring systems.",
        ],
    ),
    (
        lambda s: s["TP1721"] >= TP1721_HIGH or s["TP1711"] >= TP1711_HIGH,
        [
            "Reduce equipment load on affected mining machinery immediately.",
            "Inspect mine ventilation, cooling and pressure control systems.",
            "Monitor temperature continuously until levels stabilise.",
        ],
    ),
    (
        lambda s: s["RH1722"] >= RH1722_HIGH or s["RH1712"] >= RH1712_HIGH,
        [
            "Inspect moisture control systems and drainage in the affected zone.",
            "Check for water ingress and leakage at identified locations.",
        ],
    ),
    (
        lambda s: s["RH1722"] <= RH1722_LOW or s["RH1712"] <= RH1712_LOW,
        [
            "Increase humidity control to reduce dust ignition risk.",
            "Inspect ventilation for excessive drying.",
        ],
    ),
    (
        lambda s: s["BA1713"] <= BA1713_LOW or s["BA1723"] <= BA1723_LOW,
        [
            "Inspect mine ventilation, cooling and pressure control systems.",
            "Check for abnormal underground structural conditions.",
        ],
    ),
    (
        lambda s: s["MM264"] >= MM264_HIGH or s["MM252"] >= MM252_HIGH
                  or s["MM261"] >= MM261_HIGH or s["MM262"] >= MM262_HIGH
                  or s["MM263"] >= MM263_HIGH or s["MM256"] >= MM256_HIGH
                  or s["MM211"] >= MM211_HIGH,
        [
            "Suspend heavy machinery operations in the affected zone.",
            "Inspect structural integrity of mine supports and roof.",
            "Inspect rotating machinery for bearing wear and misalignment.",
            "Schedule maintenance for affected mechanical equipment.",
            "Monitor ground movement continuously.",
        ],
    ),
    (
        lambda s: s["CM861"] >= CM861_HIGH or s["CR863"] >= CR863_HIGH
                  or s["P_864"] >= P_864_HIGH or s["TC862"] >= TC862_HIGH
                  or s["WM868"] >= WM868_HIGH,
        [
            "Reduce equipment load on affected mining machinery immediately.",
            "Inspect mining equipment (crusher, conveyor and pump) for overload, blockage, leaks and abnormal operation.",
            "Isolate faulty equipment and notify the maintenance team.",
        ],
    ),
    (
        lambda s: s["AMP2_IR"] >= AMP2_IR_HIGH or s["AMP1_IR"] >= AMP1_IR_HIGH
                  or s["AMP5_IR"] >= AMP5_IR_HIGH or s["DMP3_IR"] >= DMP3_IR_HIGH
                  or s["DMP4_IR"] >= DMP4_IR_HIGH,
        [
            "Isolate overheating electrical equipment immediately.",
            "Inspect electrical equipment, panels and cable insulation for overheating, abnormal heat signatures and electrical faults.",
            "Monitor hotspot progression until equipment is cleared.",
        ],
    ),
]

# ── Severity-ordered category definitions ───────────────────────────────────────────
# Maps each raw recommendation string to its action category and prefix tag.
# Category order mirrors the severity order used in explain.py.
_REC_CATEGORIES = [
    # (tag, prefix_label, keywords_that_identify_this_category)
    # Immediate — actions required right now before conditions worsen
    ("gas_stop",    "[Immediate]",  ["stop all ignition"]),
    ("gas_vent",    "[Immediate]",  ["increase mine ventilation", "gas extraction"]),
    ("ir_isolate",  "[Immediate]",  ["isolate overheating", "isolate faulty"]),
    ("equip_load",  "[Immediate]",  ["reduce equipment load"]),
    ("humidity_ig", "[Immediate]",  ["humidity control to reduce dust ignition"]),
    # Inspection — equipment or structural checks
    ("electrical",  "[Inspection]", ["electrical equipment, panels", "electrical equipment for"]),
    ("equipment",   "[Inspection]", ["mining equipment", "crusher", "conveyor", "pump"]),
    ("ventilation", "[Inspection]", ["mine ventilation, cooling", "cooling and pressure"]),
    ("moisture",    "[Inspection]", ["moisture control", "water ingress", "excessive drying"]),
    ("structural",  "[Inspection]", ["structural integrity", "structural conditions", "bearing wear", "mechanical equipment"]),
    ("heavy_mach",  "[Inspection]", ["heavy machinery", "maintenance for affected"]),
    # Monitoring — ongoing observation
    ("mon_ground",  "[Monitoring]", ["monitor ground"]),
    ("mon_temp",    "[Monitoring]", ["monitor temperature"]),
    ("mon_hotspot", "[Monitoring]", ["monitor hotspot"]),
    ("normal",      "[Monitoring]", ["normal mining", "routine environmental", "standard safety"]),
]


def _prefix_recommendations(raw: list) -> list:
    """
    Assign each raw recommendation string a severity-ordered prefix tag
    ([Immediate], [Inspection], or [Monitoring]) and return the list
    sorted by category severity.  Duplicates are removed.

    This function is the only place that controls recommendation presentation.
    _SENSOR_RULES and generate_recommendations() logic are completely untouched.
    """
    seen = set()
    # Collect (sort_index, prefixed_string) pairs.
    tagged = []
    for item in raw:
        if item in seen:
            continue
        seen.add(item)
        lower = item.lower()
        matched = False
        for idx, (_, prefix, keywords) in enumerate(_REC_CATEGORIES):
            if any(kw in lower for kw in keywords):
                tagged.append((idx, f"{prefix} {item}"))
                matched = True
                break
        if not matched:
            tagged.append((len(_REC_CATEGORIES), f"[Monitoring] {item}"))

    tagged.sort(key=lambda x: x[0])
    return [text for _, text in tagged]


# ── Default recommendations when all sensors are normal ───────────────────────
_ALL_NORMAL = [
    "Continue normal mining operations.",
    "Maintain routine environmental monitoring.",
    "Follow standard safety procedures.",
]

# ── Public API ─────────────────────────────────────────────────────────────────

def generate_recommendations(predicted_risk: str, reasons: list, sensor_data: dict = None) -> dict:
    """
    Generate deduplicated, severity-sorted, prefixed safety recommendations.

    When sensor_data is provided, recommendations are driven exclusively by
    threshold violations so they never contradict the explanation reasons.
    Each recommendation is prefixed with its action category:
        [Immediate]  — critical actions required right now
        [Inspection] — equipment or structural checks required
        [Monitoring] — ongoing observation actions

    When sensor_data is absent (e.g. legacy callers), falls back to the
    all-normal defaults.

    Parameters
    ----------
    predicted_risk : str   — "LOW", "MEDIUM", or "HIGH" (kept for compatibility).
    reasons        : list  — explanation strings (kept for compatibility).
    sensor_data    : dict  — sensor feature values keyed by column name.

    Returns
    -------
    dict  { "recommendations": [...] }
    """
    if not sensor_data:
        return {"recommendations": _prefix_recommendations(list(_ALL_NORMAL))}

    raw = []
    seen_raw = set()

    for condition, recs in _SENSOR_RULES:
        try:
            if condition(sensor_data):
                for rec in recs:
                    _add_unique(raw, seen_raw, rec)
        except (KeyError, TypeError, ValueError):
            continue

    if not raw:
        return {"recommendations": _prefix_recommendations(list(_ALL_NORMAL))}

    return {"recommendations": _prefix_recommendations(raw)}


# ── Internal helper ────────────────────────────────────────────────────────────

def _add_unique(lst: list, seen: set, item: str) -> None:
    if item not in seen:
        seen.add(item)
        lst.append(item)
