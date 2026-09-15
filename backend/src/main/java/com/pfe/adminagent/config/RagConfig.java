package com.pfe.adminagent.config;

import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wires the RAG infrastructure:
 *  - {@link EmbeddingModel}: bge-m3 served locally by Ollama.
 *  - {@link EmbeddingStore}: pgvector table in the same PostgreSQL database.
 *  - {@link DocumentSplitter}: recursive chunking with overlap.
 */
@Configuration
public class RagConfig {

    private static final Logger log = LoggerFactory.getLogger(RagConfig.class);
    private static final Pattern JDBC_PG = Pattern.compile("jdbc:postgresql://([^:/]+):(\\d+)/([^?]+).*");

    private final AiProperties props;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;
    @Value("${spring.datasource.username}")
    private String datasourceUser;
    @Value("${spring.datasource.password}")
    private String datasourcePassword;

    public RagConfig(AiProperties props) {
        this.props = props;
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        AiProperties.Embedding e = props.getEmbedding();
        log.info("Configuring Ollama embedding model '{}' at {}", e.getModel(), e.getBaseUrl());
        return OllamaEmbeddingModel.builder()
                .baseUrl(e.getBaseUrl())
                .modelName(e.getModel())
                .timeout(Duration.ofSeconds(120))
                .maxRetries(2)
                .build();
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        Matcher m = JDBC_PG.matcher(datasourceUrl);
        if (!m.matches()) {
            throw new IllegalStateException("Unsupported datasource URL for pgvector: " + datasourceUrl);
        }
        String host = m.group(1);
        int port = Integer.parseInt(m.group(2));
        String database = m.group(3);
        AiProperties.Rag rag = props.getRag();

        log.info("Configuring pgvector embedding store: table='{}', dimension={}",
                rag.getEmbeddingTable(), props.getEmbedding().getDimension());

        return PgVectorEmbeddingStore.builder()
                .host(host)
                .port(port)
                .database(database)
                .user(datasourceUser)
                .password(datasourcePassword)
                .table(rag.getEmbeddingTable())
                .dimension(props.getEmbedding().getDimension())
                .createTable(true)
                .dropTableFirst(false)
                .useIndex(true)
                .indexListSize(100)
                .build();
    }

    @Bean
    public DocumentSplitter documentSplitter() {
        AiProperties.Rag rag = props.getRag();
        return DocumentSplitters.recursive(rag.getChunkSizeChars(), rag.getChunkOverlapChars());
    }
}
