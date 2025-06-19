package com.example.demo.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public class EventDetail {
	public Integer eventId;
    public String eventTitle;
    public Integer userId;
    public String host;
    public LocalDate eventDate;
    public LocalTime startTime;
    public LocalTime endTime;
    public String eventMessage;
    public Integer recruitMin;
    public Integer recruitMax;
}

