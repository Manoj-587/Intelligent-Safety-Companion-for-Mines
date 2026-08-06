package com.minecompanion.knowledge;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Represents a single knowledge document loaded from any KnowledgeSource.
 *
 * Metadata fields are parsed from the YAML front-matter block at the top
 * of each Markdown file. The body field contains the remaining document
 * content after the front-matter is stripped.
 *
 * This class is source-agnostic — MarkdownKnowledgeSource, PDFKnowledgeSource,
 * and VectorKnowledgeSource all produce KnowledgeDocument instances.
 * KnowledgeBaseService consumes only this type.
 */
@Data
@Builder
public class KnowledgeDocument {

    /** Unique identifier for this document, e.g. "gas-methane". */
    private String id;

    /** Human-readable title, e.g. "Methane Gas — Properties and Hazards". */
    private String title;

    /**
     * Domain category this document belongs to.
     * Matches the folder name: gas | emergency | equipment | ppe |
     * maintenance | procedures | first-aid | general
     */
    private String category;

    /**
     * Tags used for intent-driven retrieval.
     * Example: ["methane", "CH4", "gas", "explosion", "flammable"]
     */
    private List<String> tags;

    /**
     * Intended audience for this document.
     * Used by KnowledgeBaseService to boost documents that match the user's role.
     * Supported values: worker | supervisor | maintenance | safety_officer
     * A document with audience [worker, supervisor] is relevant to both roles.
     */
    private List<String> audience;

    /**
     * Priority used to rank documents when multiple match a query.
     * 1 = highest priority (e.g. emergency procedures).
     * Higher numbers = lower priority.
     */
    private int priority;

    /** Document version string, e.g. "1.0". */
    private String version;

    /**
     * ISO-8601 date of the last content update, e.g. "2024-01-15".
     * Supports future document versioning, auditing, and staleness checks.
     */
    private String lastUpdated;

    /**
     * The full document body text after front-matter is stripped.
     * Injected into the LLM prompt as the knowledge context section.
     */
    private String body;
}
