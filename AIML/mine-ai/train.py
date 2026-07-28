"""
Mine Safety Risk Prediction — Model Training
============================================
Trains an XGBoost classifier on the preprocessed splits,
evaluates performance, performs 5-fold cross-validation,
displays feature importance, and saves the trained model.
"""

import os
import joblib
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns

from xgboost import XGBClassifier
from sklearn.model_selection import StratifiedKFold, cross_validate
from sklearn.metrics import (
    accuracy_score,
    precision_score,
    recall_score,
    f1_score,
    classification_report,
    confusion_matrix,
)

# ── Paths ──────────────────────────────────────────────────────────────────────
BASE_DIR    = os.path.dirname(os.path.abspath(__file__))
DATASET_DIR = os.path.join(BASE_DIR, "dataset")
MODEL_DIR   = os.path.join(BASE_DIR, "model")
MODEL_PATH  = os.path.join(MODEL_DIR, "xgboost_model.pkl")

CLASS_NAMES = ["LOW", "MEDIUM", "HIGH"]

# ── 1. Load preprocessed splits ───────────────────────────────────────────────
X_train = pd.read_csv(os.path.join(DATASET_DIR, "X_train.csv"))
X_test  = pd.read_csv(os.path.join(DATASET_DIR, "X_test.csv"))
y_train = pd.read_csv(os.path.join(DATASET_DIR, "y_train.csv")).squeeze()
y_test  = pd.read_csv(os.path.join(DATASET_DIR, "y_test.csv")).squeeze()

print("=" * 60)
print("DATA LOADED")
print("=" * 60)
print(f"  X_train : {X_train.shape}  |  y_train : {y_train.shape}")
print(f"  X_test  : {X_test.shape}   |  y_test  : {y_test.shape}")

# ── 2. Define and train the XGBoost classifier ────────────────────────────────
model = XGBClassifier(
    n_estimators=200,
    max_depth=6,
    learning_rate=0.1,
    subsample=0.8,
    colsample_bytree=0.8,
    eval_metric="mlogloss",
    random_state=42,
    n_jobs=-1,
)

print("\n" + "=" * 60)
print("TRAINING")
print("=" * 60)
model.fit(X_train, y_train)
print("  XGBoost training complete.")

# ── 3. Predictions ────────────────────────────────────────────────────────────
y_pred = model.predict(X_test)

# ── 4. Evaluation metrics ─────────────────────────────────────────────────────
accuracy  = accuracy_score(y_test, y_pred)
precision = precision_score(y_test, y_pred, average="weighted", zero_division=0)
recall    = recall_score(y_test, y_pred, average="weighted", zero_division=0)
f1        = f1_score(y_test, y_pred, average="weighted", zero_division=0)

print("\n" + "=" * 60)
print("EVALUATION METRICS")
print("=" * 60)
print(f"  Accuracy  : {accuracy:.4f}")
print(f"  Precision : {precision:.4f}  (weighted)")
print(f"  Recall    : {recall:.4f}  (weighted)")
print(f"  F1-Score  : {f1:.4f}  (weighted)")

# ── 5. Classification report ──────────────────────────────────────────────────
print("\n" + "=" * 60)
print("CLASSIFICATION REPORT")
print("=" * 60)
print(classification_report(y_test, y_pred, target_names=CLASS_NAMES, zero_division=0))

# ── 6. Confusion matrix ───────────────────────────────────────────────────────
cm = confusion_matrix(y_test, y_pred)

print("=" * 60)
print("CONFUSION MATRIX")
print("=" * 60)
cm_df = pd.DataFrame(cm, index=CLASS_NAMES, columns=CLASS_NAMES)
cm_df.index.name   = "Actual"
cm_df.columns.name = "Predicted"
print(cm_df.to_string())

# Plot and save confusion matrix
plt.figure(figsize=(6, 5))
sns.heatmap(cm_df, annot=True, fmt="d", cmap="Blues")
plt.title("Confusion Matrix — XGBoost")
plt.tight_layout()
plt.savefig(os.path.join(MODEL_DIR, "confusion_matrix.png"), dpi=150)
plt.close()
print("\n  Confusion matrix plot saved to model/confusion_matrix.png")

# ── 7. 5-Fold Stratified Cross-Validation ─────────────────────────────────────
print("\n" + "=" * 60)
print("5-FOLD CROSS-VALIDATION  (on full training set)")
print("=" * 60)

cv = StratifiedKFold(n_splits=5, shuffle=True, random_state=42)
cv_results = cross_validate(
    model, X_train, y_train, cv=cv,
    scoring=["accuracy", "precision_weighted", "recall_weighted", "f1_weighted"],
    n_jobs=-1,
)

cv_metrics = {
    "Accuracy" : cv_results["test_accuracy"],
    "Precision": cv_results["test_precision_weighted"],
    "Recall"   : cv_results["test_recall_weighted"],
    "F1-Score" : cv_results["test_f1_weighted"],
}

for metric, scores in cv_metrics.items():
    print(f"  {metric:<10}: {scores.round(4)}  ->  mean={scores.mean():.4f}  std={scores.std():.4f}")

# ── 8. Feature importance ─────────────────────────────────────────────────────
print("\n" + "=" * 60)
print("FEATURE IMPORTANCE  (top 15)")
print("=" * 60)

importance_df = (
    pd.DataFrame({"Feature": X_train.columns, "Importance": model.feature_importances_})
    .sort_values("Importance", ascending=False)
    .reset_index(drop=True)
)

print(importance_df.head(15).to_string(index=False))

# Plot and save feature importance
plt.figure(figsize=(8, 6))
top15 = importance_df.head(15)
sns.barplot(x="Importance", y="Feature", hue="Feature", data=top15, palette="viridis", legend=False)
plt.title("Top 15 Feature Importances — XGBoost")
plt.tight_layout()
plt.savefig(os.path.join(MODEL_DIR, "feature_importance.png"), dpi=150)
plt.close()
print("\n  Feature importance plot saved to model/feature_importance.png")

# ── 9. Save the trained model ─────────────────────────────────────────────────
os.makedirs(MODEL_DIR, exist_ok=True)
joblib.dump(model, MODEL_PATH)

print("\n" + "=" * 60)
print("MODEL SAVED")
print("=" * 60)
print(f"  Path : {MODEL_PATH}")
print("\nTraining pipeline completed successfully.")
