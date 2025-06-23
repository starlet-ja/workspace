package com.example.demo.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.common.Constants;
import com.example.demo.dto.LoginRequest;

@Service
public class UserService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    // メールアドレスに対するユーザーIDを取得
    public Integer getUserIdByEmail(String email) {
        try {
            String sql = "SELECT user_id FROM user_mail WHERE email = ?";
            return jdbcTemplate.queryForObject(sql, Integer.class, email);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
    
    // メールアドレスに対するパスワードを取得
    public Integer authenticate(String email, String password) {
        String sql = "SELECT p.hashed_password FROM user_mail m " +
                     "JOIN user_password p ON m.user_id = p.user_id " +
                     "WHERE m.email = ?";
        try {
            String hashedPassword = jdbcTemplate.queryForObject(sql, String.class, email);
            if (hashedPassword != null && passwordEncoder.matches(password, hashedPassword)) {
                return jdbcTemplate.queryForObject("SELECT user_id FROM user_mail WHERE email = ?", Integer.class, email);
            }
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
        return null;
    }
    
    // 名前を取得
    public String getUserName(Integer userId) {
        try {
            return jdbcTemplate.queryForObject("SELECT user_name FROM user_name WHERE user_id = ?", String.class, userId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    // 性別を取得
    public String getUserGender(Integer userId) {
        try {
            return jdbcTemplate.queryForObject("SELECT user_gender FROM user_gender WHERE user_id = ?", String.class, userId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    // プロフィールを取得
    public String getUserProfile(Integer userId) {
        try {
            return jdbcTemplate.queryForObject("SELECT user_profile FROM user_profile WHERE user_id = ?", String.class, userId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    // 将来行われるイベント
    public List<Map<String, Object>> getUpcomingEvents(Integer userId) {
        return jdbcTemplate.queryForList(
            "SELECT ev.event_id, ev.event_title " +
            "FROM event_participants ep " +
            "INNER JOIN event ev ON ep.event_id = ev.event_id " +
            "WHERE ep.user_id = ? AND (ev.event_date + ev.start_time) > CURRENT_TIMESTAMP",
            userId
        );
    }

    // 過去に行われたイベント
    public List<Map<String, Object>> getPastEvents(Integer userId) {
        return jdbcTemplate.queryForList(
            "SELECT ev.event_id, ev.event_title " +
            "FROM event_participants ep " +
            "INNER JOIN event ev ON ep.event_id = ev.event_id " +
            "WHERE ep.user_id = ? AND (ev.event_date + ev.start_time) < CURRENT_TIMESTAMP",
            userId
        );
    }
    
    // 新規ユーザー情報を登録
    public String registerUser(LoginRequest request) {
        try {
        	// 既存ユーザーがいないか確認
            String checkSql = "SELECT COUNT(*) FROM user_mail WHERE email = ?";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, request.getAddress());

            if (count != null && count > 0) {
                return "duplicate";  // 登録済み
            }
            
            // user_id を自動採番
            String maxIdSql = "SELECT COALESCE(MAX(user_id), 0) + 1 FROM user_mail";
            Integer newUserId = jdbcTemplate.queryForObject(maxIdSql, Integer.class);

            // user_mail を登録
            jdbcTemplate.update("INSERT INTO user_mail(user_id, email) VALUES (?, ?)", newUserId, request.getAddress());
            
            // user_password を登録
            String hashedPassword = passwordEncoder.encode(request.getPassword());
            jdbcTemplate.update("INSERT INTO user_password(user_id, hashed_password) VALUES (?, ?)", newUserId, hashedPassword);
            // user_nameにデフォルト値を登録
            jdbcTemplate.update("INSERT INTO user_name(user_id, user_name) VALUES (?, ?)", newUserId, Constants.DEFAULT_USER_NAME);
            // user_genderにデフォルト値を登録
            jdbcTemplate.update("INSERT INTO user_gender(user_id, user_gender) VALUES (?, ?)", newUserId, Constants.DEFAULT_USER_GENDER);
            // user_profileにデフォルト値を登録
            jdbcTemplate.update("INSERT INTO user_profile(user_id, user_profile) VALUES (?, ?)", newUserId, Constants.DEFAULT_USER_PROFILE);

            return "success";
        } catch (Exception e) {
            return "error";
        }
    }
    
    // マイプロフィールを取得
    public Map<String, Object> getUserProfileData(Integer userId) {
        Map<String, Object> profileData = new HashMap<>();

        // メールアドレス
        String email = jdbcTemplate.queryForObject(
            "SELECT email FROM user_mail WHERE user_id = ?", String.class, userId);
        profileData.put("userEmail", email);

        // 名前・性別・プロフィール
        profileData.put("userName", getUserName(userId));
        profileData.put("userGender", Optional.ofNullable(getUserGender(userId)).orElse("未選択"));
        profileData.put("userProfile", getUserProfile(userId));

        // イベント
        profileData.put("breforeParticipatingEvents", getUpcomingEvents(userId));
        profileData.put("afterParticipatingEvents", getPastEvents(userId));

        return profileData;
    }

    // マイプロフィールを更新
    public boolean updateUserProfile(String email, String userName, String userGender, String userProfile) {
        try {
            // user_id を取得
            String userIdSql = "SELECT user_id FROM user_mail WHERE email = ?";
            Integer userId = jdbcTemplate.queryForObject(userIdSql, Integer.class, email);

            // 各テーブルを更新
            jdbcTemplate.update("UPDATE user_name SET user_name = ? WHERE user_id = ?", userName, userId);
            jdbcTemplate.update("UPDATE user_gender SET user_gender = ? WHERE user_id = ?", userGender, userId);
            jdbcTemplate.update("UPDATE user_profile SET user_profile = ? WHERE user_id = ?", userProfile, userId);

            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    // 他人のプロフィールを取得する
    public Map<String, Object> getUserProfileById(Integer userId) {
        Map<String, Object> profileData = new HashMap<>();

        // ユーザー名
        String userName = null;
        try {
            userName = jdbcTemplate.queryForObject(
                "SELECT user_name FROM user_name WHERE user_id = ?", String.class, userId);
        } catch (EmptyResultDataAccessException e) {
            userName = null;
        }
        profileData.put("userName", userName);

        // 性別
        String userGender = Constants.DEFAULT_USER_GENDER;
        try {
            userGender = jdbcTemplate.queryForObject(
                "SELECT user_gender FROM user_gender WHERE user_id = ?", String.class, userId);
        } catch (EmptyResultDataAccessException e) {
            userGender = Constants.DEFAULT_USER_GENDER;
        }
        profileData.put("userGender", userGender);

        // プロフィール
        String userProfile = null;
        try {
            userProfile = jdbcTemplate.queryForObject(
                "SELECT user_profile FROM user_profile WHERE user_id = ?", String.class, userId);
        } catch (EmptyResultDataAccessException e) {
            userProfile = null;
        }
        profileData.put("userProfile", userProfile);

        // 参加予定イベント
        List<Map<String, Object>> beforeEvents = jdbcTemplate.queryForList(
            "SELECT ev.event_id, ev.event_title FROM event_participants ep " +
            "INNER JOIN event ev ON ep.event_id = ev.event_id " +
            "WHERE ep.user_id = ? AND (ev.event_date + ev.start_time) > CURRENT_TIMESTAMP", userId);
        profileData.put("beforeEvents", beforeEvents);

        // 過去イベント
        List<Map<String, Object>> afterEvents = jdbcTemplate.queryForList(
            "SELECT ev.event_id, ev.event_title FROM event_participants ep " +
            "INNER JOIN event ev ON ep.event_id = ev.event_id " +
            "WHERE ep.user_id = ? AND (ev.event_date + ev.start_time) < CURRENT_TIMESTAMP", userId);
        profileData.put("afterEvents", afterEvents);

        return profileData;
    }

}
