package com.resumeai.candidate;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.resumeai.auth.CustomUserDetails;

/** Career record, not a resume: a candidate-maintained timeline. */
@RestController
@RequestMapping("/api/candidate/career-events")
public class CareerEventController {

    private final CareerEventRepository careerEventRepository;
    private final CandidateProfileRepository candidateProfileRepository;

    public CareerEventController(CareerEventRepository careerEventRepository, CandidateProfileRepository candidateProfileRepository) {
        this.careerEventRepository = careerEventRepository;
        this.candidateProfileRepository = candidateProfileRepository;
    }

    private CandidateProfile currentCandidate(UUID userId) {
        return candidateProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Candidate profile not found"));
    }

    @GetMapping
    public ResponseEntity<?> list(@AuthenticationPrincipal CustomUserDetails userDetails) {
        CandidateProfile candidate = currentCandidate(userDetails.getUser().getId());
        return ResponseEntity.ok(careerEventRepository.findByCandidateIdOrderByEventDateDesc(candidate.getId()));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CareerEventRequest request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (request.title() == null || request.title().isBlank() || request.eventDate() == null || request.type() == null) {
            return ResponseEntity.badRequest().body("type, title, and eventDate are required");
        }
        CandidateProfile candidate = currentCandidate(userDetails.getUser().getId());

        CareerEvent event = new CareerEvent();
        event.setCandidate(candidate);
        event.setType(request.type());
        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setEventDate(request.eventDate());

        return ResponseEntity.ok(careerEventRepository.save(event));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails userDetails) {
        CandidateProfile candidate = currentCandidate(userDetails.getUser().getId());
        CareerEvent event = careerEventRepository.findById(id).orElse(null);
        if (event == null || !event.getCandidate().getId().equals(candidate.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        careerEventRepository.delete(event);
        return ResponseEntity.noContent().build();
    }
}
