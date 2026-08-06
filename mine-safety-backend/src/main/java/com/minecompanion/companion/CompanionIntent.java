package com.minecompanion.companion;

/**
 * All business intents the AI Safety Companion can classify.
 *
 * Each intent carries:
 *   categoryHint — the knowledge base category most relevant to this intent.
 *                  Passed directly into KnowledgeQuery so retrieval is intent-driven.
 *   label        — human-readable name returned in ChatResponse.intent.
 *
 * Adding a new intent:
 *   1. Add an enum constant here with its categoryHint and label.
 *   2. Add one or more IntentRule entries in IntentClassifier.
 *   That is all. CompanionService routing reads the intent value directly —
 *   no switch statements or if-chains need to change.
 */
public enum CompanionIntent {

    /** User is asking a general mine safety question. */
    SAFETY_QUESTION("general", "Safety Question"),

    /** User wants to understand why the current risk level was predicted. */
    RISK_EXPLANATION("general", "Risk Explanation"),

    /** User wants to understand a specific safety recommendation. */
    RECOMMENDATION_EXPLANATION("general", "Recommendation Explanation"),

    /** User is reporting or asking about an active emergency. */
    EMERGENCY_GUIDANCE("emergency", "Emergency Guidance"),

    /** User is asking about a specific piece of mining equipment. */
    EQUIPMENT_HELP("equipment", "Equipment Help"),

    /** User is asking about personal protective equipment. */
    PPE_GUIDANCE("ppe", "PPE Guidance"),

    /** User is asking about first aid treatment. */
    FIRST_AID("first-aid", "First Aid"),

    /** User is asking a general mining knowledge question. */
    GENERAL_MINING("general", "General Mining"),

    /** User message is conversational and unrelated to mining (e.g. greetings). */
    SMALL_TALK("general", "Small Talk"),

    /** Intent could not be determined from the message. */
    UNKNOWN("general", "Unknown");

    private final String categoryHint;
    private final String label;

    CompanionIntent(String categoryHint, String label) {
        this.categoryHint = categoryHint;
        this.label        = label;
    }

    public String getCategoryHint() { return categoryHint; }
    public String getLabel()        { return label; }
}
