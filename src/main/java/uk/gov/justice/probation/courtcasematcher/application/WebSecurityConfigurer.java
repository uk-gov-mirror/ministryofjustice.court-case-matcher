package uk.gov.justice.probation.courtcasematcher.application;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;

@EnableWebSecurity
public class WebSecurityConfigurer extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                // Can't have CSRF protection as requires session
                .csrf().disable()
                .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)

                .and().oauth2Client()

                .and().authorizeRequests()
                    .antMatchers("/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated();
    }
}
