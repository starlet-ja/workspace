package com.example.demo.controller;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.demo.common.Constants;
import com.example.demo.dto.EventDetail;
import com.example.demo.dto.LoginRequest;

@Controller
public class BreadMapController {
	
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PasswordEncoder passwordEncoder;
	
	/* オンライン画面 */
	@GetMapping("/online-event")
	public String showOnlineEvent(Model model, HttpSession session, HttpServletRequest request) {
	    // ログイン状態を確認
		if (session.getAttribute("loginUser") == null) {
	        // Cookie をチェック
	        Cookie[] cookies = request.getCookies();
	        if (cookies != null) {
	            for (Cookie cookie : cookies) {
	                if ("rememberMeUserId".equals(cookie.getName())) {
	                    session.setAttribute("loginUser", Integer.valueOf(cookie.getValue()));
	                    break;
	                }
	            }
	        }
	    }
		model.addAttribute("isLoggedIn", session.getAttribute("loginUser") != null);
		// ログインユーザーIDを埋め込む
		model.addAttribute("userId", session.getAttribute("loginUser"));
		return "online-event";
	}
	
	/* オフライン画面 */
	/* 画面を表示する */
	@GetMapping("/offline-event")
	public String showFreeLance(Model model, HttpSession session, HttpServletRequest request) {
	    // ログイン状態を確認
		if (session.getAttribute("loginUser") == null) {
	        // Cookie をチェック
	        Cookie[] cookies = request.getCookies();
	        if (cookies != null) {
	            for (Cookie cookie : cookies) {
	                if ("rememberMeUserId".equals(cookie.getName())) {
	                    session.setAttribute("loginUser", Integer.valueOf(cookie.getValue()));
	                    break;
	                }
	            }
	        }
	    }
		model.addAttribute("isLoggedIn", session.getAttribute("loginUser") != null);
		return "offline-event";
	}
	
	/* 新規登録画面 */
	@GetMapping("/register")
	public String showRegisterPage() {
	    return "register"; // templates/register.html を表示
	}
	
