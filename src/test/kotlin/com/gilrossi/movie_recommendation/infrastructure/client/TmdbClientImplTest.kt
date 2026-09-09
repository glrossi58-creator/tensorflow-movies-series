package com.gilrossi.movie_recommendation.infrastructure.client

import com.gilrossi.movie_recommendation.exception.TmdbResourceNotFoundException
import com.gilrossi.movie_recommendation.exception.TmdbTimeoutException
import com.gilrossi.movie_recommendation.exception.TmdbUnavailableException
import io.netty.channel.ChannelOption
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.time.Duration
import java.util.concurrent.TimeUnit

class TmdbClientImplTest {
    private lateinit var server: MockWebServer

    @BeforeEach fun start() { server = MockWebServer().also { it.start() } }
    @AfterEach fun stop() { server.shutdown() }

    @Test
    fun `search parses json and encodes query`() = runTest {
        server.enqueue(json("""{"page":1,"results":[{"id":603,"title":"The Matrix","overview":"x"}],"total_pages":1,"total_results":1}"""))
        val response = client().searchMovie("The Matrix")
        val request = server.takeRequest()
        assertEquals(603, response.results.single().id)
        assertEquals("The Matrix", request.requestUrl?.queryParameter("query"))
    }

    @Test
    fun `parses movie details and credits`() = runTest {
        server.enqueue(json("""{"id":603,"title":"Matrix","overview":null,"genres":[],"release_date":"1999-03-30","poster_path":null,"vote_average":8.2}"""))
        server.enqueue(json("""{"id":603,"cast":[{"id":1,"name":"Actor","character":"Neo","order":0}],"crew":[]}"""))
        assertEquals("Matrix", client().getMovieDetails(603).title)
        assertEquals("Actor", client().getMovieCredits(603).cast.single().name)
    }

    @Test
    fun `parses series details and aggregate credits`() = runTest {
        server.enqueue(json("""{"id":10,"name":"Show","overview":null,"genres":[],"first_air_date":"2020-01-01","poster_path":null,"created_by":[{"id":5,"name":"Creator"}]}"""))
        server.enqueue(json("""{"id":10,"cast":[{"id":2,"name":"Actor","order":0}]}"""))
        assertEquals("Creator", client().getSeriesDetails(10).createdBy.single().name)
        assertEquals(2, client().getSeriesAggregateCredits(10).cast.single().id)
    }

    @Test
    fun `maps 404 and 5xx without leaking response body`() {
        server.enqueue(MockResponse().setResponseCode(404).setBody("secret upstream details"))
        assertThrows(TmdbResourceNotFoundException::class.java) { kotlinx.coroutines.runBlocking { client().getMovieDetails(1) } }
        server.enqueue(MockResponse().setResponseCode(503).setBody("secret upstream details"))
        val exception = assertThrows(TmdbUnavailableException::class.java) { kotlinx.coroutines.runBlocking { client().getMovieDetails(1) } }
        assertEquals("TMDB respondeu com status 503.", exception.message)
    }

    @Test
    fun `maps response timeout`() {
        server.enqueue(json("""{"id":1,"title":"Late","overview":null,"genres":[],"release_date":null,"poster_path":null,"vote_average":0}""")
            .setBodyDelay(500, TimeUnit.MILLISECONDS))
        assertThrows(TmdbTimeoutException::class.java) { kotlinx.coroutines.runBlocking { client(Duration.ofMillis(50)).getMovieDetails(1) } }
    }

    private fun client(timeout: Duration = Duration.ofSeconds(2)): TmdbClientImpl {
        val http = HttpClient.create().option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 500).responseTimeout(timeout)
        val webClient = WebClient.builder()
            .baseUrl(server.url("/").toString().removeSuffix("/"))
            .defaultHeader("Authorization", "Bearer test-token")
            .clientConnector(ReactorClientHttpConnector(http)).build()
        return TmdbClientImpl(webClient, "test-token")
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = [401,403,429,500,502])
    fun `translates authorization rate limit and upstream errors`(status: Int) {
        server.enqueue(MockResponse().setResponseCode(status).setBody("secret upstream body"))
        val error = assertThrows(TmdbUnavailableException::class.java) { kotlinx.coroutines.runBlocking { client().searchUniversal("Matrix",null) } }
        org.junit.jupiter.api.Assertions.assertFalse(error.message.orEmpty().contains("secret"))
    }

    @Test fun `malformed json and missing token have friendly errors`() {
        server.enqueue(json("{broken json"))
        assertThrows(TmdbUnavailableException::class.java) { kotlinx.coroutines.runBlocking { client().searchUniversal("Matrix",null) } }
        val noToken = TmdbClientImpl(WebClient.create(server.url("/").toString()), "")
        assertThrows(com.gilrossi.movie_recommendation.exception.TmdbConfigurationException::class.java) { kotlinx.coroutines.runBlocking { noToken.searchMovie("Matrix") } }
    }

    @Test fun `parses real person credits without inventing creator role`() = runTest {
        server.enqueue(json("""{"results":[{"id":9,"name":"Person","known_for_department":"Acting","profile_path":null}]}"""))
        server.enqueue(json("""{"id":9,"cast":[{"id":10,"media_type":"movie"}],"crew":[{"id":12,"media_type":"tv","job":"Writer"}]}"""))
        assertEquals("Person",client().searchUniversal("Person",com.gilrossi.movie_recommendation.discovery.DiscoveryType.PERSON).results.single().name)
        assertEquals("Writer",client().getPersonCredits(9).crew.single().job)
    }

    private fun json(body: String) = MockResponse().setResponseCode(200)
        .setHeader("Content-Type", "application/json").setBody(body)
}
