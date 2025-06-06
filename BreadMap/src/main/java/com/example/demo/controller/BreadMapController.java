package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
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
