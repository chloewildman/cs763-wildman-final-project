package edu.bu.met.wildmancs763.cs763_wildman_final_project;

import java.util.List;
import edu.bu.met.wildmancs763.cs763_wildman_final_project.entities.UserRole;
import edu.bu.met.wildmancs763.cs763_wildman_final_project.entities.User;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import edu.bu.met.wildmancs763.cs763_wildman_final_project.repositories.UserRepository;

@Configuration
public class SecurityConfiguration {

	// Adding additional role-based rules to original AI gen, finish with deny by default
	@Bean
	SecurityFilterChain studentApiSecurity(HttpSecurity http) throws Exception {
		return http
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authorize -> authorize
						// User management endpoints
						.requestMatchers(HttpMethod.GET, "/users").hasRole("ADMIN")
						.requestMatchers(HttpMethod.POST, "/users").hasRole("ADMIN")
						.requestMatchers(HttpMethod.PUT, "/users/*").hasRole("ADMIN")
						.requestMatchers(HttpMethod.DELETE, "/users/*").hasRole("ADMIN")

						// Course management endpoints
						.requestMatchers(HttpMethod.GET, "/courses*").hasAnyRole("STUDENT", "PROFESSOR", "ADMIN")
						.requestMatchers(HttpMethod.POST, "/courses*").hasRole("ADMIN")
						.requestMatchers(HttpMethod.PUT, "/courses*").hasAnyRole("ADMIN", "PROFESSOR")
						.requestMatchers(HttpMethod.DELETE, "/courses*").hasAnyRole("ADMIN", "PROFESSOR")

						// Project management endpoints
						.requestMatchers(HttpMethod.GET, "/courses/*/projects").hasAnyRole("STUDENT", "PROFESSOR", "ADMIN")
						.requestMatchers(HttpMethod.GET, "/courses/*/projects/*").hasAnyRole("STUDENT", "PROFESSOR", "ADMIN")
						.requestMatchers(HttpMethod.POST, "/courses/*/projects").hasAnyRole("PROFESSOR", "ADMIN")
						.requestMatchers(HttpMethod.PUT, "/courses/*/projects/*").hasAnyRole("PROFESSOR", "ADMIN")
						.requestMatchers(HttpMethod.DELETE, "/courses/*/projects/*").hasAnyRole("PROFESSOR", "ADMIN")

						// Post management endpoints within projects
						.requestMatchers(HttpMethod.GET, "/courses/*/projects/*/posts").hasAnyRole("STUDENT", "PROFESSOR", "ADMIN")
						.requestMatchers(HttpMethod.POST, "/courses/*/projects/*/posts").hasAnyRole("STUDENT", "PROFESSOR", "ADMIN")
						.requestMatchers(HttpMethod.GET, "/courses/*/projects/*/posts/*").hasAnyRole("STUDENT", "PROFESSOR", "ADMIN")
						.requestMatchers(HttpMethod.PUT, "/courses/*/projects/*/posts/*").hasAnyRole("STUDENT", "PROFESSOR", "ADMIN")
						.requestMatchers(HttpMethod.DELETE, "/courses/*/projects/*/posts/*").hasAnyRole("STUDENT", "PROFESSOR", "ADMIN")
						
						// Comment management endpoints within posts
						.requestMatchers(HttpMethod.GET, "/courses/*/projects/*/posts/*/comments").hasAnyRole("STUDENT", "PROFESSOR", "ADMIN")
						.requestMatchers(HttpMethod.GET, "/courses/*/projects/*/posts/*/comments/*").hasAnyRole("STUDENT", "PROFESSOR", "ADMIN")
						.requestMatchers(HttpMethod.POST, "/courses/*/projects/*/posts/*/comments").hasAnyRole("STUDENT", "PROFESSOR", "ADMIN")
						.requestMatchers(HttpMethod.PUT, "/courses/*/projects/*/posts/*/comments/*").hasAnyRole("STUDENT", "PROFESSOR", "ADMIN")
						.requestMatchers(HttpMethod.DELETE, "/courses/*/projects/*/posts/*/comments/*").hasAnyRole("STUDENT", "PROFESSOR", "ADMIN")

						.anyRequest().denyAll())
				.httpBasic(Customizer.withDefaults())
				.build();
	}

	// Creating all users - removing unnecessary complexity and security risk of authentication mechanisms for week 1 :)
	@Bean
	CommandLineRunner seedUsers(UserRepository userRepository) {
		return args -> {
			userRepository.saveAll(List.of(
					new User(5, "lgilmore", "Lorelai", "Gilmore",
							"lgilmore@example.edu", UserRole.STUDENT),

					new User(6, "ldanes", "Luke", "Danes",
							"ldanes@example.edu", UserRole.STUDENT),

					new User(7, "sstjames", "Sookie", "St. James",
							"sstjames@example.edu", UserRole.STUDENT),
							
					new User(8, "jdoe", "John", "Doe",
							"jdoe@example.edu", UserRole.PROFESSOR),

					new User(9, "jadoe", "Jane", "Doe",
							"jadoe@example.edu", UserRole.PROFESSOR),

					new User(10, "admin", "Admin", "Admin",
							"admin@example.edu", UserRole.ADMIN)));
		};
	}
}
