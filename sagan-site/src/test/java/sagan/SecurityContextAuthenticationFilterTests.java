package sagan;

import sagan.SecurityConfig.SecurityContextAuthenticationFilter;

import java.util.List;

import org.junit.After;
import org.junit.Test;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.Assert.assertEquals;

public class SecurityContextAuthenticationFilterTests {

    private SecurityContextAuthenticationFilter filter = new SecurityContextAuthenticationFilter();

    @After
    public void clean() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testSuccessfulAuthentication() throws Exception {
        List<GrantedAuthority> roleUser = AuthorityUtils.commaSeparatedStringToAuthorityList("ROLE_USER");
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("githubusername",  null, roleUser);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User", "githubusername");
        assertEquals(authentication, filter.attemptAuthentication(request, null));
    }

    @Test
    public void testUnsuccessfulAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertEquals(null, filter.attemptAuthentication(request, null));
    }

}
