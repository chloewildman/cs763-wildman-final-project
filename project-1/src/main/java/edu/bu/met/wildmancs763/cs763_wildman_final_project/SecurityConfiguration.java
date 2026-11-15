/*
AI Contribution Weight: 99% 
Human Reviewed and Commented: 1%
Reviewed, commented out understanding throughout, reorganized to my preferred format
*/
package edu.bu.met.wildmancs763.cs763_wildman_final_project;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {

	// Deny-all permit-by-exception policy, any request not explicitly allowed below will be denied.
	@Bean
	SecurityFilterChain studentApiSecurity(HttpSecurity http) throws Exception {
		return http
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(HttpMethod.GET, "/courses/*/projects").hasRole("STUDENT")
						.requestMatchers(HttpMethod.GET, "/courses/*/projects/*").hasRole("STUDENT")
						.requestMatchers(HttpMethod.GET, "/courses/*/projects/*/posts").hasRole("STUDENT")
						.requestMatchers(HttpMethod.GET, "/courses/*/projects/*/posts/*").hasRole("STUDENT")
						.requestMatchers(HttpMethod.POST, "/courses/*/projects/*/posts").hasRole("STUDENT")
						.requestMatchers(HttpMethod.PUT, "/courses/*/projects/*/posts/*").hasRole("STUDENT")
						.requestMatchers(HttpMethod.GET, "/courses/*/projects/*/posts/*/comments").hasRole("STUDENT")
						.requestMatchers(HttpMethod.GET, "/courses/*/projects/*/posts/*/comments/*").hasRole("STUDENT")
						.requestMatchers(HttpMethod.POST, "/courses/*/projects/*/posts/*/comments").hasRole("STUDENT")
						.requestMatchers(HttpMethod.PUT, "/courses/*/projects/*/posts/*/comments/*").hasRole("STUDENT")
						.anyRequest().denyAll())
				.httpBasic(Customizer.withDefaults())
				.build();
	}

	// Configure Spring password hashing for fake accounts
	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	// Configures demo in-memory student accounts associated with demo password (not secure, demo only!)
	@Bean
	UserDetailsService demoStudents(@Value("${DEMO_STUDENT_PASSWORD}") String password, PasswordEncoder passwordEncoder) {
		List<UserDetails> students = List.of("lgilmore", "ldanes", "sstjames").stream()
				.map(username -> org.springframework.security.core.userdetails.User
						.withUsername(username)
						.password(passwordEncoder.encode(password))
						.roles("STUDENT")
						.build())
				.toList();
		return new InMemoryUserDetailsManager(students);
	}
}
