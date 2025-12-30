package com.GeekPaperAssistant;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import top.continew.starter.web.annotation.EnableGlobalResponse;

/**
 * @author 席崇援
 * @since 2025-10-03
 */
@EnableGlobalResponse
@SpringBootApplication
@EnableAsync
@EnableCaching
@MapperScan("com.GeekPaperAssistant.mapper")
@EnableNeo4jRepositories(basePackages = "com.GeekPaperAssistant.repository.graph")
public class IcanApplication {

	public static void main(String[] args) {
		SpringApplication.run(IcanApplication.class, args);
		System.out.println("===========================================================\n"+
		 "帮助文档 UI (Knife4j): " + "http://localhost:8080/doc.html\n"
		 + "===========================================================");

	}

}