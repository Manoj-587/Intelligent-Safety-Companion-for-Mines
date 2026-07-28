"""
Mine Safety Risk Prediction — Recommendation Engine
====================================================
Generates actionable safety recommendations by combining:
  1. General recommendations based on the predicted risk level.
  2. Dynamic recommendations derived from the explanation reasons.

Architecture note — future compatibility
-----------------------------------------
This module consumes only two inputs:
    predicted_risk  (str)   — "LOW", "MEDIUM", or "HIGH"
    reasons         (list)  — list of explanation strings

It does NOT depend on how those inputs were produced.  Whether the
reasons come from the current Rule-Based Explanation Engine or a future
Bayesian Network, this module requires zero changes.  The public
contract:

    generate_recommendations(predicted_risk, reasons) -> dict

must remain identical.
"""

# ── General recommendations by risk level ─────────────────────────────────────
# These are always included regardless of the specific reasons.
# Ordered from least to most urgent to match the risk escalation model.

_GENERAL_RECOMMENDATIONS = {
    "LOW": [
        "Continue normal mining operations.",
        "Maintain routine environmental monitoring.",
        "Follow standard safety procedures.",
    ],
    "MEDIUM": [
        "Increase monitoring frequency.",
        "Inspect mine ventilation systems.",
        "Reduce worker exposure in the affected zone.",
        "Inform the site supervisor immediately.",
    ],
    "HIGH": [
        "Evacuate the affected area immediately.",
        "Stop all drilling and blasting operations.",
        "Notify the mine supervisor and emergency response team.",
        "Increase ventilation immediately.",
        "Restrict worker access until conditions normalize.",
    ],
}

# ── Dynamic recommendation rules ──────────────────────────────────────────────
# Each entry maps a keyword (case-insensitive substring of a reason string)
# to a list of additional recommendations triggered when that keyword appears
# in any of the explanation reasons.
#
# Design principle: adding a new trigger requires only a new dict entry.
# The evaluation loop in generate_recommendations() never needs to change.

_DYNAMIC_RECOMMENDATIONS = {
    "gas": [
        "Increase mine ventilation to dilute gas concentration.",
        "Check gas extraction and monitoring systems.",
        "Stop all ignition sources immediately.",
    ],
    "temperature": [
        "Inspect cooling and ventilation equipment.",
        "Reduce equipment load to lower heat generation.",
        "Monitor temperature continuously until levels stabilize.",
    ],
    "humidity": [
        "Check moisture control and dehumidification systems.",
        "Inspect for water leakage in the affected zone.",
        "Improve drainage if necessary.",
    ],
    "oxygen": [
        "Inspect oxygen supply systems immediately.",
        "Verify ventilation effectiveness in the affected area.",
        "Remove workers from low-oxygen zones without delay.",
    ],
    "pressure": [
        "Inspect pressure monitoring equipment.",
        "Check for abnormal underground structural conditions.",
    ],
    "vibration": [
        "Suspend heavy machinery operations in the affected zone.",
        "Inspect structural integrity of mine supports.",
        "Monitor ground movement continuously.",
    ],
    "infrared": [
        "Inspect electrical equipment for overheating.",
        "Check cable insulation and connections in the affected zone.",
    ],
}

# ── Public API ─────────────────────────────────────────────────────────────────

def generate_recommendations(predicted_risk: str, reasons: list) -> dict:
    """
    Generate a deduplicated list of safety recommendations.

    Combines general risk-level recommendations with dynamic recommendations
    triggered by keywords found in the explanation reasons.

    Parameters
    ----------
    predicted_risk : str
        Risk level from the prediction engine: "LOW", "MEDIUM", or "HIGH".
    reasons : list[str]
        Explanation strings produced by the explanation engine.
        This function is agnostic to how these strings were generated.

    Returns
    -------
    dict
        {
            "recommendations": [
                "Evacuate the affected area immediately.",
                "Increase mine ventilation to dilute gas concentration.",
                ...
            ]
        }
    """
    # ── 1. Start with general recommendations for the risk level ──────────────
    # Use a list to preserve insertion order (important for operator readability).
    # A seen set tracks duplicates without disrupting order.
    recommendations = []
    seen = set()

    for rec in _GENERAL_RECOMMENDATIONS.get(predicted_risk, []):
        _add_unique(recommendations, seen, rec)

    # ── 2. Add dynamic recommendations based on reason keywords ───────────────
    # Join all reasons into one lowercase string for a single-pass keyword scan.
    # This avoids O(n*m) nested loops and handles multi-word reasons cleanly.
    combined_reasons = " ".join(reasons).lower()

    for keyword, dynamic_recs in _DYNAMIC_RECOMMENDATIONS.items():
        if keyword in combined_reasons:
            for rec in dynamic_recs:
                _add_unique(recommendations, seen, rec)

    # ── 3. Fallback — should never be reached in normal operation ─────────────
    if not recommendations:
        recommendations.append("Follow standard mine safety protocols.")

    return {"recommendations": recommendations}


# ── Internal helper ────────────────────────────────────────────────────────────

def _add_unique(recommendations: list, seen: set, item: str) -> None:
    """
    Append item to recommendations only if it has not been added before.
    Preserves insertion order while guaranteeing no duplicates.
    """
    if item not in seen:
        seen.add(item)
        recommendations.append(item)
