package com.codex.finance;

import com.codex.finance.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackageClasses = AppProperties.class)
@ComponentScan(basePackages = "com.codex.finance")
public class FinanceApplication {
	public static void main(String[] args) {
		SpringApplication.run(FinanceApplication.class, args);
	}
}