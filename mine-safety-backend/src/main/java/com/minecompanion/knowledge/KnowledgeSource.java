package com.minecompanion.knowledge;

import java.util.List;

/**
 * Extension point for knowledge document sources.
 *
 * KnowledgeBaseService depends only on this interface.
 * The document format and storage backend are completely hidden from
 * the rest of the application.
 *
 * Current implementation:
 *   MarkdownKnowledgeSource — loads .md files from classpath domain folders.
 *
 * Future implementations (no changes to KnowledgeBaseService or CompanionService):
 *   PDFKnowledgeSource      — extracts text from PDF manuals and DGMS guidelines.
 *   DatabaseKnowledgeSource — loads documents from a relational or document database.
 *   VectorKnowledgeSource   — semantic retrieval via embeddings (RAG pipeline).
 */
public interface KnowledgeSource {

    /**
     * Returns all documents available from this source.
     * Called once at startup by KnowledgeBaseService to build the in-memory index.
     *
     * @return all loaded KnowledgeDocument instances
     */
    List<KnowledgeDocument> loadAll();

    /**
     * Human-readable name of this source, used in logging and ChatResponse.sources.
     * Example: "MarkdownKnowledgeSource", "PDFKnowledgeSource"
     */
    String sourceName();
}
