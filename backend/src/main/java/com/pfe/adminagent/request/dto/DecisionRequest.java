package com.pfe.adminagent.request.dto;

import com.pfe.adminagent.request.domain.RequestStatus;
import com.pfe.adminagent.request.domain.RequestEventType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * An approver's decision on a request. The AI recommends; this is the human's call.
 */
public record DecisionRequest(
        @NotNull Action action,
        @Size(max = 4000) String comment
) {
    public enum Action {
        APPROVE(RequestStatus.APPROVED, RequestEventType.APPROVED),
        REJECT(RequestStatus.REJECTED, RequestEventType.REJECTED),
        REQUEST_CHANGES(RequestStatus.CHANGES_REQUESTED, RequestEventType.CHANGES_REQUESTED);

        private final RequestStatus resultingStatus;
        private final RequestEventType eventType;

        Action(RequestStatus resultingStatus, RequestEventType eventType) {
            this.resultingStatus = resultingStatus;
            this.eventType = eventType;
        }

        public RequestStatus resultingStatus() {
            return resultingStatus;
        }

        public RequestEventType eventType() {
            return eventType;
        }
    }
}
