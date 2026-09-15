package com.pfe.adminagent.rag.web;

import com.pfe.adminagent.config.AiProperties;
import com.pfe.adminagent.rag.IngestionService;
import com.pfe.adminagent.rag.RetrievalService;
import com.pfe.adminagent.rag.dto.RagDocumentDto;
import com.pfe.adminagent.rag.dto.RagMatch;
import com.pfe.adminagent.rag.dto.RagSearchRequest;
import com.pfe.adminagent.rag.dto.ReindexResult;
import com.pfe.adminagent.rag.repository.RagDocumentRepository;
import com.pfe.adminagent.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * RAG endpoints: ingest regulation documents, list them, and run semantic search.
 */
@RestController
@RequestMapping("/api/v1/rag")
@Tag(name = "RAG", description = "Regulation ingestion and semantic search")
@SecurityRequirement(name = "bearerAuth")
public class RagController {

    private final IngestionService ingestionService;
    private final RetrievalService retrievalService;
    private final RagDocumentRepository documentRepository;
    private final AiProperties aiProperties;

    public RagController(IngestionService ingestionService,
                         RetrievalService retrievalService,
                         RagDocumentRepository documentRepository,
                         AiProperties aiProperties) {
        this.ingestionService = ingestionService;
        this.retrievalService = retrievalService;
        this.documentRepository = documentRepository;
        this.aiProperties = aiProperties;
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Upload and index a regulation document (PDF, DOCX, TXT, MD)")
    public ResponseEntity<RagDocumentDto> upload(@RequestParam("file") MultipartFile file,
                                                 @RequestParam(value = "category", required = false) String category,
                                                 @AuthenticationPrincipal UserPrincipal principal) throws IOException {
        UUID uploader = principal != null ? principal.getId() : null;
        String cat = (category != null && !category.isBlank()) ? category : "uploads";
        RagDocumentDto dto = ingestionService.ingest(
                file.getBytes(), file.getOriginalFilename(), file.getContentType(), cat, uploader);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/documents")
    @Operation(summary = "List all ingested regulation documents")
    public List<RagDocumentDto> list() {
        return documentRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(RagDocumentDto::from)
                .toList();
    }

    @PostMapping("/search")
    @Operation(summary = "Semantic search over the regulation corpus (returns cited chunks)")
    public List<RagMatch> search(@Valid @RequestBody RagSearchRequest request) {
        return retrievalService.search(request.query(), request.topK());
    }

    @PostMapping("/reindex")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "(Re)index every document in the bundled knowledge-base directory")
    public ReindexResult reindex(@AuthenticationPrincipal UserPrincipal principal) {
        UUID uploader = principal != null ? principal.getId() : null;
        return ingestionService.reindexKnowledgeBase(
                aiProperties.getRag().getKnowledgeBasePath(), uploader);
    }
}
