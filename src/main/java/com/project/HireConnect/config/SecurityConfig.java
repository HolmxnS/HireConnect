package com.project.HireConnect.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import com.project.HireConnect.repository.PasswordResetTokenRepository;
import com.project.HireConnect.security.CustomLoginSuccessHandler;
import com.project.HireConnect.security.CustomOAuth2SuccessHandler;
import com.project.HireConnect.security.CustomUsuarioDetailsService;

@Configuration
public class SecurityConfig {

        private final CustomLoginSuccessHandler successHandler;
        private final CustomUsuarioDetailsService userDetailsService;
        private final CustomOAuth2SuccessHandler oauth2SuccessHandler;

        public SecurityConfig(CustomLoginSuccessHandler successHandler,
                        CustomUsuarioDetailsService userDetailsService,
                        PasswordResetTokenRepository passwordResetTokenRepository,
                        CustomOAuth2SuccessHandler oauth2SuccessHandler) {
                this.successHandler = successHandler;
                this.userDetailsService = userDetailsService;
                this.oauth2SuccessHandler = oauth2SuccessHandler;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .userDetailsService(userDetailsService)
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(HttpMethod.GET,
                                                                "/", "/index", "/login", "/register", "/styles/**",
                                                                "/source/**",
                                                                "/register/postulante",
                                                                "/register/postulante/paso1",
                                                                "/register/postulante/paso2",
                                                                "/register/verificar-codigo",
                                                                "/register/empresa",
                                                                "/forgot-password", "/reset-password",
                                                                "/js/**", "/images/**", "/webjars/**")
                                                .permitAll()
                                                .requestMatchers("/images/lightning.png").permitAll()

                                                .requestMatchers(HttpMethod.POST, "/register", "/register/postulante",
                                                                "/register/postulante/paso1",
                                                                "/register/postulante/paso2",
                                                                "/register/oauth/postulante",
                                                                "/register/oauth/empresa",
                                                                "/register/empresa",
                                                                "/register/verificar-codigo",
                                                                "/register/reenviar-codigo",
                                                                "/forgot-password", "/reset-password",
                                                                "/empresa/crear", "/empresa/vincular")
                                                .permitAll()

                                                .requestMatchers(HttpMethod.POST,
                                                                "/postulante/completar_perfil_postulante",
                                                                "/empresa/completar_perfil_empresa")
                                                .hasAnyRole("PERSONA", "EMPRESA")

                                                // ✅ Chat IA - permite POST autenticado con rol PERSONA
                                                .requestMatchers(HttpMethod.POST, "/postulante/chat").hasRole("PERSONA")

                                                .requestMatchers("/postulante/descargarCV/**")
                                                .hasAnyRole("PERSONA", "EMPRESA", "ADMIN_PLATAFORMA")

                                                .requestMatchers("/admin/**").hasRole("ADMIN_PLATAFORMA")
                                                .requestMatchers("/empresa/**").hasRole("EMPRESA")
                                                .requestMatchers("/postulante/**").hasRole("PERSONA")

                                                .requestMatchers("/admin/dashboard").authenticated()

                                                .anyRequest().authenticated())
                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .loginProcessingUrl("/login")
                                                .failureUrl("/login?error=true")
                                                .usernameParameter("username")
                                                .passwordParameter("password")
                                                .successHandler(successHandler)
                                                .permitAll())

                                .oauth2Login(oauth2 -> oauth2
                                                .loginPage("/login")
                                                .successHandler(oauth2SuccessHandler))

                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/")
                                                .deleteCookies("JSESSIONID", "remember-me")
                                                .permitAll())
                                .csrf(csrf -> csrf.disable())
                                .headers(headers -> headers.frameOptions(frame -> frame.disable()));

                return http.build();
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
                return authConfig.getAuthenticationManager();
        }
}