package com.resumeai.candidate;

import java.time.LocalDate;

public record CareerEventRequest(String type, String title, String description, LocalDate eventDate) {}
