package com.narainox.spring_security;

import com.narainox.spring_security.entity.UserEntity;
import com.narainox.spring_security.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@SpringBootApplication
public class SpringSecurityApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringSecurityApplication.class, args);
	}

	@Bean
	CommandLineRunner commandLineRunner(UserRepository userRepository,PasswordEncoder passwordEncoder)
	{
		return args -> {
			UserEntity manager = new UserEntity();
			manager.setUserName("manager");
			manager.setPassword(passwordEncoder.encode("password"));
			manager.setRoles("ROLE_MANAGER");

			UserEntity admin = new UserEntity();
			admin.setUserName("admin");
			admin.setPassword(passwordEncoder.encode("password"));
			admin.setRoles("ROLE_ADMIN,ROLE_MANAGER");

			userRepository.saveAll(List.of(manager,admin));

		};
	}

}
