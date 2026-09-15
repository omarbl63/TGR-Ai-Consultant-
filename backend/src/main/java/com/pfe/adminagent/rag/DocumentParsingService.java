package com.pfe.adminagent.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts plain text and light structural metadata (title, reference) from
 * uploaded documents. Plain-text formats are read directly; binary formats
 * (PDF, DOCX, …) are parsed with Apache Tika.
 */
@Service
public class DocumentParsingService {

    private static final Pattern TITLE = Pattern.compile("^#\\s+(.+)$", Pattern.MULTILINE);
    private static final Pattern DOC_REF =
            Pattern.compile("R[eé]f[eé]rence\\s*:?\\**\\s*([A-Z]{2,}[A-Z0-9\\-]+)");

    public String extractText(byte[] content, String filename) {
        String lower = filename == null ? "" : filename.toLowerCase();
        if (lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".markdown")) {
            return new String(content, StandardCharsets.UTF_8);
        }
        Document parsed = new ApacheTikaDocumentParser().parse(new ByteArrayInputStream(content));
        return parsed.text();
    }

    /** First Markdown H1, or {@code null} if none. */
    public String extractTitle(String text) {
        Matcher m = TITLE.matcher(text);
        return m.find() ? m.group(1).trim() : null;
    }

    /** Administrative reference code (e.g. POL-MIS-2024), or {@code null}. */
    public String extractDocRef(String text) {
        Matcher m = DOC_REF.matcher(text);
        return m.find() ? m.group(1).trim() : null;
    }
}