	/* 新規登録 */
	@PostMapping("/register")
	public String register(@ModelAttribute LoginRequest request,Model model) {
	    try {
	        // 既存ユーザーがいないか確認
	        String checkSql = "SELECT COUNT(*) FROM user_mail WHERE email = ?";
	        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, request.getAddress());

	        if (count != null && count > 0) {
	            model.addAttribute("error", "このメールアドレスは既に登録されています。");
	            return "register";
	        }

	        // user_id を自動採番（ここは簡単に最大+1にしていますが、実際はSequenceやUUIDが望ましい）
	        String maxIdSql = "SELECT COALESCE(MAX(user_id), 0) + 1 FROM user_mail";
	        Integer newUserId = jdbcTemplate.queryForObject(maxIdSql, Integer.class);

	        // user_mail に登録
	        jdbcTemplate.update("INSERT INTO user_mail(user_id, email) VALUES (?, ?)",
	                newUserId, request.getAddress());

	        // user_password に登録
	        String hashedPassword = passwordEncoder.encode(request.getPassword());
	        jdbcTemplate.update("INSERT INTO user_password(user_id, hashed_password) VALUES (?, ?)",
	                newUserId, hashedPassword);
	        
	        // user_nameにデフォルト値を登録
	        jdbcTemplate.update("INSERT INTO user_name(user_id, user_name) VALUES (?, ?)",
	                newUserId, Constants.DEFAULT_USER_NAME);
	        
	        // user_genderにデフォルト値を登録
	        jdbcTemplate.update("INSERT INTO user_gender(user_id, user_gender) VALUES (?, ?)",
	                newUserId, Constants.DEFAULT_USER_GENDER);
	        
	        // user_profileにデフォルト値を登録
	        jdbcTemplate.update("INSERT INTO user_profile(user_id, user_profile) VALUES (?, ?)",
	                newUserId, Constants.DEFAULT_USER_PROFILE);

	        model.addAttribute("message", "新規登録が完了しました！ログインしてください。");
	        return "login"; // 登録後に login.html に戻る
	    } catch (Exception e) {
	        model.addAttribute("error", "登録時にエラーが発生しました。");
	        return "register";
	    }
	}

	/* ログイン画面 */
	@GetMapping("/login")
	public String showLoginPage() {
	    return "login";
	}

	/* ログイン画面 */
	@PostMapping("/login")
	public String login(@ModelAttribute LoginRequest request, Model model,
			HttpSession session, HttpServletResponse response) {
	    try {
	        String sql = "SELECT p.hashed_password FROM user_mail m " +
	                     "JOIN user_password p ON m.user_id = p.user_id " +
	                     "WHERE m.email = ?";

	        String hashedPassword = jdbcTemplate.queryForObject(sql, String.class, request.getAddress());

	        if (hashedPassword != null && passwordEncoder.matches(request.getPassword(), hashedPassword)) {
	            // user_id を取得
	            String userIdSql = "SELECT user_id FROM user_mail WHERE email = ?";
	            Integer userId = jdbcTemplate.queryForObject(userIdSql, Integer.class, request.getAddress());

	            // ユーザー名を取得
	            String userNameSql = "SELECT user_name FROM user_name WHERE user_id = ?";
	            String userName = null;
	            try {
	                userName = jdbcTemplate.queryForObject(userNameSql, String.class, userId);
	            } catch (EmptyResultDataAccessException e) {
	                // 見つからなかった場合は null にして処理を継続
	                userName = null;
	            }
	            
	            // 性別を取得
	            String userGenderSql = "SELECT user_gender FROM user_gender WHERE user_id = ?";
	            String userGender = null;
	            try {
	            	userGender = jdbcTemplate.queryForObject(userGenderSql, String.class, userId);	            	
	            } catch (EmptyResultDataAccessException e) {
	                // 見つからなかった場合は null にして処理を継続
	            	userGender = null;
	            }

	            // プロフィールを取得
	            String userProfileSql = "SELECT user_profile FROM user_profile WHERE user_id = ?";
	            String userProfile = null;
	            try {
	            	userProfile = jdbcTemplate.queryForObject(userProfileSql, String.class, userId);	            	
	            } catch (EmptyResultDataAccessException e) {
	                // 見つからなかった場合は null にして処理を継続
	            	userProfile = null;
	            }
	            
	            // 参加するイベントを取得
	            List<Map<String, Object>> breforeParticipatingEvents = jdbcTemplate.queryForList(
	            	    "SELECT ev.event_id, ev.event_title " +
	            	    "FROM event_participants ep " +
	            	    "INNER JOIN event ev ON ep.event_id = ev.event_id " +
	            	    "WHERE ep.user_id = ? " +
	            	    "AND (ev.event_date + ev.start_time) > CURRENT_TIMESTAMP",
	            	    userId
	            	);
	            
	            // 過去に参加したイベントを取得
	            List<Map<String, Object>> afterParticipatingEvents = jdbcTemplate.queryForList(
	            	    "SELECT ev.event_id, ev.event_title " +
	            	    "FROM event_participants ep " +
	            	    "INNER JOIN event ev ON ep.event_id = ev.event_id " +
	            	    "WHERE ep.user_id = ? " +
	            	    "AND (ev.event_date + ev.start_time) < CURRENT_TIMESTAMP",
	            	    userId
	            	);

	            // セッションにログイン情報をセット
	            session.setAttribute("loginUser", userId);
	            
	            // Cookie にuserIdを保存
	            Cookie cookie = new Cookie("rememberMeUserId", String.valueOf(userId));
	            cookie.setMaxAge(60 * 60 * 24 * 7); // 7日間
	            cookie.setPath("/");
	            cookie.setHttpOnly(true); // JSからアクセス不可に
	            response.addCookie(cookie);

	            // model にセット
	            model.addAttribute("isLoggedIn", session.getAttribute("loginUser") != null);
	            model.addAttribute("userEmail", request.getAddress());
	            model.addAttribute("userName", userName);
	            model.addAttribute("userGender", userGender);
	            model.addAttribute("userProfile", userProfile);
	    	    model.addAttribute("breforeParticipatingEvents", breforeParticipatingEvents);
	    	    model.addAttribute("afterParticipatingEvents", afterParticipatingEvents);

	            return "profile";
	        } else {
	            model.addAttribute("error", "ログイン失敗。メールアドレスまたはパスワードが間違っています。");
	            return "login";
	        }
	    } catch (Exception e) {
	        model.addAttribute("error", "ログイン失敗。メールアドレスまたはパスワードが間違っています。");
	        return "login";
	    }
	}

	/* ログアウト */
	@GetMapping("/user-logout")
	public String logout(HttpSession session, HttpServletResponse response) {
	    // セッションを無効化
	    session.invalidate();
	    // Cookie削除
	    Cookie cookie = new Cookie("rememberMeUserId", null);
	    cookie.setMaxAge(0); // 即時削除
	    cookie.setPath("/");
	    response.addCookie(cookie);
	    // ログイン画面にリダイレクト
	    return "redirect:/login";
	}

	@GetMapping("/profile")
	public String showProfile(HttpSession session, Model model) {
	    Object loginUser = session.getAttribute("loginUser");

	    if (loginUser == null) {
	        return "redirect:/login";
	    }

	    Integer userId = (Integer) loginUser;

	    // ユーザー情報を取得
	    String userEmail = jdbcTemplate.queryForObject(
	        "SELECT email FROM user_mail WHERE user_id = ?", String.class, userId);
	    
	    // ユーザー名を取得
        String userName = null;
        try {
        	userName = jdbcTemplate.queryForObject(
        	        "SELECT user_name FROM user_name WHERE user_id = ?", String.class, userId);	            	
        } catch (EmptyResultDataAccessException e) {
            // 見つからなかった場合は null にして処理を継続
        	userName = null;
        }

	    // ユーザーの性別を取得
        String userGender = "未選択";
        try {
        	userGender = jdbcTemplate.queryForObject(
        	        "SELECT user_gender FROM user_gender WHERE user_id = ?", String.class, userId);	            	
        } catch (EmptyResultDataAccessException e) {
            // 見つからなかった場合は null にして処理を継続
        	userGender = "未選択";
        }
        
        // プロフィールを取得
        String userProfile = null;
        try {
        	userProfile = jdbcTemplate.queryForObject(
        			"SELECT user_profile FROM user_profile WHERE user_id = ?", String.class, userId);
        } catch (EmptyResultDataAccessException e) {
            // 見つからなかった場合は null にして処理を継続
        	userProfile = null;
        }
        
        // 参加するイベントを取得
        List<Map<String, Object>> breforeParticipatingEvents = jdbcTemplate.queryForList(
        	    "SELECT ev.event_id, ev.event_title " +
        	    "FROM event_participants ep " +
        	    "INNER JOIN event ev ON ep.event_id = ev.event_id " +
        	    "WHERE ep.user_id = ? " +
        	    "AND (ev.event_date + ev.start_time) > CURRENT_TIMESTAMP",
        	    userId
        	);
        
        // 過去に参加したイベントを取得
        List<Map<String, Object>> afterParticipatingEvents = jdbcTemplate.queryForList(
        	    "SELECT ev.event_id, ev.event_title " +
        	    "FROM event_participants ep " +
        	    "INNER JOIN event ev ON ep.event_id = ev.event_id " +
        	    "WHERE ep.user_id = ? " +
        	    "AND (ev.event_date + ev.start_time) < CURRENT_TIMESTAMP",
        	    userId
        	);
        	
        
	    // modelにセット
	    model.addAttribute("isLoggedIn", true);
	    model.addAttribute("userEmail", userEmail);
	    model.addAttribute("userName", userName);
	    model.addAttribute("userGender", userGender);
	    model.addAttribute("userProfile", userProfile);
	    model.addAttribute("breforeParticipatingEvents", breforeParticipatingEvents);
	    model.addAttribute("afterParticipatingEvents", afterParticipatingEvents);

	    return "profile";
	}
	
	// プロフィール編集画面を表示
	@GetMapping("/edit-profile")
	public String showEditProfile(@RequestParam String email, Model model) {
	    // user_id を取得
	    String userIdSql = "SELECT user_id FROM user_mail WHERE email = ?";
	    Integer userId = jdbcTemplate.queryForObject(userIdSql, Integer.class, email);

	    // ユーザー名を取得
        String userName = null;
        try {
        	userName = jdbcTemplate.queryForObject(
        	        "SELECT user_name FROM user_name WHERE user_id = ?", String.class, userId);	            	
        } catch (EmptyResultDataAccessException e) {
            // 見つからなかった場合は null にして処理を継続
        	userName = null;
        }
        
	    // ユーザーの性別を取得
        String userGender = "未選択";
        try {
        	userGender = jdbcTemplate.queryForObject(
        	        "SELECT user_gender FROM user_gender WHERE user_id = ?", String.class, userId);	            	
        } catch (EmptyResultDataAccessException e) {
            // 見つからなかった場合は null にして処理を継続
        	userGender = "未選択";
        }

	    // プロフィールを取得
        String userProfile = null;
        try {
        	userProfile = jdbcTemplate.queryForObject(
        			"SELECT user_profile FROM user_profile WHERE user_id = ?", String.class, userId);
        } catch (EmptyResultDataAccessException e) {
            // 見つからなかった場合は null にして処理を継続
        	userProfile = null;
        }

	    // model にセット
	    model.addAttribute("userEmail", email);
	    model.addAttribute("userName", userName);
	    model.addAttribute("userGender", userGender);
	    model.addAttribute("userProfile", userProfile);

	    return "edit-profile";
	}

	// プロフィールを更新する
	@PostMapping("/edit-profile")
	public String updateProfile(@RequestParam String email,
	                            @RequestParam String userName,
	                            @RequestParam String userGender,
	                            @RequestParam String userProfile,
	                            Model model) {
	    try {
	        // user_id を取得
	        String userIdSql = "SELECT user_id FROM user_mail WHERE email = ?";
	        Integer userId = jdbcTemplate.queryForObject(userIdSql, Integer.class, email);

	        // user_name テーブル更新
	        jdbcTemplate.update("UPDATE user_name SET user_name = ? WHERE user_id = ?",
	                userName, userId);
	        
	        // user_gender テーブル更新
	        jdbcTemplate.update("UPDATE user_gender SET user_gender = ? WHERE user_id = ?",
	                userGender, userId);

	        // user_profile テーブル更新
	        jdbcTemplate.update("UPDATE user_profile SET user_profile = ? WHERE user_id = ?",
	                userProfile, userId);

	        // 参加するイベントを取得
	        List<Map<String, Object>> breforeParticipatingEvents = jdbcTemplate.queryForList(
	        	    "SELECT ev.event_id, ev.event_title " +
	        	    "FROM event_participants ep " +
	        	    "INNER JOIN event ev ON ep.event_id = ev.event_id " +
	        	    "WHERE ep.user_id = ? " +
	        	    "AND (ev.event_date + ev.start_time) > CURRENT_TIMESTAMP",
	        	    userId
	        	);
	        
	        // 過去に参加したイベントを取得
	        List<Map<String, Object>> afterParticipatingEvents = jdbcTemplate.queryForList(
	        	    "SELECT ev.event_id, ev.event_title " +
	        	    "FROM event_participants ep " +
	        	    "INNER JOIN event ev ON ep.event_id = ev.event_id " +
	        	    "WHERE ep.user_id = ? " +
	        	    "AND (ev.event_date + ev.start_time) < CURRENT_TIMESTAMP",
	        	    userId
	        	);
	        
	        // 更新後はプロフィール画面に遷移
	        model.addAttribute("userEmail", email);
	        model.addAttribute("userName", userName);
	        model.addAttribute("userGender", userGender);
	        model.addAttribute("userProfile", userProfile);
		    model.addAttribute("breforeParticipatingEvents", breforeParticipatingEvents);
		    model.addAttribute("afterParticipatingEvents", afterParticipatingEvents);
	        model.addAttribute("message", "プロフィールを更新しました！");
	        return "profile";
	    } catch (Exception e) {
	        model.addAttribute("message", "更新時にエラーが発生しました。");
	        return "profile";
	    }
	}

	@PostMapping("/get-events")
	@ResponseBody
	public List<Map<String, Object>> getEventsByDate(@RequestBody Map<String, String> request) {
	    // オンラインかオフラインを確認する
	    String eventType = (String)request.get("eventType");
	    
		    String sql = "SELECT ev.event_id, ev.event_title, ev.event_date, ev.start_time, "
		               + "ev.end_time, ev.lat, ev.lng, ev.recruit_min, ev.recruit_max, ev.user_id, ed.event_message, "
		               + "(SELECT COUNT(*) FROM event_participants p WHERE p.event_id = ev.event_id) AS participant_count, "
		               + "(SELECT user_name FROM user_name WHERE user_id = ev.user_id) AS host "
		               + "FROM event ev "
		               + "LEFT JOIN event_details ed ON ev.event_id = ed.event_id "
		               + "WHERE ev.event_date BETWEEN ? AND ? AND ev.event_type = ? "
		               + "ORDER BY ev.event_date ASC, ev.start_time ASC";
	
		    String startDateStr = request.get("startDate");
		    String endDateStr = request.get("endDate");
		    
		    java.sql.Date startDate = java.sql.Date.valueOf(startDateStr);
		    java.sql.Date endDate = java.sql.Date.valueOf(endDateStr);
	
		    // ① restriction テーブルから全イベントの制限リストを取得
		    String sql2 = "SELECT event_id, restriction FROM restriction";
		    Map<Integer, List<String>> restrictionMap = new HashMap<>();

		    jdbcTemplate.query(sql2, rs -> {
		        int eventId = rs.getInt("event_id");
		        String restriction = rs.getString("restriction");
		        restrictionMap.computeIfAbsent(eventId, k -> new ArrayList<>()).add(restriction);
		    });
		    
		    // ② イベントリストを作成しながら、制限リストも付加
		    // フォーマッターを用意
		    java.time.format.DateTimeFormatter timeFormatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
		    java.time.format.DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("MM/dd");

		    return jdbcTemplate.query(sql, new Object[]{startDate, endDate, eventType}, (rs, rowNum) -> {
		        Map<String, Object> map = new HashMap<>();
		        int eventId = rs.getInt("event_id");

		        map.put("eventId", eventId);
		        map.put("eventTitle", rs.getString("event_title"));
		        map.put("host", rs.getString("host"));
		        map.put("userId", rs.getString("user_id"));

		        java.sql.Date sqlDate = rs.getDate("event_date");
		        java.time.LocalDate localDate = sqlDate.toLocalDate();
		        map.put("eventDate", localDate.format(dateFormatter));

		        java.sql.Time startTime = rs.getTime("start_time");
		        map.put("startTime", startTime.toLocalTime().format(timeFormatter));
		        map.put("recruitMin", rs.getInt("recruit_min"));
		        map.put("recruitMax", rs.getInt("recruit_max"));
		        map.put("lat", rs.getDouble("lat"));
		        map.put("lng", rs.getDouble("lng"));
		        map.put("message", rs.getString("event_message"));
		        map.put("participantCount", rs.getInt("participant_count"));

		        // ③ 制限リストを追加
		        List<String> restrictions = restrictionMap.getOrDefault(eventId, new ArrayList<>());
		        map.put("restrictions", restrictions);

		        return map;
		    });
	  }
	
	/* メッセージをDBに登録する */
	@PostMapping("/set-event")
	@ResponseBody
	@Transactional
	public ResponseEntity<Map<String, Object>> registerEvent(@RequestBody Map<String, Object> postMessage, HttpSession session){
	    // セッションから loginUser を確認
	    Object loginUser = session.getAttribute("loginUser");
	    // ログインしていなければ 403 Forbidden を返す
	    if (loginUser == null) {
	    	Map<String, Object> errorResponse = new HashMap<>();
	    	errorResponse.put("error", "ログインが必要です。");
	    	return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
	    }
	    
	    Integer userId;
	    String eventTitle;
	    LocalDate date;
	    LocalTime startTime;
	    LocalTime endTime;
	    Double lat;
	    Double lng;
	    String message;
	    Integer numPeopleMin;
	    Integer numPeopleMax;
	    String comMethod;
	    List<Integer> restrictions;

	    // オンラインか、オフラインかで処理を変える
	    String eventType = (String)postMessage.get("eventType");
	    if (Constants.EVENT_TYPE_ON.equals(eventType)) {
	    	// オンラインの処理
	    	userId = (Integer) loginUser;
	    	eventTitle = (String)postMessage.get("eventTitle");
	    	date = LocalDate.parse((String)postMessage.get("date"));
	    	startTime = LocalTime.parse((String)postMessage.get("startTime"));
	    	// LocalTime endTime = LocalTime.parse((String)postMessage.get("endTime"));
	    	endTime = null;
	    	lat = null;
	    	lng = null;
	    	message = (String)postMessage.get("message");
	    	numPeopleMin = Integer.valueOf((String) postMessage.get("numPeopleMin"));
	    	numPeopleMax = Integer.valueOf((String) postMessage.get("numPeopleMax"));
	    	comMethod = (String) postMessage.get("comMethod");
	    	// restrictions は List<Object> 型になる場合があるのでキャスト
	    	restrictions = new ArrayList<>();
	    	Object raw = postMessage.get("restrictions");
	    	if (raw instanceof List<?>) {
	    	    List<?> rawList = (List<?>) raw;
	    	    for (Object obj : rawList) {
	    	        restrictions.add(Integer.valueOf((String) obj));
	    	    }
	    	}    	
	    } else if (Constants.EVENT_TYPE_OFF.equals(eventType)) {
	    	// オフラインの処理
	    	userId = (Integer) loginUser;
	    	eventTitle = (String)postMessage.get("eventTitle");
	    	date = LocalDate.parse((String)postMessage.get("date"));
	    	startTime = LocalTime.parse((String)postMessage.get("startTime"));
	    	// LocalTime endTime = LocalTime.parse((String)postMessage.get("endTime"));
	    	endTime = null;
	    	lat = ((Number) postMessage.get("lat")).doubleValue();
	    	lng = ((Number) postMessage.get("lng")).doubleValue();
	    	message = (String)postMessage.get("message");
	    	numPeopleMin = Integer.valueOf((String) postMessage.get("numPeopleMin"));
	    	numPeopleMax = Integer.valueOf((String) postMessage.get("numPeopleMax"));
	    	comMethod = null;
	    	// restrictions は List<Object> 型になる場合があるのでキャスト
	    	restrictions = new ArrayList<>();
	    	Object raw = postMessage.get("restrictions");
	    	if (raw instanceof List<?>) {
	    	    List<?> rawList = (List<?>) raw;
	    	    for (Object obj : rawList) {
	    	        restrictions.add(Integer.valueOf((String) obj));
	    	    }
	    	}	    	
	    } else {
	    	Map<String, Object> errorResponse = new HashMap<>();
	    	errorResponse.put("error", "イベント種別が不明");
	    	return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
	    }
	    
		// DBに値を登録
	    KeyHolder keyHolder = new GeneratedKeyHolder();
	    
	    // イベントテーブルにデータ登録
	    jdbcTemplate.update(con -> {
	        PreparedStatement ps = con.prepareStatement(
	            "INSERT INTO event (start_time, end_time, lat, lng, user_id, "
	            + "event_date, event_title, recruit_min, recruit_max, com_method, event_type) "
	            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
	            Statement.RETURN_GENERATED_KEYS);
	        ps.setObject(1, startTime);
	        ps.setObject(2, endTime);
	        
	        if (lat != null) {
	            ps.setDouble(3, lat);
	        } else {
	            ps.setNull(3, java.sql.Types.DOUBLE);
	        }
	        
	        if (lng != null) {
	            ps.setDouble(4, lng);
	        } else {
	            ps.setNull(4, java.sql.Types.DOUBLE);
	        }
	        
	        ps.setInt(5, userId);
	        ps.setObject(6, date);
	        ps.setString(7, eventTitle);
	        
	        if (numPeopleMin != null) {
	            ps.setInt(8, numPeopleMin);
	        } else {
	            ps.setNull(8, java.sql.Types.INTEGER);
	        }

	        if (numPeopleMax != null) {
	            ps.setInt(9, numPeopleMax);
	        } else {
	            ps.setNull(9, java.sql.Types.INTEGER);
	        }

	        if (comMethod != null) {
	            ps.setString(10, comMethod);
	        } else {
	            ps.setNull(10, java.sql.Types.VARCHAR);
	        }
	        
	        if (eventType != null) {
	            ps.setString(11, eventType);
	        } else {
	            ps.setNull(11, java.sql.Types.VARCHAR);
	        }
	        
	        return ps;
	    }, keyHolder);
	    
	    // イベント詳細テーブルにデータ登録
	    Number eventId = (Number) keyHolder.getKeys().get("event_id");
	    jdbcTemplate.update("INSERT INTO event_details (event_id, event_message) VALUES (?, ?)",
        eventId, message);

	    // 制限テーブルに restrictions をINSERT
	    for (Integer restrict : restrictions) {
	        jdbcTemplate.update(connection -> {
	            PreparedStatement ps = connection.prepareStatement(
	                "INSERT INTO restriction (event_id, restriction) VALUES (?, ?)"
	            );
	            ps.setInt(1, eventId.intValue());

	            if (restrict != null) {
	                ps.setInt(2, restrict.intValue());
	            } else {
	                ps.setNull(2, java.sql.Types.INTEGER);
	            }

	            return ps;
	        });
	    }
	    
	    // レスポンスとしてeventIdを返す
	    Map<String, Object> response = new HashMap<>();
	    response.put("eventId", eventId);

	    return ResponseEntity.ok(response);
	}
	
	
	// ID → 表示名の対応マップ
	Map<Integer, String> restrictionLabels = Map.of(
	    1, "初心者歓迎",
	    2, "敬語",
	    3, "男性限定",
	    4, "女性限定",
	    5, "10代OK",
	    6, "20代OK",
	    7, "30代OK",
	    8, "40代OK",
	    9, "50代OK",
	    10, "60代OK"
	);
	
	// イベント詳細ページを返す
	/**
	 * @param eventId
	 * @param model
	 * @param session
	 * @return
	 */
	@GetMapping("/event-detail")
	public String showEventDetail(@RequestParam Integer eventId, Model model, HttpSession session) {

	    // イベント詳細取得
	    EventDetail eventDetail = jdbcTemplate.queryForObject(
	        "SELECT ev.event_id, ev.event_title, ev.user_id, ev.event_date, "
	        + "(SELECT user_name FROM user_name WHERE user_id = ev.user_id) AS host, "
	        + "ev.start_time, ev.end_time, ed.event_message, ev.recruit_min, ev.recruit_max " +
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
	            // d.endTime = rs.getTime("end_time").toLocalTime();
	            d.eventMessage = rs.getString("event_message");
	            d.recruitMin = rs.getInt("recruit_min");
	            d.recruitMax = rs.getInt("recruit_max");
	            return d;
	        },
	        eventId
	    );
	    
		 // 制限リストを取得（整数で保持）
	    List<Integer> restrictionIds = jdbcTemplate.query(
	    	    "SELECT restriction FROM restriction WHERE event_id = ?",
	    	    (rs, rowNum) -> rs.getInt("restriction"),
	    	    eventId
	    	);

    	// 制限リストを表示名に変換
    	List<String> restrictionNames = restrictionIds.stream()
    	    .map(id -> restrictionLabels.getOrDefault(id, "不明"))
    	    .collect(Collectors.toList());
		
		// モデルに追加
	    model.addAttribute("eventDetail", eventDetail);
	    // 参加条件を追加
	    model.addAttribute("restrictions", restrictionNames);
	    
	    // 参加者一覧取得
	    List<Map<String, Object>> participants = jdbcTemplate.queryForList(
	        "SELECT p.user_id, n.user_name "
	        + "FROM event_participants p "
	        + "JOIN user_name n ON p.user_id = n.user_id "
	        + "WHERE p.event_id = ?",
	        eventId
	    );
	    model.addAttribute("participants", participants);

	    // ログイン情報
	    Object loginUser = session.getAttribute("loginUser");
	    boolean isLoggedIn = (loginUser != null);
	    model.addAttribute("isLoggedIn", isLoggedIn);
        // イベントに参加するユーザーか確認
	    boolean isParticipating = false;
	    if (isLoggedIn) {
	        Integer userId = (Integer) loginUser;
	        Integer count = jdbcTemplate.queryForObject(
	            "SELECT COUNT(*) FROM event_participants WHERE event_id = ? AND user_id = ?",
	            Integer.class, eventId, userId
	        );
	        isParticipating = (count != null && count > 0);
	    }
	    model.addAttribute("isParticipating", isParticipating);
	    // イベントのURL
	    String eventUrl = "http://192.168.1.17:8080/event-detail?eventId=" + eventId;
	    model.addAttribute("eventUrl", eventUrl);
	    // 主催者を取得
	    
	    return "event-detail";
	}


	
	// イベント参加ボタンをクリックした際の処理
	@PostMapping("/toggle-participation")
	public String toggleParticipation(@RequestParam Integer eventId, HttpSession session) {
	    Object loginUser = session.getAttribute("loginUser");
	    if (loginUser == null) {
	        return "redirect:/login";
	    }

	    Integer userId = (Integer) loginUser;

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

	    // 詳細画面にリダイレクト
	    return "redirect:/event-detail?eventId=" + eventId;
	}
	
	// 他人のプロフィールページ
	@GetMapping("/user-profile")
	public String showUserProfile(@RequestParam("userId") Integer userId, Model model, HttpSession session) {

	    // ユーザー名を取得
        String userName = null;
        try {
        	userName = jdbcTemplate.queryForObject(
        	        "SELECT user_name FROM user_name WHERE user_id = ?", String.class, userId);	            	
        } catch (EmptyResultDataAccessException e) {
            // 見つからなかった場合は null にして処理を継続
        	userName = null;
        }
        
        // ユーザー性別を取得
        String userGender = Constants.DEFAULT_USER_GENDER;
        try {
        	userGender = jdbcTemplate.queryForObject(
        	        "SELECT user_gender FROM user_gender WHERE user_id = ?", String.class, userId);	            	
        } catch (EmptyResultDataAccessException e) {
        	userGender = Constants.DEFAULT_USER_GENDER;
        }

	    // プロフィールを取得
        String userProfile = null;
        try {
        	userProfile = jdbcTemplate.queryForObject(
        			"SELECT user_profile FROM user_profile WHERE user_id = ?", String.class, userId);
        } catch (EmptyResultDataAccessException e) {
            // 見つからなかった場合は null にして処理を継続
        	userProfile = null;
        }

        // 参加するイベントを取得
        List<Map<String, Object>> breforeParticipatingEvents = jdbcTemplate.queryForList(
        	    "SELECT ev.event_id, ev.event_title " +
        	    "FROM event_participants ep " +
        	    "INNER JOIN event ev ON ep.event_id = ev.event_id " +
        	    "WHERE ep.user_id = ? " +
        	    "AND (ev.event_date + ev.start_time) > CURRENT_TIMESTAMP",
        	    userId
        	);
        
        // 過去に参加したイベントを取得
        List<Map<String, Object>> afterParticipatingEvents = jdbcTemplate.queryForList(
        	    "SELECT ev.event_id, ev.event_title " +
        	    "FROM event_participants ep " +
        	    "INNER JOIN event ev ON ep.event_id = ev.event_id " +
        	    "WHERE ep.user_id = ? " +
        	    "AND (ev.event_date + ev.start_time) < CURRENT_TIMESTAMP",
        	    userId
        	);

	    // ログイン状態判定
	    boolean isLoggedIn = (session.getAttribute("loginUser") != null);

	    // modelにセット
	    model.addAttribute("isLoggedIn", isLoggedIn);
	    // model.addAttribute("userEmail", userEmail);
	    model.addAttribute("userName", userName);
	    model.addAttribute("userGender", userGender);
	    model.addAttribute("userProfile", userProfile);
	    model.addAttribute("breforeParticipatingEvents", breforeParticipatingEvents);
	    model.addAttribute("afterParticipatingEvents", afterParticipatingEvents);

	    return "user-profile";
	}

	/* イベントに対するコメントをDBに登録する */
	@PostMapping("/post-comment")
	@ResponseBody
	@Transactional
	public ResponseEntity<?> postComment(@RequestBody Map<String, Object> postComment, HttpSession session) {
	    Object loginUser = session.getAttribute("loginUser");
	    if (loginUser == null) {
	        return ResponseEntity.status(HttpStatus.FORBIDDEN)
	            .body(Map.of("error", "ログインが必要です。"));
	    }

	    Integer userId = (Integer) loginUser;
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
	
	/* イベントに対するコメントをDBから取得する */
	@PostMapping("/get-comments")
	@ResponseBody
	public ResponseEntity<List<Map<String, Object>>> getComments(@RequestBody Map<String, Object> payload) {
	    Object rawEventId = payload.get("eventId");
	    Integer eventId;
	    if (rawEventId instanceof Number) {
	        eventId = ((Number) rawEventId).intValue();
	    } else if (rawEventId instanceof String) {
	        eventId = Integer.parseInt((String) rawEventId);
	    } else {
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
