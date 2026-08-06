package com.minecompanion.knowledge;

import com.minecompanion.exception.KnowledgeException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Retrieves relevant KnowledgeDocument instances for a given KnowledgeQuery.
 *
 * Retrieval priority chain (applied in order, results merged and ranked):
 *   1. Intent mapping  — maps the classified intent to a primary category
 *   2. Tag matching    — scores documents by tag overlap with query keywords
 *   3. Category filter — restricts to the mapped or hinted category
 *   4. Keyword search  — fallback full-text match on title and body
 *
 * Documents are ranked by: tag overlap score (desc) → priority field (asc).
 * The top maxResults documents are returned.
 *
 * This service depends only on the KnowledgeSource interface.
 * Replacing MarkdownKnowledgeSource with PDFKnowledgeSource or
 * VectorKnowledgeSource requires no changes here.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final List<KnowledgeSource> sources;   // all registered KnowledgeSource beans

    @Value("${knowledge.max-sections-per-query}")
    private int defaultMaxResults;

    /** In-memory index built at startup from all registered sources. */
    private List<KnowledgeDocument> index = new ArrayList<>();

    // ── Intent → Category mapping ──────────────────────────────────────────────
    // Keys match CompanionIntent enum names exactly.
    private static final Map<String, String> INTENT_CATEGORY_MAP = Map.of(
            "EMERGENCY_GUIDANCE",        "emergency",
            "EQUIPMENT_HELP",            "equipment",
            "PPE_GUIDANCE",              "ppe",
            "FIRST_AID",                 "first-aid",
            "RISK_EXPLANATION",          "general",
            "RECOMMENDATION_EXPLANATION","general",
            "SAFETY_QUESTION",           "general",
            "GENERAL_MINING",            "general"
    );

    // ── Startup ────────────────────────────────────────────────────────────────

    @PostConstruct
    public void buildIndex() {
        log.info("[KnowledgeBaseService] Building knowledge index from {} source(s)...", sources.size());

        List<KnowledgeDocument> all = new ArrayList<>();
        for (KnowledgeSource source : sources) {
            try {
                List<KnowledgeDocument> docs = source.loadAll();
                all.addAll(docs);
                log.info("[KnowledgeBaseService] Source '{}' contributed {} document(s).",
                        source.sourceName(), docs.size());
            } catch (KnowledgeException ex) {
                log.error("[KnowledgeBaseService] Failed to load from source '{}': {}",
                        source.sourceName(), ex.getMessage());
            }
        }

        this.index = Collections.unmodifiableList(all);
        log.info("[KnowledgeBaseService] Index built. Total documents: {}", index.size());

        // Log the full index summary at DEBUG level
        index.forEach(doc -> log.debug(
                "[KnowledgeBaseService] Indexed: id={} category={} priority={} audience={} lastUpdated={}",
                doc.getId(), doc.getCategory(), doc.getPriority(), doc.getAudience(), doc.getLastUpdated()));
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Retrieves the most relevant KnowledgeDocument instances for the given query.
     *
     * Applies the retrieval priority chain:
     *   1. Derive primary category from intent
     *   2. Score all documents by tag overlap with query keywords
     *   3. Filter by category (intent-derived or hinted)
     *   4. Fall back to keyword search on title + body if no tagged matches found
     *
     * @param query the retrieval query built by CompanionService
     * @return ordered list of relevant documents, up to query.maxResults
     */
    public List<KnowledgeDocument> retrieve(KnowledgeQuery query) {
        log.info("[KnowledgeBaseService] ▶ Knowledge Retrieval | intent={} keywords={} categoryHint={}",
                query.getIntent(), query.getKeywords(), query.getCategoryHint());

        int maxResults = query.getMaxResults() > 0 ? query.getMaxResults() : defaultMaxResults;

        // Step 1: Derive category from intent
        String category = resolveCategory(query);

        // Step 2: Score documents by tag overlap + audience match
        List<ScoredDocument> scored = scoreDocuments(query.getKeywords(), query.getUserRole());

        // Step 3: Filter by category — prefer category-matched documents first
        List<ScoredDocument> categoryMatches = scored.stream()
                .filter(sd -> category != null && sd.doc().getCategory().equalsIgnoreCase(category))
                .collect(Collectors.toList());

        // Step 4: If category filtering yields results, use them; otherwise fall back to keyword search
        List<KnowledgeDocument> results;
        if (!categoryMatches.isEmpty()) {
            results = rank(categoryMatches, maxResults);
            log.debug("[KnowledgeBaseService] Category+tag match: {} document(s) in category '{}'",
                    results.size(), category);
        } else {
            results = keywordFallback(query.getKeywords(), maxResults);
            log.debug("[KnowledgeBaseService] Keyword fallback: {} document(s) returned", results.size());
        }

        log.info("[KnowledgeBaseService] ◀ Knowledge Retrieval complete | returned={} docs={}",
                results.size(),
                results.stream().map(KnowledgeDocument::getId).collect(Collectors.joining(", ")));

        return results;
    }

    /**
     * Returns all documents in a specific category, sorted by priority.
     * Used by SafetyGuard to load emergency fallback content at startup.
     */
    public List<KnowledgeDocument> getByCategory(String category) {
        return index.stream()
                .filter(doc -> doc.getCategory().equalsIgnoreCase(category))
                .sorted(Comparator.comparingInt(KnowledgeDocument::getPriority))
                .collect(Collectors.toList());
    }

    /** Returns the total number of indexed documents. */
    public int indexSize() {
        return index.size();
    }

    // ── Retrieval Internals ────────────────────────────────────────────────────

    private String resolveCategory(KnowledgeQuery query) {
        if (query.getCategoryHint() != null && !query.getCategoryHint().isBlank()) {
            return query.getCategoryHint();
        }
        return INTENT_CATEGORY_MAP.getOrDefault(query.getIntent(), null);
    }

    /**
     * Scores every document by tag overlap with query keywords, then adds
     * an audience bonus when the document's audience list includes the user's role.
     *
     * Scoring:
     *   +1 per matching tag
     *   +2 audience bonus when userRole is in document.audience (preference, not filter)
     */
    private List<ScoredDocument> scoreDocuments(List<String> keywords, String userRole) {
        List<String> lowerKeywords = keywords == null ? List.of() :
                keywords.stream().map(String::toLowerCase).collect(Collectors.toList());

        String lowerRole = userRole == null ? "" : userRole.toLowerCase();

        return index.stream().map(doc -> {
            // Tag overlap score
            int tagScore = (int) doc.getTags().stream()
                    .map(String::toLowerCase)
                    .filter(lowerKeywords::contains)
                    .count();

            // Audience bonus — boosts documents targeted at this user's role
            int audienceBonus = (!lowerRole.isBlank() && doc.getAudience() != null
                    && doc.getAudience().stream().map(String::toLowerCase).anyMatch(lowerRole::equals))
                    ? 2 : 0;

            return new ScoredDocument(doc, tagScore + audienceBonus);
        }).collect(Collectors.toList());
    }

    /**
     * Ranks scored documents: tag score descending, then priority ascending.
     * Returns the top maxResults documents.
     */
    private List<KnowledgeDocument> rank(List<ScoredDocument> scored, int maxResults) {
        return scored.stream()
                .sorted(Comparator
                        .comparingInt(ScoredDocument::score).reversed()
                        .thenComparingInt(sd -> sd.doc().getPriority()))
                .limit(maxResults)
                .map(ScoredDocument::doc)
                .collect(Collectors.toList());
    }

    /**
     * Fallback: searches title and body text for any query keyword.
     * Returns documents sorted by priority.
     */
    private List<KnowledgeDocument> keywordFallback(List<String> keywords, int maxResults) {
        if (keywords == null || keywords.isEmpty()) {
            return index.stream()
                    .sorted(Comparator.comparingInt(KnowledgeDocument::getPriority))
                    .limit(maxResults)
                    .collect(Collectors.toList());
        }

        List<String> lowerKeywords = keywords.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        return index.stream()
                .filter(doc -> {
                    String searchable = (doc.getTitle() + " " + doc.getBody()).toLowerCase();
                    return lowerKeywords.stream().anyMatch(searchable::contains);
                })
                .sorted(Comparator.comparingInt(KnowledgeDocument::getPriority))
                .limit(maxResults)
                .collect(Collectors.toList());
    }

    // ── Internal record ────────────────────────────────────────────────────────

    private record ScoredDocument(KnowledgeDocument doc, int score) {}
}
