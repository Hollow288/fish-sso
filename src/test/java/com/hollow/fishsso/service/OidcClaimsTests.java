package com.hollow.fishsso.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hollow.fishsso.config.SsoProperties;
import com.hollow.fishsso.controller.OidcDiscoveryController;
import com.hollow.fishsso.controller.dto.ConsentContextResponse;
import com.hollow.fishsso.controller.dto.LoginRequest;
import com.hollow.fishsso.controller.dto.ResetPasswordRequest;
import com.hollow.fishsso.controller.dto.SendResetCodeRequest;
import com.hollow.fishsso.controller.dto.UserInfoResponse;
import com.hollow.fishsso.exception.SsoException;
import com.hollow.fishsso.model.AccessToken;
import com.hollow.fishsso.model.ClientRegistration;
import com.hollow.fishsso.model.SessionInfo;
import com.hollow.fishsso.model.UserAccount;
import com.hollow.fishsso.repository.AuthCodeStore;
import com.hollow.fishsso.repository.ClientAccessRepository;
import com.hollow.fishsso.repository.ClientRepository;
import com.hollow.fishsso.repository.ConsentStore;
import com.hollow.fishsso.repository.RefreshTokenStore;
import com.hollow.fishsso.repository.SessionStore;
import com.hollow.fishsso.repository.TokenStore;
import com.hollow.fishsso.repository.UserRepository;
import com.hollow.fishsso.service.dto.UserInfoView;
import com.nimbusds.jwt.JWTClaimsSet;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 不依赖外部 MySQL、Redis 或实际 JWT 密钥的 OIDC 声明回归测试。 */
class OidcClaimsTests {
    @TempDir
    Path temporaryDirectory;

    private final ObjectMapper mapper = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    private final UserRepository users = mock(UserRepository.class);
    private final ClientRepository clients = mock(ClientRepository.class);
    private final SessionStore sessions = mock(SessionStore.class);
    private final TokenStore tokens = mock(TokenStore.class);
    private final PasswordEncoder passwords = mock(PasswordEncoder.class);
    private SsoProperties properties;
    private JwtService jwt;
    private SsoService sso;
    private UserAccount user;

