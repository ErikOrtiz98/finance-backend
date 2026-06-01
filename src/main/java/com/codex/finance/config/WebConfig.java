package com.codex.finance.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Redirigir la raíz a index.html
        registry.addViewController("/").setViewName("forward:/index.html");
        // Manejar rutas del frontend (para SPA)
        registry.addViewController("/{x:[\\w\\-]+}").setViewName("forward:/index.html");
        registry.addViewController("/{x:^(?!api|auth|actuator|static).*$}/**/{y:[\\w\\-]+}").setViewName("forward:/index.html");
    }
}