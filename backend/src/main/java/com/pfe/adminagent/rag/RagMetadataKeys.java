package com.pfe.adminagent.rag;

/**
 * Metadata keys attached to every stored chunk, enabling citations and traceability.
 */
public final class RagMetadataKeys {

    public static final String DOCUMENT_ID = "document_id";
    public static final String DOC_REF = "doc_ref";
    public static final String TITLE = "title";
    public static final String CATEGORY = "category";
    public static final String SOURCE = "source";
    /** Chunk ordinal within the document (added by the recursive splitter). */
    public static final String INDEX = "index";

    private RagMetadataKeys() {
    }
}
