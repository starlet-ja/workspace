package com.example.demo.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.demo.dto.EventDetail;
import com.example.demo.dto.LoginRequest;
import com.example.demo.service.CommentService;
import com.example.demo.service.EventService;
import com.example.demo.service.SessionService;
import com.example.demo.service.UserService;

@Controller
public class BreadMapController {
	
	private static final Logger logger = LoggerFactory.getLogger(BreadMapController.class);
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private SessionService sessionService;
	
	/* 紹介画面 */
	@GetMapping("/info")
	public String showInfo() {
		return "info";
	}
	
	/* 利用規約・プライバシーポリシー */
	@GetMapping("/policy")
	public String showPolicy() {
		return "policy";
	}
	
	/* オンライン画面 */
	@GetMapping("/online-event")
	public String showOnlineEvent(Model model, HttpSession session, HttpServletRequest request) {
	    // Cookie からログインユーザーを復元
	    sessionService.restoreLoginUserFromCookie(request, session);

	    // ログイン状態とユーザーIDをモデルに渡す
	    model.addAttribute("isLoggedIn", sessionService.isLoggedIn(session));
	    model.addAttribute("userId", sessionService.getLoginUserId(session));

	    return "online-event";
	}
	
	/* オフライン画面 */
	@GetMapping("/offline-event")
	public String showFreeLance(Model model, HttpSession session, HttpServletRequest request) {
	    // Cookieからログイン情報を復元
	    sessionService.restoreLoginUserFromCookie(request, session);

	    // ログイン状態をModelに渡す
	    model.addAttribute("isLoggedIn", sessionService.isLoggedIn(session));

	    return "offline-event";
	}
	
	/* 新規登録画面 */
	@GetMapping("/register")
	public String showRegisterPage() {
	    return "register"; // templates/register.html を表示
	}
	
	/* 新規登録 */
	@PostMapping("/register")
	public String register(@ModelAttribute LoginRequest request, Model model) {
	    String result = userService.registerUser(request);

	    switch (result) {
	        case "duplicate":
	            model.addAttribute("error", "このメールアドレスは既に登録されています。");
	            return "register";
	        case "error":
	            model.addAttribute("error", "登録時にエラーが発生しました。");
	            return "register";
	        default:
	            model.addAttribute("message", "新規登録が完了しました！ログインしてください。");
	            return "login";
	    }
	}

	/* ログイン画面 */
	@GetMapping("/login")
	public String showLoginPage() {
		logger.info("/login");
	    return "login";
	}

	/* ログイン画面 */	
	@PostMapping("/login")
	public String login(@ModelAttribute LoginRequest request, Model model,
			HttpSession session, HttpServletResponse response) {
		logger.info("/login メソッドの呼び出し: {}", request);
		
//		メールアドレス、パスワードが合っている場合は、ユーザーIDを取得
	    Integer userId = userService.authenticate(request.getAddress(), request.getPassword());

	    if (userId == null) {
	        model.addAttribute("error", "ログイン失敗。メールアドレスまたはパスワードが間違っています。");
	        return "login";
	    }
		

	    // ユーザー情報の取得
	    String userName = userService.getUserName(userId);
	    String userGender = userService.getUserGender(userId);
	    String userProfile = userService.getUserProfile(userId);
	    List<Map<String, Object>> breforeParticipatingEvents = userService.getUpcomingEvents(userId);
	    List<Map<String, Object>> afterParticipatingEvents = userService.getPastEvents(userId);


	    // セッションに保存
	    session.setAttribute("loginUser", userId);
	    // Cookieにも保存
	    Cookie cookie = new Cookie("rememberMeUserId", String.valueOf(userId));
	    cookie.setMaxAge(60 * 60 * 24 * 7);
	    cookie.setPath("/");
	    cookie.setHttpOnly(true);
	    response.addCookie(cookie);

	    // View に渡す
	    model.addAttribute("isLoggedIn", true);
	    model.addAttribute("userEmail", request.getAddress());
	    model.addAttribute("userName", userName);
	    model.addAttribute("userGender", userGender);
	    model.addAttribute("userProfile", userProfile);
	    model.addAttribute("breforeParticipatingEvents", breforeParticipatingEvents);
	    model.addAttribute("afterParticipatingEvents", afterParticipatingEvents);

	    return "profile";
	}


	/* ログアウト */
	@GetMapping("/user-logout")
	public String logout(HttpSession session, HttpServletResponse response) {
		logger.info("/user-logout");
		
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
	    if (loginUser == null) return "redirect:/login";

	    Integer userId = (Integer) loginUser;
	    Map<String, Object> profileData = userService.getUserProfileData(userId);

	    model.addAttribute("isLoggedIn", true);
	    model.addAllAttributes(profileData);

	    return "profile";
	}
	
	// プロフィール編集画面を表示
	@GetMapping("/edit-profile")
	public String showEditProfile(@RequestParam String email, Model model) {
		logger.info("/edit-profile");
		
	    // user_id を取得
	    Integer userId = userService.getUserIdByEmail(email);

	    // ユーザー名を取得
        String userName = userService.getUserName(userId);
        
	    // ユーザーの性別を取得
        String userGender = userService.getUserGender(userId);
        if (userGender == null) {
        	userGender = "未選択";
        }

	    // プロフィールを取得
        String userProfile = userService.getUserProfile(userId);

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
	    logger.info("/edit-profile メソッドの呼び出し");

	    boolean success = userService.updateUserProfile(email, userName, userGender, userProfile);
	    Integer userId = userService.getUserIdByEmail(email); // これが必要なら service にメソッド追加

	    Map<String, Object> profileData = userService.getUserProfileData(userId);
	    model.addAllAttributes(profileData);
	    model.addAttribute("message", success ? "プロフィールを更新しました！" : "更新時にエラーが発生しました。");

	    return "profile";
	}


	
	@Autowired
	private EventService eventService;

