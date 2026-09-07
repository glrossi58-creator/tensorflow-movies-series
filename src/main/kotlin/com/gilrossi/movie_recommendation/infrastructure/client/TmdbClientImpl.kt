package com.gilrossi.movie_recommendation.infrastructure.client

import com.gilrossi.movie_recommendation.application.port.out.TmdbClient
import com.gilrossi.movie_recommendation.dto.response.TmdbCreditsResponse
import com.gilrossi.movie_recommendation.dto.response.TmdbMovieDetailsResponse
import com.gilrossi.movie_recommendation.dto.response.TmdbSearchResponse
import com.gilrossi.movie_recommendation.dto.response.TmdbSeriesAggregateCreditsResponse
import com.gilrossi.movie_recommendation.dto.response.TmdbSeriesDetailsResponse
import com.gilrossi.movie_recommendation.exception.TmdbResourceNotFoundException
import com.gilrossi.movie_recommendation.exception.TmdbConfigurationException
import com.gilrossi.movie_recommendation.exception.TmdbTimeoutException
import com.gilrossi.movie_recommendation.exception.TmdbUnavailableException
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException

@Component
class TmdbClientImpl(
    @Qualifier("tmdbWebClient") private val webClient: WebClient,
    @Value("\${tmdb.read-access-token:}") private val accessToken: String
) : TmdbClient {

    override suspend fun searchMovie(title: String): TmdbSearchResponse = execute(
        webClient.get().uri { builder ->
            builder.path("/search/movie").queryParam("query", title).build()
        },
        TmdbSearchResponse::class.java
    )

    override suspend fun getMovieDetails(tmdbId: Int): TmdbMovieDetailsResponse = execute(
        webClient.get().uri("/movie/{id}", tmdbId),
        TmdbMovieDetailsResponse::class.java
    )

    override suspend fun getMovieCredits(tmdbId: Int): TmdbCreditsResponse = execute(
        webClient.get().uri("/movie/{id}/credits", tmdbId),
        TmdbCreditsResponse::class.java
    )

    override suspend fun getSeriesDetails(tmdbId: Int): TmdbSeriesDetailsResponse = execute(
        webClient.get().uri("/tv/{id}", tmdbId),
        TmdbSeriesDetailsResponse::class.java
    )

    override suspend fun getSeriesAggregateCredits(tmdbId: Int): TmdbSeriesAggregateCreditsResponse = execute(
        webClient.get().uri("/tv/{id}/aggregate_credits", tmdbId),
        TmdbSeriesAggregateCreditsResponse::class.java
    )

    private suspend fun <T : Any> execute(
        request: WebClient.RequestHeadersSpec<*>,
        responseType: Class<T>
    ): T {
        if (accessToken.isBlank()) throw TmdbConfigurationException()

        return try {
            request.retrieve().bodyToMono(responseType).awaitSingle()
        } catch (exception: WebClientResponseException.NotFound) {
            throw TmdbResourceNotFoundException()
        } catch (exception: WebClientResponseException) {
            if (exception.causeChainContainsTimeout()) throw TmdbTimeoutException()
            throw TmdbUnavailableException("TMDB respondeu com status ${exception.statusCode.value()}.")
        } catch (exception: WebClientRequestException) {
            if (exception.causeChainContainsTimeout()) throw TmdbTimeoutException()
            throw TmdbUnavailableException()
        }
    }

    private fun Throwable.causeChainContainsTimeout(): Boolean = generateSequence(this) { it.cause }
        .any { cause -> cause.javaClass.simpleName.contains("Timeout", ignoreCase = true) }
}
