package com.gilrossi.movie_recommendation.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import reactor.netty.http.client.HttpClient
import io.netty.channel.ChannelOption
import java.time.Duration

@Configuration
class WebClientConfig {

    @Value("\${tmdb.base-url}")
    private lateinit var baseUrl: String

    @Value("\${tmdb.read-access-token:}")
    private lateinit var accessToken: String

    @Value("\${tmdb.connect-timeout:3s}")
    private lateinit var connectTimeout: Duration

    @Value("\${tmdb.response-timeout:8s}")
    private lateinit var responseTimeout: Duration

    @Bean
    fun tmdbWebClient(): WebClient {
        val httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout.toMillis().toInt())
            .responseTimeout(responseTimeout)

        val builder = WebClient.builder()
            .baseUrl(baseUrl)
            .clientConnector(ReactorClientHttpConnector(httpClient))

        if (accessToken.isNotBlank()) {
            builder.defaultHeader("Authorization", "Bearer $accessToken")
        }

        return builder.build()
    }
}
