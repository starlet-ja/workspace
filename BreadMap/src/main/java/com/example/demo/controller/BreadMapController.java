package com.example.demo.controller;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class BreadMapController {
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	/* ストア画面を表示する */
	@GetMapping("freelance")
	public String showFreeLance() {
		return "freelance";
	}
	
	
	/* メッセージをDBに登録する */
	@PostMapping("post-message")
	@ResponseBody
	@Transactional
	public Boolean registerEvent(@RequestBody Map<String, Object> postMessage){
		Integer userId = (Integer)postMessage.get("userId");
		LocalDateTime startTime = LocalDateTime.parse((String)postMessage.get("startTime"));
		LocalDateTime endTime = LocalDateTime.parse((String)postMessage.get("endTime"));
		String message = (String)postMessage.get("message");
	    Double lat = ((Number) postMessage.get("lat")).doubleValue();
	    Double lng = ((Number) postMessage.get("lng")).doubleValue();
	    // TODO:制限（restrict）を追加する
		// List<Integer> resList = (List<Integer>)postMessage.get("restrict");
		// DBに値を登録
	    KeyHolder keyHolder = new GeneratedKeyHolder();
	    // イベントテーブルにデータ登録
	    jdbcTemplate.update(con -> {
	        PreparedStatement ps = con.prepareStatement(
	            "INSERT INTO event (start_time, end_time, lat, lng, message, user_id) VALUES (?, ?, ?, ?, ?, ?)",
	            Statement.RETURN_GENERATED_KEYS);
	        ps.setObject(1, startTime);
	        ps.setObject(2, endTime);
	        ps.setDouble(3, lat);
	        ps.setDouble(4, lng);
	        ps.setString(5, message);
	        ps.setInt(6, userId);
	        return ps;
	    }, keyHolder);
	    // Number eventId = (Number) keyHolder.getKeys().get("event_id");
	    // TODO:制限テーブル登録
	    // jdbcTemplate.update("INSERT INTO restriction (event_id, restriction) VALUES (?, ?)",
	        // eventId, restrict;
		return true;
	}
	
	/* ストア画面を表示する */
	@GetMapping("store")
	public String showView() {
		return "store";
	}
	
	/* 対象範囲の登録IDを返す */
	@PostMapping("area-info")
	@ResponseBody
	public List<Integer> areaInfo(@RequestBody List<List<Double>> bounds) {
		double ido_a = bounds.get(0).get(0);
		double ido_b = bounds.get(1).get(0);
		double keido_a = bounds.get(0).get(1);
		double keido_b = bounds.get(1).get(1);
		List<Integer> user_id = jdbcTemplate.queryForList(
			    "SELECT user_id FROM location WHERE (ido BETWEEN ? AND ?) " +
			    "AND (keido BETWEEN ? AND ?)",
			    Integer.class,
			    ido_a, ido_b, keido_a, keido_b
			);
		System.out.println(user_id);
		return user_id;
	}	

	/* 対象範囲の登録IDリストページを返す */
	@PostMapping("pro-list")
	public String proList(@RequestParam String list) {
	    // list = "1,3"
	    String[] ids = list.split(",");
	    for (String id : ids) {
	        System.out.println("ID: " + id);
	    }
	    return "prolist";
	}

}
