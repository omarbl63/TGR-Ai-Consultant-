package com.pfe.adminagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Typed configuration for the AI layer (LLM, embeddings, storage),
 * bound from {@code adminai.*}. Consumed by the AI/RAG modules in later phases.
 */
@ConfigurationProperties(prefix = "adminai")
public class AiProperties {

    @NestedConfigurationProperty
    private final Llm llm = new Llm();
    @NestedConfigurationProperty
    private final Embedding embedding = new Embedding();
    @NestedConfigurationProperty
    private final Storage storage = new Storage();
    @NestedConfigurationProperty
    private final Rag rag = new Rag();

    public Llm getLlm() {
        return llm;
    }

    public Embedding getEmbedding() {
        return embedding;
    }

    public Storage getStorage() {
        return storage;
    }

    public Rag getRag() {
        return rag;
    }

    public static class Llm {
        private String baseUrl;
        private String apiKey;
        private String model;
        private double temperature = 0.2;
        private int maxTokens = 2048;

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public double getTemperature() { return temperature; }
        public void setTemperature(double temperature) { this.temperature = temperature; }
        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    }

    public static class Embedding {
        private String baseUrl;
        private String model = "bge-m3";
        private int dimension = 1024;

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public int getDimension() { return dimension; }
        public void setDimension(int dimension) { this.dimension = dimension; }
    }

    public static class Storage {
        private String path = "./storage";

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
    }

    /**
     * Retrieval-Augmented Generation settings. Chunk sizes are expressed in
     * characters; ~3600 chars ≈ 800–1000 tokens for FR/EN administrative text,
     * with ~18% overlap, per the project's chunking strategy.
     */
    public static class Rag {
        private String knowledgeBasePath = "./knowledge-base";
        private String embeddingTable = "regulation_embeddings";
        private int chunkSizeChars = 3600;
        private int chunkOverlapChars = 650;
        private int topK = 5;
        private double minScore = 0.5;

        public String getKnowledgeBasePath() { return knowledgeBasePath; }
        public void setKnowledgeBasePath(String knowledgeBasePath) { this.knowledgeBasePath = knowledgeBasePath; }
        public String getEmbeddingTable() { return embeddingTable; }
        public void setEmbeddingTable(String embeddingTable) { this.embeddingTable = embeddingTable; }
        public int getChunkSizeChars() { return chunkSizeChars; }
        public void setChunkSizeChars(int chunkSizeChars) { this.chunkSizeChars = chunkSizeChars; }
        public int getChunkOverlapChars() { return chunkOverlapChars; }
        public void setChunkOverlapChars(int chunkOverlapChars) { this.chunkOverlapChars = chunkOverlapChars; }
        public int getTopK() { return topK; }
        public void setTopK(int topK) { this.topK = topK; }
        public double getMinScore() { return minScore; }
        public void setMinScore(double minScore) { this.minScore = minScore; }
    }
}
