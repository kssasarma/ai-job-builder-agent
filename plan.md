# Execution Plan

## EPIC B-1: Fix Candidate Matching Pipeline

### Story B-1.1: Default `openToOpportunities` to `true` for new candidates
1. Edit `src/main/java/com/resumeai/candidate/CandidateProfile.java`. Update the `@Column(name = "open_to_opportunities")` to initialize `openToOpportunities = true`.
2. Create DB migration `src/main/resources/db/migration/V6__default_open_to_opportunities.sql` with query `UPDATE candidate_profiles SET open_to_opportunities = TRUE WHERE open_to_opportunities = FALSE AND id IN (SELECT candidate_id FROM resumes);`.
3. Verify file edits using `ls -l src/main/resources/db/migration/V6__default_open_to_opportunities.sql`.

### Story B-1.2, B-1.3, B-1.4: Create Candidate Browse and Detail Endpoints
1. Create a `CandidateDto` containing necessary fields in `com.resumeai.recruiter`.
2. Update `CandidateProfileRepository` to add methods:
   - `@Query(value = "SELECT c FROM CandidateProfile c WHERE c.openToOpportunities = true AND (:skills IS NULL OR (EXISTS (SELECT 1 FROM unnest(c.skills) s WHERE s IN :skills))) AND (:minAtsScore IS NULL OR (SELECT MAX(r.atsScore) FROM Resume r WHERE r.candidate.id = c.id) >= :minAtsScore)", nativeQuery = false)` Page<CandidateProfile> findCandidates(List<String> skills, Integer minAtsScore, Pageable pageable);
3. Create `RecruiterCandidateController.java` to handle `/api/recruiter/candidates` and `/api/recruiter/candidates/{id}`.
4. Verify by checking if the files are created and compiling them.

### Story B-1.5: Persist async operation status to the database
1. Create DB migration `V7__async_operations.sql` with table `async_operations`.
2. Create `AsyncOperation` entity and `AsyncOperationRepository.java`.
3. Edit `AiService.java` to use `AsyncOperationRepository` instead of `ConcurrentHashMap`. Check `save` and `findByReferenceIdAndType` methods.

## EPIC B-2: Secure API Responses & Prevent Data Leaks
1. Create Response DTOs: `RecruiterProfileDto`, `JobPostingDto`, `CandidateMatchDto`, `ResumeDto`, `UserDto`.
2. Add `@JsonIgnore` to `User.java` for `passwordHash`.
3. Edit `JobPostingController.java` to return `JobPostingDto` instead of `JobPosting` and `CandidateMatchDto` in `getMatches`.
4. Edit `ResumeController.java`, `TailoringController.java` to verify ownership before proceeding with `scoreResumeAsync` or `analyzeCompatibility`.

## EPIC B-3: Token Refresh & Session Management
1. Create DB migration `V8__refresh_tokens.sql` with `refresh_tokens` table.
2. Create `RefreshToken` entity and `RefreshTokenRepository`.
3. Edit `JwtService.java` to generate refresh tokens and set access token expiry to 15m.
4. Edit `AuthResponse.java` to include `expiresIn` and `refreshToken`.
5. Edit `AuthenticationController.java` to add `/refresh` endpoint. Add logic to validate refresh token and generate a new access token.

## EPIC B-4: Configuration & Environment Safety
1. Edit `OAuth2AuthenticationSuccessHandler.java` and `WebConfig.java` to use `app.frontend.url` from environment properties.
2. Edit `application.yml` to set `app.frontend.url: ${FRONTEND_URL:http://localhost:5173}` and change `OPENAI_API_KEY` to `${OPENAI_API_KEY}` without default.
3. Edit `RateLimitFilter.java` to change AI endpoints limit to 20 req/min per user and differentiate by operation.

## EPIC B-5: Onboarding Profile Completion
1. Edit `ProfileSuggestionController.java` to add `/api/candidate/profile/suggestions/{resumeId}/apply` if needed and update `CandidateProfile` with `openToOpportunities = true`.
2. Create `GET /api/candidate/profile/completeness` in `CandidateProfileController.java` calculating percentages based on fields.

## EPIC F-1: Role-Based Auto-Routing & Protected Routes
1. Edit `resumeai-frontend/src/App.tsx` to handle auto-redirect based on role. Add `<Navbar>` component for all authenticated routes.
2. Edit `resumeai-frontend/src/pages/auth/LoginPage.tsx` to redirect authenticated users.
3. Create `resumeai-frontend/src/components/layout/Navbar.tsx` displaying user info, role badge, theme toggle, and logout logic.

## EPIC F-2: Token Lifecycle & Session Management
1. Edit `resumeai-frontend/src/lib/axios.ts` to add response interceptor handling 401s and refreshing token via `/api/auth/refresh`.
2. Edit `resumeai-frontend/src/context/AuthContext.tsx` to fetch `/api/auth/me` on load to validate token. Add backend `/me` endpoint in `AuthenticationController`.

## EPIC F-3: Candidate Onboarding & Profile Completion UX
1. Edit `resumeai-frontend/src/pages/candidate/ResumeDashboard.tsx` to poll for profile suggestion status and display modal.
2. Add `ProfileCompletenessIndicator` component and include it in `CandidateProfilePage.tsx` and `ResumeDashboard.tsx`.
3. Add profile link in Navbar.

## EPIC F-4: Recruiter Candidate Browse & Filter UI
1. Create `resumeai-frontend/src/pages/recruiter/RecruiterCandidateBrowsePage.tsx`.
2. Add table of candidates fetched from `/api/recruiter/candidates` with skill and ATS score filters using debounced inputs.

## EPIC F-5: UI/UX Modernization
1. Add `ErrorBoundary` component to `main.tsx`. Add dark mode provider (`ThemeProvider`) and toggle in Navbar.
2. Make `RecruiterDashboard.tsx` and file uploads responsive.
3. Use `react-helmet-async` for page titles on major pages.
4. Edit `resumeai-frontend/src/pages/auth/RoleSelectionPage.tsx` to add `useEffect` to the react import.

## EPIC F-6: Recruiter Dashboard Enhancements
1. Edit `resumeai-frontend/src/pages/recruiter/RecruiterDashboard.tsx` and `JobCreateModal.tsx` to handle editing/deleting jobs and changing status.
2. Improve polling in `getMatches` using exponential backoff inside `RecruiterDashboard.tsx` or a custom hook.

7. Run Backend tests using `./mvnw test` to ensure changes haven't broken existing functionality. Check Frontend compilation using `npm run build` in the `resumeai-frontend` directory.
8. *Complete pre commit steps*
   - Complete pre commit steps to ensure proper testing, verification, review, and reflection are done.
9. *Submit the change.*
   - Once all tests pass, I will submit the change with a descriptive commit message.
