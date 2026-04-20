package com.meridian.analytics;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final JdbcTemplate jdbc;

    public MasteryTrendSeries masteryTrends(AnalyticsFilter f) {
        String scope = f.learnerId() != null ? "LEARNER" :
                       f.cohortId() != null  ? "COHORT" : "COURSE";

        String sql = """
                SELECT DATE_TRUNC('day', aa.attempted_at) AS day,
                       COUNT(*) AS attempts,
                       SUM(CASE WHEN aa.is_correct THEN 1 ELSE 0 END)::numeric / COUNT(*) AS mastery
                FROM assessment_attempts aa
                JOIN assessment_items ai ON ai.id = aa.item_id
                WHERE aa.attempted_at IS NOT NULL
                  AND (:from::timestamptz IS NULL OR aa.attempted_at >= :from::timestamptz)
                  AND (:to::timestamptz IS NULL OR aa.attempted_at <= :to::timestamptz)
                  AND (:courseId::uuid IS NULL OR ai.course_id = :courseId::uuid)
                  AND (:learnerId::uuid IS NULL OR aa.student_id = :learnerId::uuid)
                GROUP BY 1
                ORDER BY 1
                """.replace(":from::timestamptz", f.from() != null ? "'" + f.from() + "'" : "NULL")
                   .replace(":to::timestamptz", f.to() != null ? "'" + f.to() + "'" : "NULL")
                   .replace(":courseId::uuid", f.courseId() != null ? "'" + f.courseId() + "'" : "NULL")
                   .replace(":learnerId::uuid", f.learnerId() != null ? "'" + f.learnerId() + "'" : "NULL");

        List<MasteryTrendSeries.Point> points = jdbc.query(sql, (rs, i) -> new MasteryTrendSeries.Point(
                rs.getTimestamp("day").toInstant(),
                rs.getDouble("mastery") * 100.0,
                rs.getInt("attempts")));

        return new MasteryTrendSeries(scope, points);
    }

    public WrongAnswerDistribution wrongAnswers(AnalyticsFilter f) {
        String sql = """
                SELECT aa.item_id,
                       LEFT(ai.stem, 80) AS stem_preview,
                       aa.chosen_answer::text AS choice,
                       COUNT(*) AS cnt,
                       COUNT(*)::numeric / SUM(COUNT(*)) OVER (PARTITION BY aa.item_id) AS pct
                FROM assessment_attempts aa
                JOIN assessment_items ai ON ai.id = aa.item_id
                WHERE aa.is_correct = false
                  AND (:courseId::uuid IS NULL OR ai.course_id = :courseId::uuid)
                  AND (:learnerId::uuid IS NULL OR aa.student_id = :learnerId::uuid)
                GROUP BY aa.item_id, ai.stem, aa.chosen_answer::text
                ORDER BY cnt DESC
                LIMIT 200
                """.replace(":courseId::uuid", f.courseId() != null ? "'" + f.courseId() + "'" : "NULL")
                   .replace(":learnerId::uuid", f.learnerId() != null ? "'" + f.learnerId() + "'" : "NULL");

        List<WrongAnswerDistribution.Item> items = jdbc.query(sql, (rs, i) -> new WrongAnswerDistribution.Item(
                UUID.fromString(rs.getString("item_id")),
                rs.getString("stem_preview"),
                rs.getString("choice"),
                rs.getLong("cnt"),
                rs.getDouble("pct")));

        return new WrongAnswerDistribution(items);
    }

    public WeakKnowledgePointList weakKnowledgePoints(AnalyticsFilter f) {
        String sql = """
                SELECT kp.id AS kp_id, kp.name,
                       AVG(CASE WHEN aa.is_correct THEN 1.0 ELSE 0.0 END) * 100 AS mastery_pct,
                       COUNT(*) AS volume
                FROM assessment_attempts aa
                JOIN assessment_items ai ON ai.id = aa.item_id
                JOIN knowledge_points kp ON kp.id = ai.knowledge_point_id
                WHERE kp.id IS NOT NULL
                  AND (:courseId::uuid IS NULL OR ai.course_id = :courseId::uuid)
                  AND (:learnerId::uuid IS NULL OR aa.student_id = :learnerId::uuid)
                GROUP BY kp.id, kp.name
                ORDER BY mastery_pct ASC
                LIMIT 50
                """.replace(":courseId::uuid", f.courseId() != null ? "'" + f.courseId() + "'" : "NULL")
                   .replace(":learnerId::uuid", f.learnerId() != null ? "'" + f.learnerId() + "'" : "NULL");

        List<WeakKnowledgePointList.Item> items = jdbc.query(sql, (rs, i) -> new WeakKnowledgePointList.Item(
                UUID.fromString(rs.getString("kp_id")),
                rs.getString("name"),
                rs.getDouble("mastery_pct"),
                rs.getLong("volume")));

        return new WeakKnowledgePointList(items);
    }

    public ItemStatsList itemStats(AnalyticsFilter f) {
        String sql = """
                SELECT ai.id AS item_id, ai.difficulty, ai.discrimination, COUNT(aa.id) AS attempts
                FROM assessment_items ai
                LEFT JOIN assessment_attempts aa ON aa.item_id = ai.id
                WHERE ai.deleted_at IS NULL
                  AND (:courseId::uuid IS NULL OR ai.course_id = :courseId::uuid)
                GROUP BY ai.id, ai.difficulty, ai.discrimination
                ORDER BY attempts DESC
                LIMIT 200
                """.replace(":courseId::uuid", f.courseId() != null ? "'" + f.courseId() + "'" : "NULL");

        List<ItemStatsList.Item> items = jdbc.query(sql, (rs, i) -> new ItemStatsList.Item(
                UUID.fromString(rs.getString("item_id")),
                rs.getDouble("difficulty"),
                rs.getDouble("discrimination"),
                rs.getLong("attempts")));

        return new ItemStatsList(items);
    }
}
