package com.gilrossi.movie_recommendation.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.reactive.config.CorsRegistry
import org.springframework.web.reactive.config.WebFluxConfigurer

@Configuration
@EnableScheduling
class ProductConfig(@Value("\${app.cors.allowed-origins:http://localhost:3000}") private val origins: String) : WebFluxConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        val allowed = origins.split(',').map(String::trim).filter(String::isNotEmpty)
        require(allowed.none { '*' in it }) { "Configure origens CORS explícitas." }
        registry.addMapping("/**").allowedOrigins(*allowed.toTypedArray())
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("Content-Type").allowCredentials(false).maxAge(3600)
    }
}
