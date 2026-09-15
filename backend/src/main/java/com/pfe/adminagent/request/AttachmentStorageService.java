package com.pfe.adminagent.request;

import com.pfe.adminagent.config.AiProperties;
import com.pfe.adminagent.request.domain.RequestAttachment;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Persists attachment binaries to the configured storage directory and returns
 * the (unsaved) metadata entity. Files are namespaced by request id.
 */
@Service
public class AttachmentStorageService {

    private final Path root;

    public AttachmentStorageService(AiProperties aiProperties) {
        this.root = Path.of(aiProperties.getStorage().getPath(), "attachments");
    }

    public RequestAttachment store(UUID requestId, String filename, String contentType, byte[] content,
                                   String documentLabel) {
        String safeName = sanitize(filename);
        Path dir = root.resolve(requestId.toString());
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(UUID.randomUUID() + "_" + safeName);
            Files.write(target, content);
            return new RequestAttachment(requestId, safeName, contentType, content.length,
                    target.toString(), documentLabel);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store attachment '" + filename + "'", e);
        }
    }

    private String sanitize(String filename) {
        if (filename == null || filename.isBlank()) {
            return "document";
        }
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
