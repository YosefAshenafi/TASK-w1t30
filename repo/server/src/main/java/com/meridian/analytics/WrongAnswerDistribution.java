package com.meridian.analytics;

import java.util.List;
import java.util.UUID;

public record WrongAnswerDistribution(List<Item> items) {
    public record Item(UUID itemId, String stemPreview, String wrongChoiceId, long count, double pct) {}
}
