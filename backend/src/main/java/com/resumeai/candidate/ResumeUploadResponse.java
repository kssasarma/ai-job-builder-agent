package com.resumeai.candidate;

import java.util.UUID;

public record ResumeUploadResponse(UUID resumeId, String textPreview) {
}
