package com.minecompanion.knowledge;

import com.minecompanion.exception.KnowledgeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Loads KnowledgeDocument instances from Markdown files stored under
 * src/main/resources/knowledge/ and its domain subfolders.
 *
 * Document format — each .md file must begin with a YAML front-matter block:
 *
 *   ---
 *   id:       gas-methane
 *   title:    Methane Gas — Properties and Hazards
 *   category: gas
 *   tags:     [methane, CH4, gas, explosion, flammable, LEL]
 *   priority: 1
 *   version:  1.0
 *   ---
 *
 *   # Methane Gas
 *   Body content follows here...
 *
 * The front-matter block is stripped before the body is stored.
 * All files are loaded once at startup — KnowledgeBaseService indexes them.
 *
 * To add a new knowledge source (PDF, DB, Vector), implement KnowledgeSource
 * and register it as a Spring bean. No changes here are required.
 */
@Slf4j
@Component
public class MarkdownKnowledgeSource implements KnowledgeSource {

    @Value("${knowledge.base-path}")
    private String basePath;

    @Override
    public String sourceName() {
        return "MarkdownKnowledgeSource";
    }

    @Override
    public List<KnowledgeDocument> loadAll() {
        log.info("[MarkdownKnowledgeSource] Loading knowledge documents from classpath:{}", basePath);

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources;

        try {
            // Recursively match all .md files under all domain subfolders
            resources = resolver.getResources("classpath:" + basePath + "**/*.md");
        } catch (Exception ex) {
            throw new KnowledgeException("Failed to scan knowledge base directory: " + basePath, ex);
        }

        if (resources.length == 0) {
            log.warn("[MarkdownKnowledgeSource] No .md files found under classpath:{}", basePath);
            return List.of();
        }

        List<KnowledgeDocument> documents = new ArrayList<>();
        for (Resource resource : resources) {
            try {
                KnowledgeDocument doc = parseDocument(resource);
                documents.add(doc);
                log.debug("[MarkdownKnowledgeSource] Loaded: id={} category={} tags={}",
                        doc.getId(), doc.getCategory(), doc.getTags());
            } catch (Exception ex) {
                // Log and skip malformed documents — do not abort the entire load
                log.warn("[MarkdownKnowledgeSource] Skipping malformed document '{}': {}",
                        resource.getFilename(), ex.getMessage());
            }
        }

        log.info("[MarkdownKnowledgeSource] Loaded {} document(s) from {} file(s).",
                documents.size(), resources.length);
        return List.copyOf(documents);
    }

    // ── Parsing ────────────────────────────────────────────────────────────────

    private KnowledgeDocument parseDocument(Resource resource) throws Exception {
        String raw;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            raw = reader.lines().collect(Collectors.joining("\n"));
        }

        // Split front-matter from body
        if (!raw.startsWith("---")) {
            throw new KnowledgeException("Document '" + resource.getFilename()
                    + "' is missing the required YAML front-matter block.");
        }

        int secondDelimiter = raw.indexOf("---", 3);
        if (secondDelimiter == -1) {
            throw new KnowledgeException("Document '" + resource.getFilename()
                    + "' has an unclosed YAML front-matter block.");
        }

        String frontMatter = raw.substring(3, secondDelimiter).trim();
        String body        = raw.substring(secondDelimiter + 3).trim();

        // Parse front-matter fields
        String id          = extractField(frontMatter, "id",          resource.getFilename());
        String title       = extractField(frontMatter, "title",       resource.getFilename());
        String category    = extractField(frontMatter, "category",    resource.getFilename());
        String version     = extractField(frontMatter, "version",     resource.getFilename());
        String lastUpdated = extractField(frontMatter, "lastUpdated", resource.getFilename());
        int    priority    = Integer.parseInt(extractField(frontMatter, "priority", resource.getFilename()));
        List<String> tags     = extractInlineList(frontMatter, "tags");
        List<String> audience = extractInlineList(frontMatter, "audience");

        return KnowledgeDocument.builder()
                .id(id)
                .title(title)
                .category(category)
                .tags(tags)
                .audience(audience)
                .priority(priority)
                .version(version)
                .lastUpdated(lastUpdated)
                .body(body)
                .build();
    }

    /**
     * Extracts a scalar YAML field value by key.
     * Handles both quoted and unquoted values.
     */
    private String extractField(String frontMatter, String key, String filename) {
        for (String line : frontMatter.split("\n")) {
            if (line.trim().startsWith(key + ":")) {
                String value = line.substring(line.indexOf(':') + 1).trim();
                // Strip surrounding quotes if present
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                return value;
            }
        }
        throw new KnowledgeException("Document '" + filename + "' is missing required field: " + key);
    }

    /**
     * Extracts a YAML inline list by key: field: [a, b, c]
     * Reused for both tags and audience fields.
     */
    private List<String> extractInlineList(String frontMatter, String key) {
        for (String line : frontMatter.split("\n")) {
            if (line.trim().startsWith(key + ":")) {
                String value = line.substring(line.indexOf(':') + 1).trim();
                if (value.startsWith("[") && value.endsWith("]")) {
                    String inner = value.substring(1, value.length() - 1);
                    return Arrays.stream(inner.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toList());
                }
            }
        }
        return List.of();
    }
}
