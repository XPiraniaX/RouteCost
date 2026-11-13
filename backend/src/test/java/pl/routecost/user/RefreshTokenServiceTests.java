package pl.routecost.user;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RefreshTokenServiceTests {

    private final RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
    private final RefreshTokenService service = new RefreshTokenService(repository, Duration.ofDays(7));

    @Test
    void storesOnlySha256HashOfRandomRefreshToken() {
        User user = mock(User.class);

        RefreshTokenGrant grant = service.issue(user);

        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(saved.capture());
        assertNotEquals(grant.rawToken(), saved.getValue().getTokenHash());
        assertEquals(service.hash(grant.rawToken()), saved.getValue().getTokenHash());
        assertTrue(saved.getValue().getExpiresAt().isAfter(Instant.now().plus(Duration.ofDays(6))));
    }

    @Test
    void rotatesValidTokenAndMarksPreviousTokenRevoked() {
        User user = mock(User.class);
        when(user.isEnabled()).thenReturn(true);
        UUID family = UUID.randomUUID();
        RefreshToken current = new RefreshToken(
                user, service.hash("valid-refresh-token"), family,
                Instant.now().plus(Duration.ofDays(1)), Instant.now()
        );
        when(repository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(current));

        RefreshTokenGrant rotated = service.rotate("valid-refresh-token").orElseThrow();

        assertNotEquals("valid-refresh-token", rotated.rawToken());
        assertNotNull(current.getRevokedAt());
        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(saved.capture());
        RefreshToken replacement = saved.getValue();
        assertEquals(family, replacement.getFamilyId());
        assertEquals(service.hash(rotated.rawToken()), replacement.getTokenHash());
    }

    @Test
    void rejectsUnknownRefreshTokenWithoutWritingToDatabase() {
        when(repository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.empty());

        assertTrue(service.rotate("unknown-token").isEmpty());
        verify(repository, never()).save(any(RefreshToken.class));
    }

    @Test
    void rejectsExpiredTokenAndRevokesItsStoredRecord() {
        User user = mock(User.class);
        when(user.isEnabled()).thenReturn(true);
        RefreshToken expired = new RefreshToken(
                user, service.hash("expired-token"), UUID.randomUUID(),
                Instant.now().minusSeconds(1), Instant.now().minus(Duration.ofDays(8))
        );
        when(repository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(expired));

        assertFalse(service.rotate("expired-token").isPresent());
        assertNotNull(expired.getRevokedAt());
        verify(repository, never()).save(any(RefreshToken.class));
    }

    @Test
    void replayedTokenRevokesAllStillActiveTokensInItsFamily() {
        UUID family = UUID.randomUUID();
        RefreshToken replayed = new RefreshToken(
                mock(User.class), service.hash("replayed-token"), family,
                Instant.now().plus(Duration.ofDays(1)), Instant.now()
        );
        replayed.revoke(Instant.now(), "replacement-hash");
        when(repository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(replayed));

        assertTrue(service.rotate("replayed-token").isEmpty());
        verify(repository).revokeActiveFamily(org.mockito.ArgumentMatchers.eq(family), any(Instant.class));
    }
}
