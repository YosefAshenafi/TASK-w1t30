package com.meridian.analytics;

import java.time.Instant;
import java.util.List;

public record MasteryTrendSeries(String scope, List<Point> points) {
    public record Point(Instant at, double masteryPct, int attempts) {}
}
