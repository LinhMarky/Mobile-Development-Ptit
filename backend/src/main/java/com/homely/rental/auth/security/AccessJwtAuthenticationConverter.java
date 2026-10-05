package com.homely.rental.auth.security;

import com.homely.rental.auth.service.TokenService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.BearerTokenErrorCodes;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/** A typed converter can also be safely discovered by Spring MVC's conversion service. */
public final class AccessJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private final JwtAuthenticationConverter delegate = new JwtAuthenticationConverter();

    public AccessJwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthorityPrefix("");
        authorities.setAuthoritiesClaimName("roles");
        delegate.setJwtGrantedAuthoritiesConverter(authorities);
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        if (!TokenService.TOKEN_TYPE_ACCESS.equals(jwt.getClaimAsString(TokenService.TOKEN_TYPE_CLAIM))) {
            throw new OAuth2AuthenticationException(BearerTokenErrorCodes.INVALID_TOKEN);
        }
        return delegate.convert(jwt);
    }
}
