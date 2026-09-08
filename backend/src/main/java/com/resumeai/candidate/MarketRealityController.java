package com.resumeai.candidate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.resumeai.recruiter.JobPosting;
import com.resumeai.recruiter.JobPostingRepository;

/**
 * Market Reality Check: a light compensation benchmark sourced entirely from live
 * postings already on the platform — no external salary-data feed required.
 */
@RestController
@RequestMapping("/api/candidate/market-reality")
public class MarketRealityController {

    private final JobPostingRepository jobPostingRepository;

    public MarketRealityController(JobPostingRepository jobPostingRepository) {
        this.jobPostingRepository = jobPostingRepository;
    }

    @GetMapping
    public ResponseEntity<MarketRealityResponse> get(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String location) {

        List<JobPosting> postings = jobPostingRepository.findForSalaryBenchmark(
                blankToNull(title), blankToNull(location));

        List<Integer> midpoints = new ArrayList<>();
        for (JobPosting posting : postings) {
            int[] range = SalaryRangeParser.parse(posting.getSalaryRange());
            if (range != null) {
                midpoints.add((range[0] + range[1]) / 2);
            }
        }
        Collections.sort(midpoints);

        Integer min = midpoints.isEmpty() ? null : midpoints.get(0);
        Integer max = midpoints.isEmpty() ? null : midpoints.get(midpoints.size() - 1);
        Integer median = midpoints.isEmpty() ? null : midpoints.get(midpoints.size() / 2);

        return ResponseEntity.ok(new MarketRealityResponse(title, location, midpoints.size(), min, median, max));
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
