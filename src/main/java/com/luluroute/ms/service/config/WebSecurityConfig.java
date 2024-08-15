package com.luluroute.ms.service.config;

import com.luluroute.ms.service.util.ShipmentConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

import static com.luluroute.ms.service.util.ShipmentConstants.SOAP_INBOUND_URI;

@EnableWebSecurity
@EnableGlobalMethodSecurity(
        securedEnabled = true,
        prePostEnabled = true
)
@RequiredArgsConstructor
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    private static final String[] AUTH_WHITELIST = {
            "/swagger-resources/**", "/v2/api-docs", "/actuator/**", "/swagger-ui/**", SOAP_INBOUND_URI + "/**"
    };

    @Autowired
    private final AppConfig config;

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        // Set unauthorized requests exception handler
        http.cors().and().csrf().disable()
                .authorizeRequests()
                .antMatchers(AUTH_WHITELIST).permitAll()
                .antMatchers("/v1/api/shipment/**").hasAnyAuthority(
                        ShipmentConstants.SCOPE_PREFIX + config.getRequestScope(),
                        ShipmentConstants.SCOPE_PREFIX + config.getLabelsScope())
                .anyRequest().authenticated()
                .and().oauth2ResourceServer().jwt();

    }

}
