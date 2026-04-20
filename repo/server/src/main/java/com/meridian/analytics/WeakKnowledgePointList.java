package com.meridian.analytics;

import java.util.List;
import java.util.UUID;

public record WeakKnowledgePointList(List<Item> items) {
    public record Item(UUID knowledgePointId, String name, double masteryPct, long attemptVolume) {}
}
