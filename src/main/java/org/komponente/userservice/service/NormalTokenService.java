package org.komponente.userservice.service;

import io.jsonwebtoken.Claims;

public interface NormalTokenService {
    String generate(Claims claims);

    Claims parseToken(String jwt);
}
