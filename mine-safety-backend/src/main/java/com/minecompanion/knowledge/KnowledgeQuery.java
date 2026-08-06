package com.minecompanion.knowledge;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Encapsulates all signals used to retrieve relevant knowledge documents.
 *
 * KnowledgeBaseService applies these signals in priority order:
 *   1. intent    — maps directly to a category (e.g. EMERGENCY_GUIDANCE → "emergency")
 *   2. tags      — matches documents whose tag list overlaps with these terms
 *   3. category  — filters by domain folder when intent mapping is ambiguous
 *   4. keywords  — fallback full-text search across document body and title
 *
 * Built by CompanionService after IntentClassifier runs.
 */
@Data
@Builder
public class KnowledgeQuery {

    /**
     * The classified intent string, e.g. "EMERGENCY_GUIDANCE", "MINE_KNOWLEDGE".
     * Used to derive the primary category filter.
     */
    private String intent;

    /**
     * Keywords extracted from the user message.
     * Used for tag matching and fallback keyword search.
     * Example: ["methane", "leak", "gas"]
     */
    private List<String> keywords;

    /**
     * The role of the user making the request.
     * Used to boost documents whose audience list includes this role.
     * Supported values: worker | supervisor | maintenance | safety_officer
     */
    private String userRole;

    /**
     * Optional explicit category override.
     * When set, restricts retrieval to documents in this category only.
     */
    private String categoryHint;

    /**
     * Maximum number of documents to return.
     * Defaults to the value of knowledge.max-sections-per-query in application.yml.
     */
    @Builder.Default
    private int maxResults = 3;
}
