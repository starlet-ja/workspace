package com.example.demo.service;

import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class CommentService {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    // イベントに対するコメントを登録
    public ResponseEntity<?> postComment(Map<String, Object> postComment, Integer userId) {
        String commentText = (String) postComment.get("comment");

        if (commentText == null || commentText.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "コメント内容が空です。"));
        }

        Integer eventId;
        try {
            Object rawEventId = postComment.get("eventId");
            if (rawEventId instanceof Number) {
                eventId = ((Number) rawEventId).intValue();
            } else if (rawEventId instanceof String) {
                eventId = Integer.parseInt((String) rawEventId);
            } else {
                throw new IllegalArgumentException("eventIdの形式が不正です");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "イベントIDの取得に失敗しました。"));
        }

        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO event_comments (event_id, user_id, comment_text, comment_time) VALUES (?, ?, ?, ?)");
            ps.setInt(1, eventId);
            ps.setInt(2, userId);
            ps.setString(3, commentText);
            ps.setObject(4, now);
            return ps;
        });

        return ResponseEntity.ok(Map.of("message", "コメント登録成功"));
    }
    
    // イベントに対するコメントを取得
    public ResponseEntity<List<Map<String, Object>>> getComments(Object rawEventId) {
        Integer eventId;

        try {
            if (rawEventId instanceof Number) {
                eventId = ((Number) rawEventId).intValue();
            } else if (rawEventId instanceof String) {
                eventId = Integer.parseInt((String) rawEventId);
            } else {
                return ResponseEntity.badRequest().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }

        List<Map<String, Object>> comments = jdbcTemplate.query(
            "SELECT c.comment_text, c.comment_time, u.user_name " +
            "FROM event_comments c " +
            "JOIN user_name u ON c.user_id = u.user_id " +
            "WHERE c.event_id = ? ORDER BY c.comment_time ASC",
            new Object[]{eventId},
            (rs, rowNum) -> {
                Map<String, Object> map = new HashMap<>();
                map.put("commentText", rs.getString("comment_text"));
                map.put("commentTime", rs.getTimestamp("comment_time").toLocalDateTime().toString());
                map.put("userName", rs.getString("user_name"));
                return map;
            }
        );

        return ResponseEntity.ok(comments);
    }
}
