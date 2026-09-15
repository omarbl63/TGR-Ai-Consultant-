package com.pfe.adminagent.request.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A regulation excerpt the AI relied on, linked to an {@link AiAnalysis}.
 * Gives every recommendation a traceable source.
 */
@Entity
@Table(name = "ai_citations")
public class AiCitation {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "analysis_id", nullable = false)
    private UUID analysisId;

    @Column(name = "doc_ref", length = 64)
    private String docRef;

    @Column(length = 255)
    private String title;

    @Column(length = 255)
    private String source;

    @Column(name = "chunk_index")
    private Integer chunkIndex;

    private Double score;

    @Column(columnDefinition = "text")
    private String excerpt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AiCitation() {
    }

    public AiCitation(UUID analysisId, String docRef, String title, String source,
                      Integer chunkIndex, Double score, String excerpt) {
        this.id = UUID.randomUUID();
        this.analysisId = analysisId;
        this.docRef = docRef;
        this.title = title;
        this.source = source;
        this.chunkIndex = chunkIndex;
        this.score = score;
        this.excerpt = excerpt;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getAnalysisId() { return analysisId; }
    public String getDocRef() { return docRef; }
    public String getTitle() { return title; }
    public String getSource() { return source; }
    public Integer getChunkIndex() { return chunkIndex; }
    public Double getScore() { return score; }
    public String getExcerpt() { return excerpt; }
    public Instant getCreatedAt() { return createdAt; }
}
