package com.resumeai.common;

import java.util.UUID;

public record RatingCreateRequest(UUID jobApplicationId, int score, String comment) {}
