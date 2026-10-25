package com.kido.testapp.config;

import java.util.function.Supplier;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

/**
 * CSRF for a JavaScript SPA (design.md D3), per the Spring Security 6.x reference "Single-Page Applications".
 * ({@code csrf.spa()} only exists from Spring Security 7.)
 * <ul>
 *   <li>The SPA reads the raw token from the {@code XSRF-TOKEN} cookie and sends it in {@code X-XSRF-TOKEN},
 *       so header values are resolved as plain tokens.</li>
 *   <li>Anything else (e.g. a form parameter) keeps BREACH protection via the XOR handler.</li>
 *   <li>The deferred token is loaded on every request so the cookie is always issued.</li>
 * </ul>
 */
final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

    private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
    private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
        xor.handle(request, response, csrfToken);
        csrfToken.get(); // render the token so CookieCsrfTokenRepository writes the cookie
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        String header = request.getHeader(csrfToken.getHeaderName());
        return (StringUtils.hasText(header) ? plain : xor).resolveCsrfTokenValue(request, csrfToken);
    }
}