	@PostMapping("/get-events")
	@ResponseBody
	public List<Map<String, Object>> getEventsByDate(@RequestBody Map<String, String> request) {
	    logger.info("/get-events メソッドの呼び出し: {}", request);
	    String eventType = request.get("eventType");
	    String startDateStr = request.get("startDate");
	    String endDateStr = request.get("endDate");

	    return eventService.getEventsByDate(startDateStr, endDateStr, eventType);
	}

	@PostMapping("/set-event")
	@ResponseBody
	@Transactional
	public ResponseEntity<Map<String, Object>> registerEvent(@RequestBody Map<String, Object> postMessage, HttpSession session) {
	    logger.info("/set-event メソッドの呼び出し: {}", postMessage);

	    Object loginUser = session.getAttribute("loginUser");
	    if (loginUser == null) {
	        Map<String, Object> errorResponse = new HashMap<>();
	        errorResponse.put("error", "ログインが必要です。");
	        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
	    }

	    Integer userId = (Integer) loginUser;
	    Map<String, Object> response = eventService.registerEvent(postMessage, userId);
	    return ResponseEntity.ok(response);
	}

	
	// イベントの詳細
    @Value("${base.url}")
    private String baseUrl;
	@GetMapping("/event-detail")
	public String showEventDetail(@RequestParam Integer eventId, Model model, HttpSession session) {
	    logger.info("/event-detail ページ表示");

	    Object loginUser = session.getAttribute("loginUser");
	    
	    EventDetail detail = eventService.fetchEventDetail(eventId);
	    List<String> restrictions = eventService.fetchRestrictionLabels(eventId);
	    List<Map<String, Object>> participants = eventService.fetchParticipants(eventId);

	    // 主催者がページを要求しているか確認
	    boolean isOwner = loginUser != null && loginUser.equals(detail.userId);
	    model.addAttribute("isOwner", isOwner);
	    
	    model.addAttribute("eventDetail", detail);
	    model.addAttribute("restrictions", restrictions);
	    model.addAttribute("participants", participants);

	    boolean isLoggedIn = (loginUser != null);
	    model.addAttribute("isLoggedIn", isLoggedIn);

	    boolean isParticipating = isLoggedIn && eventService.isParticipating((Integer) loginUser, eventId);
	    model.addAttribute("isParticipating", isParticipating);

	    String eventUrl = baseUrl + "/event-detail?eventId=" + eventId;
	    model.addAttribute("eventUrl", eventUrl);

	    return "event-detail";
	}
	
	// イベント削除ボタンをクリックした際の処理
	@PostMapping("/delete-event")
	public String deleteEvent(@RequestParam("eventId") Integer eventId, HttpSession session) {
	    Object loginUser = session.getAttribute("loginUser");
	    if (loginUser == null) {
	        return "redirect:/login";
	    }
	    
	    // 削除が完了したらイベントを削除
	    eventService.deleteEventById(eventId, (Integer) loginUser);
	    return "redirect:/profile";
	}

	// イベント参加ボタンをクリックした際の処理
	@PostMapping("/toggle-participation")
	public String toggleParticipation(@RequestParam Integer eventId, HttpSession session) {
	    logger.info("/toggle-participation メソッドの呼び出し: {}", eventId);

	    Object loginUser = session.getAttribute("loginUser");
	    if (loginUser == null) {
	        return "redirect:/login";
	    }

	    Integer userId = (Integer) loginUser;

	    eventService.toggleParticipation(eventId, userId);
	    // 詳細画面にリダイレクト
	    return "redirect:/event-detail?eventId=" + eventId;
	}
	
	@GetMapping("/user-profile")
	public String showUserProfile(@RequestParam("userId") Integer userId, Model model, HttpSession session) {
	    logger.info("/user-profile ページ表示");

	    Map<String, Object> profileData = userService.getUserProfileById(userId);

	    boolean isLoggedIn = (session.getAttribute("loginUser") != null);
	    model.addAttribute("isLoggedIn", isLoggedIn);
	    model.addAttribute("userName", profileData.get("userName"));
	    model.addAttribute("userGender", profileData.get("userGender"));
	    model.addAttribute("userProfile", profileData.get("userProfile"));
	    model.addAttribute("breforeParticipatingEvents", profileData.get("beforeEvents"));
	    model.addAttribute("afterParticipatingEvents", profileData.get("afterEvents"));

	    return "user-profile";
	}

	@Autowired
	private CommentService commentService;

	@PostMapping("/post-comment")
	@ResponseBody
	@Transactional
	public ResponseEntity<?> postComment(@RequestBody Map<String, Object> postComment, HttpSession session) {
	    logger.info("/post-comment メソッドの呼び出し: {}", postComment);

	    Object loginUser = session.getAttribute("loginUser");
	    if (loginUser == null) {
	        return ResponseEntity.status(HttpStatus.FORBIDDEN)
	            .body(Map.of("error", "ログインが必要です。"));
	    }

	    Integer userId = (Integer) loginUser;
	    return commentService.postComment(postComment, userId);
	}


	@PostMapping("/get-comments")
	@ResponseBody
	public ResponseEntity<List<Map<String, Object>>> getComments(@RequestBody Map<String, Object> payload) {
	    logger.info("/get-comments メソッドの呼び出し: {}", payload);
	    return commentService.getComments(payload.get("eventId"));
	}

}
