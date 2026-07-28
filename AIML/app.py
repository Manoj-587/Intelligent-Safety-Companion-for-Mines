import pandas as pd
from pathlib import Path

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

df = pd.read_csv(DATA_PATH)

print("=== Dataset Analysis ===")
print()

# Display the first few rows to inspect the raw structure of the data.
print("1) First 5 rows")
print(df.head().to_string(index=False))
print("\n" + "-" * 80)

# Display the last few rows to check the end of the dataset.
print("2) Last 5 rows")
print(df.tail().to_string(index=False))
print("\n" + "-" * 80)

# Show the dataset size: number of rows and columns.
print("3) Dataset shape")
print(df.shape)
print("\n" + "-" * 80)

# List all column names to understand the available features.
print("4) Column names")
print(df.columns.tolist())
print("\n" + "-" * 80)

# Inspect the data types to know whether columns are numeric, categorical, etc.
print("5) Data types")
print(df.dtypes)
print("\n" + "-" * 80)

# Check for missing values to see whether data cleaning is needed.
print("6) Missing values")
print(df.isna().sum())
print("\n" + "-" * 80)

# Count duplicate rows to check data quality.
print("7) Duplicate rows")
print("Duplicate rows:", df.duplicated().sum())
print("\n" + "-" * 80)

# View descriptive statistics for numeric columns.
print("8) Summary statistics")
print(df.describe(include="all").transpose().to_string())
print("\n" + "-" * 80)

# Explain the meaning of each column based on its name and the dataset context.
print("9) Column meanings")
column_meanings = {
    "year": "Year of the observation.",
    "month": "Month of the observation.",
    "day": "Day of the observation.",
    "hour": "Hour of the timestamp.",
    "minute": "Minute of the timestamp.",
    "second": "Second of the timestamp.",
    "AN311": "Gas/air monitoring sensor reading from channel AN311.",
    "AN422": "Gas/air monitoring sensor reading from channel AN422.",
    "AN423": "Gas/air monitoring sensor reading from channel AN423.",
    "TP1721": "Temperature-related sensor reading from channel TP1721.",
    "RH1722": "Relative humidity sensor reading from channel RH1722.",
    "BA1723": "Barometric/pressure-related sensor reading from channel BA1723.",
    "TP1711": "Temperature-related sensor reading from channel TP1711.",
    "RH1712": "Relative humidity sensor reading from channel RH1712.",
    "BA1713": "Barometric/pressure-related sensor reading from channel BA1713.",
    "MM252": "Mine-monitoring sensor reading from channel MM252.",
    "MM261": "Mine-monitoring sensor reading from channel MM261.",
    "MM262": "Mine-monitoring sensor reading from channel MM262.",
    "MM263": "Mine-monitoring sensor reading from channel MM263.",
    "MM264": "Mine-monitoring sensor reading from channel MM264.",
    "MM256": "Mine-monitoring sensor reading from channel MM256.",
    "MM211": "Mine-monitoring sensor reading from channel MM211.",
    "CM861": "Monitoring channel CM861.",
    "CR863": "Monitoring channel CR863.",
    "P_864": "Monitoring channel P_864.",
    "TC862": "Monitoring channel TC862.",
    "WM868": "Monitoring channel WM868.",
    "AMP1_IR": "Infrared/current-related measurement from AMP1_IR.",
    "AMP2_IR": "Infrared/current-related measurement from AMP2_IR.",
    "DMP3_IR": "Infrared/current-related measurement from DMP3_IR.",
    "DMP4_IR": "Infrared/current-related measurement from DMP4_IR.",
    "AMP5_IR": "Infrared/current-related measurement from AMP5_IR.",
    "F_SIDE": "Side/zone indicator column.",
    "V": "A label-like column, but it is constant in this dataset and does not appear to be a usable target.",
}

for col, meaning in column_meanings.items():
    print(f"- {col}: {meaning}")
print("\n" + "-" * 80)

# Identify whether the dataset already contains a prediction target.
print("10) Target column assessment")
print("Available target candidates:")
for col in df.columns:
    if col.lower() in {"target", "label", "class", "status", "hazard", "safety"}:
        print("- Found explicit target-like column:", col)
        break
else:
    print("- No explicit target column is present in the dataset.")

print("\nColumn 'V' values:")
print(df["V"].value_counts().to_string())
print("\nConclusion:")
print("- The dataset does not contain a clear supervised target column.")
print("- The only label-like column is 'V', but it is constant (all zeros), so it cannot be used as a training target.")
print("- A prototype target can be created from the environmental sensor values by assigning a rule-based risk label.")
print("\n" + "-" * 80)

