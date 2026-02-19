package com.app.config;

import com.app.core.security.JWTFilter;
import com.app.security.security.OAuth2LoginSuccessHandler;
import com.app.security.security.UserDetailsServiceImplCustom;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

        private final UserDetailsServiceImplCustom userDetailsService;
        private final OAuth2LoginSuccessHandler oauth2LoginSuccessHandler;

        public SecurityConfig(UserDetailsServiceImplCustom userDetailsService,
                              OAuth2LoginSuccessHandler oauth2LoginSuccessHandler) {
                this.userDetailsService = userDetailsService;
                this.oauth2LoginSuccessHandler = oauth2LoginSuccessHandler;
        }

        @Bean
        @Order(1)
        public SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {
                http
                                .securityMatcher("/admin/**", "/VAADIN/**", "/sw.js", "/manifest.webmanifest", "/images/**", "/icons/**")
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/VAADIN/**", "/sw.js", "/manifest.webmanifest", "/images/**", "/icons/**")
                                                .permitAll()
                                                .requestMatchers("/admin/login", "/admin/css/**", "/admin/js/**",
                                                                "/admin/images/**")
                                                .permitAll()
                                                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "OPERATOR", "SUPPORT"))
                                .formLogin(form -> form
                                                .loginPage("/admin/login")
                                                .loginProcessingUrl("/admin/login")
                                                .defaultSuccessUrl("/admin/dashboard-native", false)
                                                .permitAll())
                                .logout(logout -> logout
                                                .logoutUrl("/admin/logout")
                                                .logoutSuccessUrl("/admin/login?logout")
                                                .permitAll())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                                .csrf(csrf -> csrf.ignoringRequestMatchers("/VAADIN/**"));

                http.authenticationProvider(daoAuthenticationProvider());

                return http.build();
        }

        @Bean
        @Order(2)
        public SecurityFilterChain apiFilterChain(HttpSecurity http, JWTFilter jwtFilter) throws Exception {
                http
                                .securityMatcher("/api/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/actuator/**")
                                .csrf(csrf -> csrf.disable())
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                                                                "/api/v1/register/**", "/api/v1/login")
                                                .permitAll()
                                                .requestMatchers("/api/v1/public/**", "/api/public/**",
                                                                "/api/v1/forgot-password",
                                                                "/api/v1/reset-password",
                                                                "/api/v1/verify-email",
                                                                "/api/webhooks/razorpay",
                                                                "/api/v1/logistics/track/**")
                                                .permitAll()
                                                .requestMatchers("/actuator/**").permitAll()
                                                .requestMatchers("/api/v1/user/**").hasAnyAuthority("USER", "ADMIN")
                                                .requestMatchers("/api/v1/admin/**").hasAuthority("ADMIN") // API admin
                                                                                                           // endpoints
                                                .anyRequest().authenticated())
                                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                                                (request, response, authException) -> response.sendError(
                                                                HttpServletResponse.SC_UNAUTHORIZED,
                                                                "Unauthorized")))
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .oauth2Login(oauth2 -> oauth2.successHandler(oauth2LoginSuccessHandler));

                http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
                http.authenticationProvider(daoAuthenticationProvider());

                return http.build();
        }

	@Bean
	public DaoAuthenticationProvider daoAuthenticationProvider() {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		//provider.setUserDetailsService(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder());
		return provider;
	}

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
                return configuration.getAuthenticationManager();
        }

	@Bean
	@Order(0)
	public SecurityFilterChain adminServerFilterChain(HttpSecurity http) throws Exception {
		// Spring Boot Admin Server requires some specific allowances
		http
				.securityMatcher("/sba-server/**", "/instances/**", "/assets/**")
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/sba-server/assets/**", "/sba-server/login").permitAll()
						.anyRequest().hasAuthority("ADMIN"))
				.formLogin(form -> form.loginPage("/sba-server/login").permitAll())
				.logout(logout -> logout.logoutUrl("/sba-server/logout").permitAll())
				.csrf(csrf -> csrf.disable());

		return http.build();
	}
}

