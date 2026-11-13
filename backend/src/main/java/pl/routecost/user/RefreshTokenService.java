package pl.routecost.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final RefreshTokenRepository repository;
    private final Duration lifetime;

    public RefreshTokenService(
            RefreshTokenRepository repository,
            @Value("${routecost.auth.refresh-token-expiration:P7D}") Duration lifetime
    ) {
        this.repository = repository;
        this.lifetime = lifetime;
    }

    @Transactional
    public RefreshTokenGrant issue(User user) {
        Instant now = Instant.now();
        String rawToken = createRawToken();
        repository.save(new RefreshToken(user, hash(rawToken), UUID.randomUUID(), now.plus(lifetime), now));
        return new RefreshTokenGrant(user, rawToken);
    }

    @Transactional
    public Optional<RefreshTokenGrant> rotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        Instant now = Instant.now();
        Optional<RefreshToken> stored = repository.findByTokenHashForUpdate(hash(rawToken));
        if (stored.isEmpty()) {
            return Optional.empty();
        }

        RefreshToken current = stored.get();
        if (current.getRevokedAt() != null) {
            repository.revokeActiveFamily(current.getFamilyId(), now);
            return Optional.empty();
        }
        if (!current.getUser().isEnabled()) {
            repository.revokeActiveFamily(current.getFamilyId(), now);
            return Optional.empty();
        }
        if (!current.getExpiresAt().isAfter(now)) {
            current.revoke(now, null);
            return Optional.empty();
        }

        String nextRawToken = createRawToken();
        String nextHash = hash(nextRawToken);
        current.revoke(now, nextHash);
        repository.save(new RefreshToken(
                current.getUser(), nextHash, current.getFamilyId(), now.plus(lifetime), now
        ));
        return Optional.of(new RefreshTokenGrant(current.getUser(), nextRawToken));
    }

    @Transactional
    public void revokeFamily(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        repository.findByTokenHashForUpdate(hash(rawToken))
                .ifPresent(token -> repository.revokeActiveFamily(token.getFamilyId(), Instant.now()));
    }

    String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.US_ASCII));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String createRawToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
