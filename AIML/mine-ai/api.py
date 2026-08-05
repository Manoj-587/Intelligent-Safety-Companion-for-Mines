"""
Mine Safety Risk Prediction — Flask REST API
=============================================
Exposes two endpoints:

    GET  /          — Health check
    POST /predict   — Accepts 27 sensor values, returns predicted risk level
                      and a list of human-readable explanation reasons.

Response schema (POST /predict):
    {
        "predicted_risk": "HIGH",
        "reasons": [
            "Gas concentration on channel AN422 has exceeded the safe operating limit.",
            "Temperature at sensor TP1721 is above the normal operating range."
        ]
    }

Designed for integration with a Spring Boot backend via HTTP.
"""

import os
import math
import logging
from flask import Flask, request, jsonify
from flask_cors import CORS

from predict import predict_risk, FEATURE_COLUMNS
from explain import generate_explanation
from recommend import generate_recommendations

# ── Logging ────────────────────────────────────────────────────────────────────
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s  [%(levelname)s]  %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
logger = logging.getLogger(__name__)

# ── Application ────────────────────────────────────────────────────────────────
app = Flask(__name__)

# Enable CORS so the React frontend and Spring Boot backend can call this API
# from a different origin without browser restrictions.
CORS(app)

# ── Validation helper ──────────────────────────────────────────────────────────

def _validate_request(body: dict) -> tuple:
    """
    Validate the incoming JSON request body.

    Checks performed in order:
      1. All 27 required feature keys are present.
      2. Every value is numeric (int or float).
      3. Every value is finite — rejects NaN and Infinity.
         float() alone silently accepts the strings "NaN" and "Infinity",
         which would corrupt the DataFrame and cause XGBoost to produce
         wrong predictions.  math.isfinite() blocks both.

    Returns
    -------
    (sensor_data, None)    — validation passed; sensor_data is a clean dict.
    (None, error_message)  — validation failed; error_message describes why.
    """
    # ── 1. Missing fields ──────────────────────────────────────────────────────
    missing = [col for col in FEATURE_COLUMNS if col not in body]
    if missing:
        return None, f"Missing required fields: {missing}"

    # ── 2 & 3. Numeric and finite check ───────────────────────────────────────
    invalid = []
    for col in FEATURE_COLUMNS:
        try:
            value = float(body[col])
            if not math.isfinite(value):
                invalid.append(col)
        except (TypeError, ValueError):
            invalid.append(col)

    if invalid:
        return None, f"Non-numeric or non-finite values in fields: {invalid}"

    # Build a clean dict with only the expected features in the correct order.
    # Explicit ordering here mirrors FEATURE_COLUMNS so the DataFrame built
    # inside predict_risk() always matches the training schema exactly.
    sensor_data = {col: float(body[col]) for col in FEATURE_COLUMNS}
    return sensor_data, None

# ── Routes ─────────────────────────────────────────────────────────────────────

@app.route("/", methods=["GET"])
def health_check():
    """
    Health check endpoint.
    Used by Spring Boot or any monitoring tool to confirm the API is alive.
    """
    logger.info("Health check requested.")
    return jsonify({"message": "Mine Safety Risk Prediction API is running."}), 200


@app.route("/predict", methods=["POST"])
def predict():
    """
    Prediction endpoint.

    Accepts a JSON body with all 27 sensor feature values.
    Calls predict_risk() to get the XGBoost classification, then calls
    generate_explanation() to produce human-readable reasons.

    The explanation engine (explain.py) is intentionally decoupled from
    the prediction engine (predict.py).  Replacing the rule-based engine
    with a Bayesian Network requires changes only to explain.py — this
    route and the response schema remain unchanged.

    Success  (200):
        {
            "predicted_risk": "HIGH",
            "reasons": ["...", "..."]
        }
    Error    (400): { "error": "<validation message>" }
    Error    (500): { "error": "<server message>" }
    """
    # ── Parse request body ─────────────────────────────────────────────────────
    body = request.get_json(silent=True)

    if body is None:
        logger.warning("Request received with empty or non-JSON body.")
        return jsonify({"error": "Request body is empty or not valid JSON."}), 400

    logger.info("Prediction request received with %d field(s).", len(body))

    # ── Validate ───────────────────────────────────────────────────────────────
    sensor_data, error_message = _validate_request(body)

    if error_message:
        logger.warning("Validation failed: %s", error_message)
        return jsonify({"error": error_message}), 400

    # ── Predict ────────────────────────────────────────────────────────────────
    try:
        # Step 1: Get the risk level from the XGBoost model.
        # predict_risk() is completely unmodified — it knows nothing about
        # explanations and returns only "LOW", "MEDIUM", or "HIGH".
        risk_level = predict_risk(sensor_data)
        logger.info("Prediction successful: %s", risk_level)

        # Step 2: Generate explanation reasons from the explanation engine.
        # Swapping explain.py for a Bayesian Network changes only this call.
        explanation = generate_explanation(sensor_data, risk_level)
        logger.info("Explanation generated with %d reason(s).", len(explanation["reasons"]))

        # Step 3: Generate safety recommendations.
        # generate_recommendations() consumes only predicted_risk and reasons
        # so it is fully decoupled from both the prediction and explanation
        # engines. Replacing either never affects this call.
        rec_result = generate_recommendations(risk_level, explanation["reasons"], sensor_data)
        logger.info("Recommendations generated: %d item(s).", len(rec_result["recommendations"]))

        response = {
            "predicted_risk":  explanation["predicted_risk"],
            "reasons":         explanation["reasons"],
            "recommendations": rec_result["recommendations"],
        }

        return jsonify(response), 200

    except FileNotFoundError as exc:
        # Model or scaler file missing — server-side configuration issue.
        logger.error("Artifact not found: %s", exc)
        return jsonify({"error": "Model artifact not found. Contact the administrator."}), 500

    except (KeyError, TypeError, ValueError) as exc:
        # Data issue that slipped past the validation layer.
        logger.error("Prediction input error: %s", exc)
        return jsonify({"error": str(exc)}), 400

    except Exception as exc:
        # Catch-all for any unforeseen runtime errors.
        logger.exception("Unexpected error during prediction: %s", exc)
        return jsonify({"error": "An unexpected error occurred. Please try again."}), 500

# ── Entry point ────────────────────────────────────────────────────────────────

if __name__ == "__main__":
    # Read configuration from environment variables so this file never needs
    # to change between development and production deployments.
    #
    # Development  : set API_HOST=0.0.0.0 to expose on all interfaces.
    # Production   : leave API_HOST unset (defaults to 127.0.0.1) and place
    #                a reverse proxy (nginx / Spring Cloud Gateway) in front.
    # Debug mode   : set FLASK_DEBUG=true only in local development.
    host  = os.environ.get("API_HOST",    "127.0.0.1")
    port  = int(os.environ.get("API_PORT", "5000"))
    debug = os.environ.get("FLASK_DEBUG", "false").lower() == "true"

    logger.info(
        "Starting Mine Safety Risk Prediction API on %s:%d  debug=%s",
        host, port, debug,
    )
    app.run(host=host, port=port, debug=debug)
