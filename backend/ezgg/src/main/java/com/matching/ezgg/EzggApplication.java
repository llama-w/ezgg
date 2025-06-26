package com.matching.ezgg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableJpaAuditing
@SpringBootApplication
// @EnableElasticsearchRepositories(basePackages = "com.matching.ezgg.domain.matching.infra.es.repository")
@EnableScheduling
public class EzggApplication {

	public static void main(String[] args) {
		SpringApplication.run(EzggApplication.class, args);
	}

}
