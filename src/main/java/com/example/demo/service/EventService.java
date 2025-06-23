package com.example.demo.service;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import com.example.demo.dto.EventDetail;

@Service
public class EventService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> getEventsByDate(String startDateStr, String endDateStr, String eventType) {
        String sql = "SELECT ev.event_id, ev.event_title, ev.event_date, ev.start_time, "
                   + "ev.end_time, ev.lat, ev.lng, ev.recruit_min, ev.recruit_max, ev.com_method, ev.user_id, ed.event_message, "
                   + "(SELECT COUNT(*) FROM event_participants p WHERE p.event_id = ev.event_id) AS participant_count, "
                   + "(SELECT user_name FROM user_name WHERE user_id = ev.user_id) AS host "
                   + "FROM event ev "
                   + "LEFT JOIN event_details ed ON ev.event_id = ed.event_id "
                   + "WHERE ev.event_date BETWEEN ? AND ? AND ev.event_type = ? "
                   + "ORDER BY ev.event_date ASC, ev.start_time ASC";

        java.sql.Date startDate = java.sql.Date.valueOf(startDateStr);
        java.sql.Date endDate = java.sql.Date.valueOf(endDateStr);

        // 制限マップの構築
        String restrictionSql = "SELECT event_id, restriction FROM restriction";
        Map<Integer, List<String>> restrictionMap = new HashMap<>();
        jdbcTemplate.query(restrictionSql, rs -> {
            int eventId = rs.getInt("event_id");
            String restriction = rs.getString("restriction");
            restrictionMap.computeIfAbsent(eventId, k -> new ArrayList<>()).add(restriction);
        });

        // イベント一覧と制限の結合
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd");

