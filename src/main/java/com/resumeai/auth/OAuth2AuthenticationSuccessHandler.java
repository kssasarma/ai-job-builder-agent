package com.resumeai.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final String frontendUrl;

    public OAuth2AuthenticationSuccessHandler(JwtService jwtService, UserRepository userRepository, RefreshTokenRepository refreshTokenRepository, @Value("${app.frontend.url:http://localhost:5173}") String frontendUrl) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        Optional<User> userOptional = userRepository.findByEmail(email);

        String targetUrl;
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            CustomUserDetails userDetails = new CustomUserDetails(user);
            String token = jwtService.generateToken(userDetails);

            refreshTokenRepository.deleteByUserId(user.getId());
            RefreshToken refreshToken = new RefreshToken();
            refreshToken.setUser(user);
            refreshToken.setToken(java.util.UUID.randomUUID().toString());
            refreshToken.setExpiryDate(java.time.Instant.now().plusMillis(7 * 24 * 60 * 60 * 1000L));
            refreshTokenRepository.save(refreshToken);

            targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth2/redirect")
                    .queryParam("token", token)
                    .queryParam("refreshToken", refreshToken.getToken())
                    .queryParam("userId", user.getId())
                    .queryParam("name", user.getName())
                    .queryParam("email", user.getEmail())
                    .queryParam("role", user.getRole().name())
                    .build().toUriString();
        } else {
            // New user, redirect to role selection
            // We can generate a temporary token or just pass user info
            User tempUser = new User();
            tempUser.setEmail(email);
            tempUser.setName(name);
            tempUser.setRole(com.resumeai.common.Role.CANDIDATE); // Dummy role for token generation, it will be validated
            String tempToken = jwtService.generateToken(new CustomUserDetails(tempUser));
            targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/register/role-selection")
                    .queryParam("email", email)
                    .queryParam("name", name)
                    .queryParam("tempToken", tempToken)
                    .build().toUriString();
        }

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
