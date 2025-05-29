package com.example.demo.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class BreadMapController {
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	/* ストア画面を表示する */
	@GetMapping("store")
	public String showView() {
		return "store";
	}
	
	/* プロフィール画面を表示する */
	@GetMapping("profile")
	public String showProfile() {
		return "profile";
	}
	
	/* 新規登録 */
	@PostMapping("entry")
	@ResponseBody
	public Map<String, Object> returnLoginData(@RequestParam 
			String userName, String passWord) {
		Map<String, Object> userDataMap = new HashMap<>();
		// 対象のユーザー名が登録されているか確認する
		try {
			jdbcTemplate.queryForMap 
			("SELECT name FROM user_info WHERE name=?", userName);
		}catch(Exception e){
		// DBにユーザー情報（名前、パスワード）を登録する
		jdbcTemplate.update("INSERT INTO user_info (name,password) "
				+ "VALUES(?,?)",userName,passWord);
		// ユーザー情報（名前、ユーザーID）を取得する
		userDataMap = jdbcTemplate.queryForMap
				("SELECT id, name FROM user_info WHERE name=?", userName);
		return userDataMap;
		}
		// DBにすでにユーザー登録されている為、登録不可
		return userDataMap;
	}
	
	
	/* 各お店の情報を返す */
	@PostMapping("store-info")
	@ResponseBody
	public Map<String, Object> returnJson(@RequestParam String storeName) {
		// DBから指定されたお店の情報を取得する
		Map<String, Object> store_info =
				jdbcTemplate.queryForMap
				("SELECT id,adress FROM store_info WHERE name=?", storeName);
		Integer storeId = (Integer) store_info.get("id");
		String storeAdress = (String) store_info.get("adress");
		List<Map<String, Object>> messageMaps=
				jdbcTemplate.queryForList
				("SELECT ui.name, uc.user_comment FROM user_comment AS uc"
						+ " JOIN user_info AS ui ON uc.user_id = ui.id"
						+ " JOIN store_info AS si ON uc.store_id = si.id"
						+ " WHERE si.name=?", storeName);
		Map<String, Object> storeMap = new HashMap<>();
		storeMap.put("name", storeName);
		storeMap.put("storeId", storeId);
		storeMap.put("adress", storeAdress);
		storeMap.put("message", messageMaps);
		return storeMap;
	}
	
	/* 各お店にコメントを残す */
	/* 各お店の情報を返す */
	@PostMapping("user-comment")
	@ResponseBody
	public boolean regCmt(@RequestParam
			String storeName, String userName, String postMessage,
			Integer storeId, Integer userId) {
		try {
			// DBにフォームから送られてきた情報を登録する
			jdbcTemplate.update("INSERT INTO user_comment "
					+ "(user_id,store_id,user_comment) "
					+ "VALUES(?,?,?)",userId,storeId,postMessage );			
		} catch (Exception e) {
			// 例外発生時の処理
			System.out.println("エラー");
		}
		return true;
	}
}
