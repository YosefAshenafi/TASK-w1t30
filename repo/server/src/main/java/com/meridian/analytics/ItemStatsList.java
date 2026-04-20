package com.meridian.analytics;

import java.util.List;
import java.util.UUID;

public record ItemStatsList(List<Item> items) {
    public record Item(UUID itemId, double difficulty, double discrimination, long attempts) {}
}
