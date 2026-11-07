package pl.routecost.user;

import pl.routecost.user.dto.AuthResponse;
import pl.routecost.user.dto.UserProfileResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.Duration;
import java.util.List;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final Duration tokenLifetime;

    public JwtService(
            JwtEncoder jwtEncoder,
            @Value("${routecost.jwt.expiration:PT15M}") Duration tokenLifetime
    ) {
        this.jwtEncoder = jwtEncoder;
        this.tokenLifetime = tokenLifetime;
    }

    public AuthResponse createAuthResponse(User user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(tokenLifetime);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("routecost")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("firstName", user.getFirstName())
                .claim("lastName", user.getLastName())
                .claim("roles", List.of(user.getRole().name()))
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        UserProfileResponse profile = new UserProfileResponse(
                user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getRole()
        );

        return new AuthResponse(token, "Bearer", expiresAt, profile);
    }
}
