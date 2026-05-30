package itmo.blps.config;

import itmo.blps.security.AppLoginModule;
import itmo.blps.security.RoleAuthorityGranter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.jaas.AuthorityGranter;
import org.springframework.security.authentication.jaas.DefaultJaasAuthenticationProvider;
import org.springframework.security.authentication.jaas.memory.InMemoryConfiguration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import javax.security.auth.login.AppConfigurationEntry;
import java.util.Collections;

@Configuration
@Profile("!worker")
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static boolean isUnknownApiPath(jakarta.servlet.http.HttpServletRequest request) {
        String uri = request.getRequestURI();
        String cp = request.getContextPath();
        String apiPath = cp + "/api/";
        
        return uri.startsWith(apiPath) &&
                !uri.startsWith(apiPath + "auth/") &&
                !uri.startsWith(apiPath + "listings") &&
                !uri.startsWith(apiPath + "seller") &&
                !uri.startsWith(apiPath + "inquiries") &&
                !uri.startsWith(apiPath + "notifications") &&
                !uri.startsWith(apiPath + "weekly") &&
                !uri.startsWith(apiPath + "admin") &&
                !uri.startsWith(apiPath + "webhooks");
    }


    @Bean
    public DefaultJaasAuthenticationProvider jaasAuthenticationProvider() {
        DefaultJaasAuthenticationProvider provider = new DefaultJaasAuthenticationProvider();
        
        AppConfigurationEntry configurationEntry = new AppConfigurationEntry(
                AppLoginModule.class.getName(),
                AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                Collections.emptyMap()
        );
        InMemoryConfiguration configuration = new InMemoryConfiguration(
                Collections.singletonMap("SPRINGSEC", new AppConfigurationEntry[]{configurationEntry})
        );

        provider.setConfiguration(configuration);
        provider.setLoginContextName("SPRINGSEC");
        provider.setAuthorityGranters(new AuthorityGranter[]{new RoleAuthorityGranter()});

        return provider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(SecurityConfig::isUnknownApiPath).permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/webhooks/bitrix").permitAll()
                        .requestMatchers("/api/auth/login", "/api/auth/register", "/api/listings/search").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/listings/*").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults())
                .authenticationProvider(jaasAuthenticationProvider());
        return http.build();
    }
}