    @BeforeEach
    void setUp() {
        properties = new SsoProperties();
        properties.setIssuer("https://sso.example.test");
        properties.setSessionTtl(Duration.ofHours(1));
        properties.setAccessTokenTtl(Duration.ofMinutes(30));
        properties.setIdTokenTtl(Duration.ofMinutes(30));
        properties.getJwt().setKeyStorePath(temporaryDirectory.resolve("jwt.properties").toString());
        jwt = new JwtService(properties);
        jwt.init();
        sso = new SsoService(users, clients, mock(ClientAccessRepository.class), sessions,
                mock(AuthCodeStore.class), tokens, mock(RefreshTokenStore.class), mock(ConsentStore.class),
                passwords, properties, mock(LoginProtectionService.class), jwt);
        user = new UserAccount("user-001", "alice", "password-hash", "爱丽丝", "alice@example.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {"openid", "openid profile", "openid email", "openid profile email"})
    void userInfoAndIdTokenExposeTheSameAuthorizedClaims(String scope) throws Exception {
        List<String> scopes = List.of(scope.split(" "));
        JsonNode info = userInfo(scopes);
        JWTClaimsSet idToken = jwt.parseAndVerify(
                jwt.generateIdToken(user.getId(), "client", scopes, "nonce", user));

        assertThat(info.get("sub").asText()).isEqualTo(user.getId()).isEqualTo(idToken.getSubject());
        assertThat(info.has("username")).isFalse();
        assertThat(idToken.getClaim("username")).isNull();
        for (String claim : List.of("preferred_username", "name", "email", "email_verified")) {
            if (info.has(claim)) {
                assertThat(mapper.treeToValue(info.get(claim), Object.class)).isEqualTo(idToken.getClaim(claim));
            } else {
                assertThat(idToken.getClaim(claim)).isNull();
            }
        }
        assertThat(info.has("preferred_username")).isEqualTo(scopes.contains("profile"));
        assertThat(info.has("name")).isEqualTo(scopes.contains("profile"));
        assertThat(info.has("email")).isEqualTo(scopes.contains("email"));
        assertThat(info.has("email_verified")).isEqualTo(scopes.contains("email"));
        if (scopes.contains("email")) {
            assertThat(info.get("email_verified").booleanValue()).isFalse();
        }
    }

    @Test
    void missingOptionalProfileAndEmailAreOmitted() throws Exception {
        user.setName(null);
        user.setEmail(null);
        user.setEmailVerified(true);
        JsonNode info = userInfo(List.of("openid", "profile", "email"));
        JWTClaimsSet idToken = jwt.parseAndVerify(jwt.generateIdToken(user.getId(), "client",
                List.of("openid", "profile", "email"), null, user));

        assertThat(info.has("name")).isFalse();
        assertThat(info.has("email")).isFalse();
        assertThat(info.has("email_verified")).isFalse();
        assertThat(idToken.getClaim("name")).isNull();
        assertThat(idToken.getClaim("email")).isNull();
        assertThat(idToken.getClaim("email_verified")).isNull();
        assertThat(user.isEmailVerified()).isFalse();
    }

    @Test
    void emailVerificationIsResetWhenEmailChanges() throws Exception {
        user.setEmailVerified(true);
        assertThat(userInfo(List.of("openid", "email")).get("email_verified").booleanValue()).isTrue();
        user.setEmail(user.getEmail());
        assertThat(user.isEmailVerified()).isTrue();
        user.setEmail("new@example.com");
        assertThat(userInfo(List.of("openid", "email")).get("email_verified").booleanValue()).isFalse();
    }

    @Test
    void renamedLoginNameDoesNotChangeSubjectOrAccountIdentity() throws Exception {
        user.setPreferredUsername("alice-new");
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(sessions.find("session")).thenReturn(Optional.of(
                new SessionInfo("session", user.getId(), Instant.now().plusSeconds(300))));
        UserInfoView current = sso.currentUser("session");
        JWTClaimsSet idToken = jwt.parseAndVerify(jwt.generateIdToken(user.getId(), "client",
                List.of("openid", "profile"), null, user));

        assertThat(current.sub()).isEqualTo("user-001").isEqualTo(idToken.getSubject());
        assertThat(current.preferredUsername()).isEqualTo("alice-new")
                .isEqualTo(idToken.getStringClaim("preferred_username"));
    }

    @Test
    void requestsUsePreferredUsernameAndLoginFindsTheRenamedProperty() throws Exception {
        LoginRequest login = mapper.readValue(
                "{\"preferred_username\":\"alice\",\"password\":\"secret\"}", LoginRequest.class);
        SendResetCodeRequest send = mapper.readValue(
                "{\"preferred_username\":\"alice\",\"email\":\"alice@example.com\"}", SendResetCodeRequest.class);
        ResetPasswordRequest reset = mapper.readValue(
                "{\"preferred_username\":\"alice\",\"new_password\":\"new\",\"code\":\"123456\"}",
                ResetPasswordRequest.class);
        assertThat(send.preferredUsername()).isEqualTo("alice");
        assertThat(reset.preferredUsername()).isEqualTo("alice");
        assertThat(reset.newPassword()).isEqualTo("new");

        when(users.findByPreferredUsername("alice")).thenReturn(Optional.of(user));
        when(passwords.matches("secret", user.getPasswordHash())).thenReturn(true);
        SessionInfo expected = new SessionInfo("session", user.getId(), Instant.now().plusSeconds(300));
        when(sessions.create(user.getId(), properties.getSessionTtl())).thenReturn(expected);
        assertThat(sso.login(login.preferredUsername(), login.password(), "127.0.0.1", "test")).isSameAs(expected);
        verify(users).findByPreferredUsername("alice");
    }

    @Test
    void legacyLoginFieldIsNotAnAlias() throws Exception {
        LoginRequest old = mapper.readValue("{\"username\":\"alice\",\"password\":\"secret\"}", LoginRequest.class);
        assertThat(old.preferredUsername()).isNull();
        assertThatThrownBy(() -> sso.login(old.preferredUsername(), old.password(), "127.0.0.1", "test"))
                .isInstanceOf(SsoException.class).hasMessageContaining("preferred_username");
    }

    @Test
    void consentAndDiscoveryAdvertiseTheStandardFieldNames() {
        JsonNode consent = mapper.valueToTree(new ConsentContextResponse("client", "https://app.test/callback",
                List.of("openid"), "alice", "爱丽丝", null, null, "openid", null, null));
        assertThat(consent.get("preferred_username").asText()).isEqualTo("alice");
        assertThat(consent.get("name").asText()).isEqualTo("爱丽丝");
        assertThat(consent.has("username")).isFalse();
        assertThat(consent.has("preferredUsername")).isFalse();
        assertThat(consent.has("displayName")).isFalse();

        JsonNode metadata = mapper.valueToTree(new OidcDiscoveryController(properties, jwt).openidConfiguration());
        assertThat(metadata.get("claims_supported")).contains(
                mapper.valueToTree("preferred_username"), mapper.valueToTree("email_verified"));
    }

    private JsonNode userInfo(List<String> scopes) {
        String access = jwt.generateAccessToken(user.getId(), "client", scopes);
        when(tokens.find(access)).thenReturn(Optional.of(
                new AccessToken(access, "client", user.getId(), scopes, Instant.now().plusSeconds(300))));
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(clients.findByClientId("client")).thenReturn(Optional.of(new ClientRegistration()));
        UserInfoView info = sso.userInfo(access);
        return mapper.valueToTree(new UserInfoResponse(info.sub(), info.preferredUsername(), info.name(),
                info.email(), info.emailVerified()));
    }
}
