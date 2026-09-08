package com.resumeai.common;

import java.util.UUID;

public record ReviewRequestCreateRequest(String subjectType, UUID subjectId, String reason) {}
