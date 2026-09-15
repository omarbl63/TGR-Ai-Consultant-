package com.pfe.adminagent.request.dto;

import java.util.Map;

/**
 * Payload to update an editable request's structured data (owner only, when the
 * request is a DRAFT or has been returned for modification).
 */
public record UpdateRequestRequest(Map<String, Object> structuredData) {
}
