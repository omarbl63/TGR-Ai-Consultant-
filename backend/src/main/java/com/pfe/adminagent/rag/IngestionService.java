package com.pfe.adminagent.rag;

import com.pfe.adminagent.rag.domain.RagDocument;
import com.pfe.adminagent.rag.dto.RagDocumentDto;
import com.pfe.adminagent.rag.dto.ReindexResult;
import com.pfe.adminagent.rag.repository.RagDocumentRepository;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Turns source documents into searchable vector chunks:
 * parse → chunk → embed (bge-m3) → store in pgvector, with citation metadata.
 */
@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);
    private static final List<String> SUPPORTED_EXTENSIONS = List.of(".md", ".markdown", ".txt", ".pdf", ".docx");

    private final RagDocumentRepository documentRepository;
    private final DocumentParsingService parsingService;
    private final DocumentSplitter splitter;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    public IngestionService(RagDocumentRepository documentRepository,
                            DocumentParsingService parsingService,
                            DocumentSplitter splitter,
                            EmbeddingModel embeddingModel,
                            EmbeddingStore<TextSegment> embeddingStore) {
        this.documentRepository = documentRepository;
        this.parsingService = parsingService;
        this.splitter = splitter;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    /** Result of an ingest attempt: the document plus whether it was freshly created. */
    public record IngestOutcome(RagDocumentDto document, boolean created) {
    }

    /**
     * Ingests a single document. Idempotent by content checksum: an already
     * indexed identical file is skipped and its existing record returned.
     */
    @Transactional
    public RagDocumentDto ingest(byte[] content, String filename, String contentType,
                                 String category, UUID uploadedBy) {
        return ingestWithOutcome(content, filename, contentType, category, uploadedBy).document();
    }

    @Transactional
    public IngestOutcome ingestWithOutcome(byte[] content, String filename, String contentType,
                                           String category, UUID uploadedBy) {
        String checksum = sha256(content);
        var existing = documentRepository.findByChecksum(checksum);
        if (existing.isPresent()) {
            log.info("Skipping already-ingested document '{}' (checksum match)", filename);
            return new IngestOutcome(RagDocumentDto.from(existing.get()), false);
        }

        RagDocument record = new RagDocument(filename, checksum);
        record.setContentType(contentType);
        record.setSizeBytes(content.length);
        record.setCategory(category);
        record.setUploadedBy(uploadedBy);

        try {
            String text = parsingService.extractText(content, filename);
            if (text == null || text.isBlank()) {
                throw new IllegalArgumentException("No extractable text in document");
            }
            String title = firstNonBlank(parsingService.extractTitle(text), filename);
            String docRef = parsingService.extractDocRef(text);
            record.setTitle(title);
            record.setDocRef(docRef);
            documentRepository.save(record);

            Metadata metadata = buildMetadata(record.getId(), docRef, title, category, filename);
            Document document = Document.from(text, metadata);
            List<TextSegment> segments = splitter.split(document);

            List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
            embeddingStore.addAll(embeddings, segments);

            record.markIndexed(segments.size());
            documentRepository.save(record);
            log.info("Indexed '{}' -> {} chunks (ref={}, category={})", filename, segments.size(), docRef, category);
            return new IngestOutcome(RagDocumentDto.from(record), true);
        } catch (Exception ex) {
            log.error("Failed to ingest document '{}': {}", filename, ex.getMessage());
            record.markFailed(ex.getMessage());
            documentRepository.save(record);
            return new IngestOutcome(RagDocumentDto.from(record), true);
        }
    }

    /**
     * Walks the configured knowledge-base directory and ingests every supported
     * file. Category is derived from the immediate parent folder name.
     */
    public ReindexResult reindexKnowledgeBase(String basePath, UUID triggeredBy) {
        Path root = Path.of(basePath);
        if (!Files.isDirectory(root)) {
            throw new IllegalStateException("Knowledge-base path is not a directory: " + basePath);
        }

        List<RagDocumentDto> results = new ArrayList<>();
        int found = 0, ingested = 0, skipped = 0, failed = 0;

        try (Stream<Path> stream = Files.walk(root)) {
            List<Path> files = stream.filter(Files::isRegularFile).filter(this::isSupported).toList();
            for (Path file : files) {
                found++;
                try {
                    byte[] bytes = Files.readAllBytes(file);
                    String category = categoryOf(root, file);
                    IngestOutcome outcome = ingestWithOutcome(bytes, file.getFileName().toString(),
                            probeContentType(file), category, triggeredBy);
                    RagDocumentDto dto = outcome.document();
                    results.add(dto);
                    if (!outcome.created()) {
                        skipped++;
                    } else if (dto.status() == com.pfe.adminagent.rag.domain.DocumentStatus.FAILED) {
                        failed++;
                    } else {
                        ingested++;
                    }
                } catch (IOException e) {
                    failed++;
                    log.error("Could not read file {}: {}", file, e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to walk knowledge-base directory", e);
        }
        return new ReindexResult(found, ingested, skipped, failed, results);
    }

    private Metadata buildMetadata(UUID documentId, String docRef, String title, String category, String source) {
        Map<String, String> map = new HashMap<>();
        map.put(RagMetadataKeys.DOCUMENT_ID, documentId.toString());
        if (docRef != null) map.put(RagMetadataKeys.DOC_REF, docRef);
        if (title != null) map.put(RagMetadataKeys.TITLE, title);
        if (category != null) map.put(RagMetadataKeys.CATEGORY, category);
        if (source != null) map.put(RagMetadataKeys.SOURCE, source);
        return Metadata.from(map);
    }

    private boolean isSupported(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return SUPPORTED_EXTENSIONS.stream().anyMatch(name::endsWith);
    }

    private String categoryOf(Path root, Path file) {
        Path parent = file.getParent();
        if (parent == null || parent.equals(root)) {
            return "general";
        }
        return parent.getFileName().toString();
    }

    private String probeContentType(Path file) {
        try {
            String ct = Files.probeContentType(file);
            return ct != null ? ct : "application/octet-stream";
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }

    private static String firstNonBlank(String a, String b) {
        return (a != null && !a.isBlank()) ? a : b;
    }

    private static String sha256(byte[] content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(content);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
