package com.resumeai.recruiter;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record JobPostingRequest(
        @NotBlank(message = "Title is required") String title,
        @NotBlank(message = "Company is required") String company,
        @NotBlank(message = "Description is required") String description,
        List<String> requiredSkills,
        Integer experienceMin,
        Integer experienceMax,
        String location,
        String salaryRange,
        String jobType,
        String status
) {
}
