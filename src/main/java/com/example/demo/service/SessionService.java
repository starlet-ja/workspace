package com.example.demo.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Service;

@Service
public class SessionService {

    /**
     * セッションに loginUser が存在しなければ Cookie から復元する
     */
    public void restoreLoginUserFromCookie(HttpServletRequest request, HttpSession session) {
        if (session.getAttribute("loginUser") == null && request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("rememberMeUserId".equals(cookie.getName())) {
                    session.setAttribute("loginUser", Integer.valueOf(cookie.getValue()));
                    break;
                }
            }
        }
    }

    /**
     * ログイン状態を確認
     */
    public boolean isLoggedIn(HttpSession session) {
        return session.getAttribute("loginUser") != null;
    }

    /**
     * ログインユーザーの ID を取得
     */
    public Integer getLoginUserId(HttpSession session) {
        Object loginUser = session.getAttribute("loginUser");
        return (loginUser instanceof Integer) ? (Integer) loginUser : null;
    }
}
