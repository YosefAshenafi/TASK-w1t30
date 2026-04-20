package com.meridian.sessions.dto;

import java.util.List;
import java.util.UUID;

public record SyncResult(List<AppliedItem> applied, List<ConflictItem> conflicts) {

    public record AppliedItem(UUID id, String kind, String status) {}

    public record ConflictItem(UUID id, String reason, Object serverVersion) {}
}