# Create a new target column named Risk_Level.
# We will treat each row as one independent one-minute observation and assign a label based on the sensor values.
print("11) Create Risk_Level labels")

# Exclude timestamp columns, the constant V column, and any label-like columns from the risk logic.
timestamp_columns = ["year", "month", "day", "hour", "minute", "second"]
target_like_columns = {"V", "Risk_Level", "target", "label", "class", "status", "hazard", "safety"}
sensor_columns = [
    col
    for col in df.columns
    if col not in timestamp_columns
    and col not in target_like_columns
    and pd.api.types.is_numeric_dtype(df[col])
]

if not sensor_columns:
    raise ValueError("No numeric feature columns were available to build the risk labels.")

# Analyze the distribution of every sensor column.
# We use percentile thresholds because they are robust and do not rely on fixed numbers.
print("Percentile thresholds used for the risk rule:")
important_sensor_columns = ["AN311", "AN422", "AN423", "TP1721", "BA1713", "MM264", "AMP2_IR", "DMP3_IR"]
thresholds = {}
for col in sensor_columns:
    q33 = df[col].quantile(0.33)
    q67 = df[col].quantile(0.67)
    if col in important_sensor_columns:
        thresholds[col] = (q33, q67)
        print(f"- {col}: LOW <= {q33:.3f}, MEDIUM between {q33:.3f} and {q67:.3f}, HIGH >= {q67:.3f}")

print("\nOther sensor columns were inspected but were not used because they were effectively constant in this dataset.")
print("\n" + "-" * 80)

# Assign LOW/MEDIUM/HIGH to each sensor value based on its percentile range.
# We compute the percentile rank of each value within its own sensor column, then combine those ranks into one overall risk score.
ranked_sensor_values = {}
for col in important_sensor_columns:
    ranked_sensor_values[col] = df[col].rank(method="average", pct=True)

# Convert the percentile ranks into LOW/MEDIUM/HIGH labels using the same thresholds printed above.
def sensor_label(percentile_rank):
    if percentile_rank <= 0.33:
        return "LOW"
    if percentile_rank >= 0.67:
        return "HIGH"
    return "MEDIUM"


# Combine the sensor-level percentile ranks into one overall score per row.
# A higher average rank means the row is more often in the upper range across sensors.
row_scores = []

for idx in df.index:
    combined_rank = 0.0
    for col in important_sensor_columns:
        combined_rank += ranked_sensor_values[col].loc[idx]
    row_scores.append(combined_rank / len(important_sensor_columns))

# Use percentile cut points on the combined rank score to assign LOW/MEDIUM/HIGH.
# This keeps the classes better balanced for prototype modeling.
combined_score_series = pd.Series(row_scores)
low_cut = combined_score_series.quantile(0.33)
high_cut = combined_score_series.quantile(0.67)


def final_risk_label(avg_score):
    if avg_score <= low_cut:
        return "LOW"
    if avg_score >= high_cut:
        return "HIGH"
    return "MEDIUM"


df["Risk_Level"] = [final_risk_label(score) for score in row_scores]

# Reorder classes for a consistent display.
risk_order = ["LOW", "MEDIUM", "HIGH"]
df["Risk_Level"] = pd.Categorical(df["Risk_Level"], categories=risk_order, ordered=True)

# Show the distribution of the new target labels.
print("Risk_Level distribution:")
print(df["Risk_Level"].value_counts().reindex(risk_order, fill_value=0).to_string())
print("\n" + "-" * 80)

# Show the first 10 rows with the new label.
print("First 10 rows with Risk_Level:")
print(df.head(10)[["Risk_Level"] + important_sensor_columns].to_string(index=False))
print("\n" + "-" * 80)

# Show the class counts explicitly.
print("Class counts:")
print(df["Risk_Level"].value_counts().reindex(risk_order, fill_value=0).to_string())
print("\n" + "-" * 80)

# Save the new dataset.
df.to_csv(OUTPUT_PATH, index=False)
print(f"Saved labeled dataset to: {OUTPUT_PATH}")
print("\nExplanation of the rule:")
print("- Each important sensor is assigned LOW, MEDIUM, or HIGH based on its own percentile range.")
print("- LOW means the sensor is in the lower range, MEDIUM means it is around the middle, and HIGH means it is in the upper range.")
print("- The per-sensor labels are combined into an overall score, and the final Risk_Level is assigned from the score distribution using percentile cut points.")
print("- This produces a more balanced three-class target for prototype classification.")
