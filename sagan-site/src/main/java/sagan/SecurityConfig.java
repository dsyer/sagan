package sagan;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.header.writers.HstsHeaderWriter;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;

/**
 * Site-wide web security configuration.
 */
@Configuration
class SecurityConfig {

    static final String SIGNIN_SUCCESS_PATH = "/signin/success";

    @Configuration
    protected static class SigninAuthenticationConfig extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            configureHeaders(http.headers());
            http.requestMatchers().antMatchers("/**").and() //
                    .authorizeRequests() //
                    .antMatchers("/admin/**").authenticated() //
                    .antMatchers(HttpMethod.GET, "/project_metadata/**").permitAll() //
                    .antMatchers(HttpMethod.HEAD, "/project_metadata/**").permitAll() //
                    .antMatchers("/project_metadata/**").authenticated() //
                    .anyRequest().permitAll() //
                    .and()
                    .addFilterBefore(authenticationFilter(),
                            AnonymousAuthenticationFilter.class).anonymous().and().csrf()
                    .ignoringAntMatchers("/project_metadata/**");
        }

        private static void configureHeaders(HeadersConfigurer<?> headers) throws Exception {
            HstsHeaderWriter writer = new HstsHeaderWriter(false);
            writer.setRequestMatcher(AnyRequestMatcher.INSTANCE);
            headers.contentTypeOptions().and().xssProtection()
                    .and().cacheControl().and().addHeaderWriter(writer).frameOptions();
        }

        // Not a @Bean because we explicitly do not want it added automatically by
        // Bootstrap to all requests
        protected Filter authenticationFilter() {
            return new SecurityContextAuthenticationFilter();
        }
    }

    /**
     * Thin filter for Spring Security chain that simply transfers an existing
     * {@link Authentication} from the {@link SecurityContext} if there is one. This is
     * useful when authentication actually happened in a controller, rather than in the
     * filter chain itself.
     */
    static class SecurityContextAuthenticationFilter implements Filter {

        private static final String USER = "X-User";

        Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
                throws AuthenticationException, IOException, ServletException {
            String user = request.getHeader(USER);
            if (user != null) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user,
                        "",
                        AuthorityUtils.createAuthorityList("ROLE_USER"));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            return SecurityContextHolder.getContext().getAuthentication();
        }

        @Override
        public void init(FilterConfig filterConfig) throws ServletException {
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
                ServletException {
            attemptAuthentication((HttpServletRequest) request, (HttpServletResponse) response);
            chain.doFilter(request, response);
        }

        @Override
        public void destroy() {
        }
    }

}
