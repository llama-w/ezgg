package com.matching.ezgg.domain.matching.infra.es.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@Profile("!test") // 테스트 환경에서는 Elasticsearch 설정을 적용하지 않음
@EnableElasticsearchRepositories(basePackages = "com.matching.ezgg.domain.matching.infra.es.repository")
public class ElasticsearchConfig {

	@Value("${es.httpHost}")
	private String esHttpHost;

	@Bean("es")
	public ElasticsearchClient elasticsearchClient() {
		log.info("Elasticsearch 클라이언트 생성 - 호스트: {}", esHttpHost);

		RestClient restClient = RestClient.builder(
			new HttpHost(esHttpHost, 9200)
		).build();

		RestClientTransport transport = new RestClientTransport(
			restClient, new JacksonJsonpMapper()
		);

		return new ElasticsearchClient(transport);
	}
}
