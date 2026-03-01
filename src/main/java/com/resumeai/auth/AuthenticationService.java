package com.resumeai.auth;

import com.resumeai.candidate.CandidateProfile;
import com.resumeai.candidate.CandidateProfileRepository;
import com.resumeai.recruiter.RecruiterProfile;
import com.resumeai.recruiter.RecruiterProfileRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

    private final UserRepository repository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthenticationService(UserRepository repository, CandidateProfileRepository candidateProfileRepository,
                                 RecruiterProfileRepository recruiterProfileRepository, PasswordEncoder passwordEncoder,
                                 JwtService jwtService, AuthenticationManager authenticationManager) {
        this.repository = repository;
        this.candidateProfileRepository = candidateProfileRepository;
        this.recruiterProfileRepository = recruiterProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (repository.existsByEmail(request.email())) {
            throw new IllegalStateException("Email already in use");
        }

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        repository.save(user);

        switch (user.getRole()) {
            case CANDIDATE -> {
                CandidateProfile profile = new CandidateProfile();
                profile.setUser(user);
                profile.setPreferredContactEmail(user.getEmail());
                candidateProfileRepository.save(profile);
            }
            case RECRUITER -> {
                RecruiterProfile profile = new RecruiterProfile();
                profile.setUser(user);
                profile.setCompanyName("Update Company Name"); // Default placeholder
                recruiterProfileRepository.save(profile);
            }
        }

        CustomUserDetails userDetails = new CustomUserDetails(user);
        var jwtToken = jwtService.generateToken(userDetails);
        return new AuthResponse(jwtToken, user.getId(), user.getName(), user.getEmail(), user.getRole().name());
    }

    public AuthResponse authenticate(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );
        User user = repository.findByEmail(request.email())
                .orElseThrow();
        CustomUserDetails userDetails = new CustomUserDetails(user);
        var jwtToken = jwtService.generateToken(userDetails);
        return new AuthResponse(jwtToken, user.getId(), user.getName(), user.getEmail(), user.getRole().name());
    }

    @Transactional
    public AuthResponse completeOAuth(CompleteOAuthRequest request) {
        User tempUser = new User();
        tempUser.setEmail(request.email());
        tempUser.setRole(com.resumeai.common.Role.CANDIDATE);

        if (!jwtService.isTokenValid(request.tempToken(), new CustomUserDetails(tempUser))) {
            throw new IllegalArgumentException("Invalid temporary token");
        }

        if (repository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("User with this email already exists");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setName(request.name());
        user.setRole(request.role());
        // For OAuth2 users, they don't have a password. Generating a random strong hash.
        user.setPasswordHash(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
        repository.save(user);

        switch (user.getRole()) {
            case CANDIDATE -> {
                CandidateProfile profile = new CandidateProfile();
                profile.setUser(user);
                profile.setPreferredContactEmail(user.getEmail());
                candidateProfileRepository.save(profile);
            }
            case RECRUITER -> {
                RecruiterProfile profile = new RecruiterProfile();
                profile.setUser(user);
                profile.setCompanyName("Update Company Name");
                recruiterProfileRepository.save(profile);
            }
        }

        CustomUserDetails userDetails = new CustomUserDetails(user);
        var jwtToken = jwtService.generateToken(userDetails);
        return new AuthResponse(jwtToken, user.getId(), user.getName(), user.getEmail(), user.getRole().name());
    }
}