        return jdbcTemplate.query(sql, new Object[]{startDate, endDate, eventType}, (rs, rowNum) -> {
            Map<String, Object> map = new HashMap<>();
            int eventId = rs.getInt("event_id");

            map.put("eventId", eventId);
            map.put("eventTitle", rs.getString("event_title"));
            map.put("host", rs.getString("host"));
            map.put("userId", rs.getString("user_id"));
            map.put("eventDate", rs.getDate("event_date").toLocalDate().format(dateFormatter));
            map.put("startTime", rs.getTime("start_time").toLocalTime().format(timeFormatter));
            map.put("recruitMin", rs.getInt("recruit_min"));
            map.put("recruitMax", rs.getInt("recruit_max"));
            map.put("comMethod", rs.getString("com_method"));
            map.put("lat", rs.getDouble("lat"));
            map.put("lng", rs.getDouble("lng"));
            map.put("message", rs.getString("event_message"));
            map.put("participantCount", rs.getInt("participant_count"));
            map.put("restrictions", restrictionMap.getOrDefault(eventId, new ArrayList<>()));

            return map;
        });
    }
    

    private static final Logger logger = LoggerFactory.getLogger(EventService.class);

    public Map<String, Object> registerEvent(Map<String, Object> postMessage, Integer userId) {
        String eventType = (String) postMessage.get("eventType");
        Double lat = null;
        Double lng = null;
        String comMethod = (String) postMessage.get("comMethod");
        String message = (String) postMessage.get("message");


        // オフラインの場合は座標必須
        if ("off".equals(eventType)) {
            lat = ((Number) postMessage.get("lat")).doubleValue();
            lng = ((Number) postMessage.get("lng")).doubleValue();
            comMethod = null;
        }
        
        // ラムダ式内で使う変数はfinalとする
        final LocalTime startTime = LocalTime.parse((String) postMessage.get("startTime"));
        final LocalTime endTime = null;
        final Double finalLat = lat;
        final Double finalLng = lng;
        final LocalDate date = LocalDate.parse((String) postMessage.get("date"));
        final String eventTitle = (String) postMessage.get("eventTitle");
        final Integer numPeopleMin = Integer.valueOf((String) postMessage.get("numPeopleMin"));
        final Integer numPeopleMax = Integer.valueOf((String) postMessage.get("numPeopleMax"));
        final String finalComMethod = comMethod;
        
        

        // restrictions は List<String> で受け取って List<Integer> に変換
        List<Integer> restrictions = new ArrayList<>();
        Object raw = postMessage.get("restrictions");
        if (raw instanceof List<?>) {
            for (Object obj : (List<?>) raw) {
                restrictions.add(Integer.valueOf(obj.toString()));
            }
        }

        // イベント登録
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO event (start_time, end_time, lat, lng, user_id, " +
                "event_date, event_title, recruit_min, recruit_max, com_method, event_type) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            );
            ps.setObject(1, startTime);
            ps.setObject(2, endTime);
            ps.setObject(3, finalLat, java.sql.Types.DOUBLE);
            ps.setObject(4, finalLng, java.sql.Types.DOUBLE);
            ps.setInt(5, userId);
            ps.setObject(6, date);
            ps.setString(7, eventTitle);
            ps.setObject(8, numPeopleMin, java.sql.Types.INTEGER);
            ps.setObject(9, numPeopleMax, java.sql.Types.INTEGER);
            ps.setString(10, finalComMethod);
            ps.setString(11, eventType);
            return ps;
        }, keyHolder);

        Number eventId = (Number) keyHolder.getKeys().get("event_id");
        logger.info("/set-event イベントテーブルにデータ登録成功");

        // イベント詳細
        jdbcTemplate.update("INSERT INTO event_details (event_id, event_message) VALUES (?, ?)", eventId, message);
        logger.info("/set-event イベント詳細テーブルにデータ登録成功");

        // 制限
        for (Integer restrict : restrictions) {
            jdbcTemplate.update("INSERT INTO restriction (event_id, restriction) VALUES (?, ?)", eventId, restrict);
        }
        logger.info("/set-event 制限テーブルにデータ登録成功");

        Map<String, Object> response = new HashMap<>();
        response.put("eventId", eventId);
        return response;
    }
    
    private static final Map<Integer, String> restrictionLabels = Map.of(
            1, "初心者歓迎",
            2, "敬語",
            3, "男性限定",
            4, "女性限定",
            5, "10代",
            6, "20代",
            7, "30代",
            8, "40代",
            9, "50代",
            10, "60代"
        );

    public EventDetail fetchEventDetail(Integer eventId) {
        return jdbcTemplate.queryForObject(
            "SELECT ev.event_id, ev.event_title, ev.user_id, ev.event_date, " +
            "(SELECT user_name FROM user_name WHERE user_id = ev.user_id) AS host, " +
            "ev.start_time, ev.end_time, ed.event_message, ev.recruit_min, ev.recruit_max " +
            "FROM event ev LEFT JOIN event_details ed ON ev.event_id = ed.event_id " +
            "WHERE ev.event_id = ?",
            (rs, rowNum) -> {
                EventDetail d = new EventDetail();
                d.eventId = rs.getInt("event_id");
                d.eventTitle = rs.getString("event_title");
                d.userId = rs.getInt("user_id");
                d.host = rs.getString("host");
                d.eventDate = rs.getDate("event_date").toLocalDate();
                d.startTime = rs.getTime("start_time").toLocalTime();
                d.eventMessage = rs.getString("event_message");
                d.recruitMin = rs.getInt("recruit_min");
                d.recruitMax = rs.getInt("recruit_max");
                return d;
            },
            eventId
        );
    }

    public List<String> fetchRestrictionLabels(Integer eventId) {
        List<Integer> ids = jdbcTemplate.query(
            "SELECT restriction FROM restriction WHERE event_id = ?",
            (rs, rowNum) -> rs.getInt("restriction"),
            eventId
        );
        return ids.stream()
            .map(id -> restrictionLabels.getOrDefault(id, "不明"))
            .collect(Collectors.toList());
    }

    public List<Map<String, Object>> fetchParticipants(Integer eventId) {
        return jdbcTemplate.queryForList(
            "SELECT p.user_id, n.user_name FROM event_participants p " +
            "JOIN user_name n ON p.user_id = n.user_id WHERE p.event_id = ?",
            eventId
        );
    }

    public boolean isParticipating(Integer userId, Integer eventId) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM event_participants WHERE event_id = ? AND user_id = ?",
                Integer.class, eventId, userId
            );
            return count != null && count > 0;
        } catch (EmptyResultDataAccessException e) {
            return false;
        }
    }
    
    // イベントへの参加登録/削除
    public void toggleParticipation(Integer eventId, Integer userId) {
        // 現在の参加状況を確認
        String checkSql = "SELECT COUNT(*) FROM event_participants WHERE event_id = ? AND user_id = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, eventId, userId);

        if (count != null && count > 0) {
            // 既に参加 → 削除
            jdbcTemplate.update("DELETE FROM event_participants WHERE event_id = ? AND user_id = ?", eventId, userId);
        } else {
            // 未参加 → 追加
            jdbcTemplate.update("INSERT INTO event_participants (event_id, user_id) VALUES (?, ?)", eventId, userId);
        }
    }
    
}
