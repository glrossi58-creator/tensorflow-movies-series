package com.gilrossi.movie_recommendation.repository

import com.gilrossi.movie_recommendation.application.port.out.CatalogImportPort
import com.gilrossi.movie_recommendation.model.AppUser
import com.gilrossi.movie_recommendation.model.Content
import com.gilrossi.movie_recommendation.model.ContentActor
import com.gilrossi.movie_recommendation.model.ContentGenre
import com.gilrossi.movie_recommendation.model.ContentType
import com.gilrossi.movie_recommendation.model.Genre
import com.gilrossi.movie_recommendation.model.Person
import com.gilrossi.movie_recommendation.model.Rating
import com.gilrossi.movie_recommendation.model.RatingTargetType
import com.gilrossi.movie_recommendation.recommendation.ContentSignals
import com.gilrossi.movie_recommendation.recommendation.PgVectorService
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.Instant
import kotlin.test.assertFailsWith

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class PostgresRepositoryIntegrationTest @Autowired constructor(
    private val contents: ContentRepository,
    private val genres: GenreRepository,
    private val people: PersonRepository,
    private val contentGenres: ContentGenreRepository,
    private val contentActors: ContentActorRepository,
    private val contentDirectors: ContentDirectorRepository,
    private val users: AppUserRepository,
    private val ratings: RatingRepository,
    private val pgVector: PgVectorService,
    private val catalogImport: CatalogImportPort
) {
    @Test
    fun `persists content catalog and relations`() = runTest {
        val content = contents.save(Content(null, "Integration Movie", 900001, ContentType.MOVIE, null, null, null))
        val genre = genres.save(Genre(null, 900001, "Integration Genre"))
        val person = people.save(Person(null, 900001, "Integration Person"))
        contentGenres.save(ContentGenre(null, requireNotNull(content.id), requireNotNull(genre.id)))
        contentActors.save(ContentActor(null, requireNotNull(content.id), requireNotNull(person.id)))

        assertEquals(content.id, contents.findByTmdbIdAndType(900001, ContentType.MOVIE)?.id)
        assertEquals(genre.id, genres.findByTmdbId(900001)?.id)
        assertEquals(person.id, people.findByTmdbId(900001)?.id)
        assertEquals(1, contentGenres.findAllByContentId(requireNotNull(content.id)).toList().size)
    }

    @Test
    fun `rating constraints preserve distinct person roles and reject duplicates`() = runTest {
        val now = Instant.now()
        val user = users.save(AppUser(null, "Integration User", "integration-${System.nanoTime()}@example.com", now, now))
        val person = people.save(Person(null, (System.nanoTime() % Int.MAX_VALUE).toInt(), "Role Person"))
        fun rating(type: RatingTargetType, value: Int) = Rating(null, requireNotNull(user.id), type, value, null, person.id, null, now, now)

        ratings.save(rating(RatingTargetType.ACTOR, 5))
        ratings.save(rating(RatingTargetType.DIRECTOR, 2))
        assertFailsWith<DataIntegrityViolationException> { ratings.save(rating(RatingTargetType.ACTOR, 3)) }
        assertEquals(2, ratings.findAllByUserId(requireNotNull(user.id)).toList().size)
    }

    @Test
    fun `pgvector persists embeddings and calculates cosine similarity`() = runTest {
        val now = Instant.now()
        val user = users.save(AppUser(null, "Vector User", "vector-${System.nanoTime()}@example.com", now, now))
        val content = contents.save(Content(null, "Vector Movie", null, ContentType.MOVIE, null, null, null))
        val signals = ContentSignals(content, setOf(1), setOf(2), setOf(3))
        val direct = Rating(999, requireNotNull(user.id), RatingTargetType.MOVIE, 5, content.id, null, null, now, now)

        pgVector.refreshContents(listOf(signals))
        pgVector.refreshUser(requireNotNull(user.id), listOf(direct), listOf(signals))
        val similarities = pgVector.similarities(requireNotNull(user.id))

        assertNotNull(similarities[content.id])
        assertTrue(requireNotNull(similarities[content.id]) > 0.99)
    }

    @Test
    fun `catalog upsert is idempotent and updates mutable fields`() = runTest {
        val tmdbId = (System.nanoTime() % Int.MAX_VALUE).toInt()
        val first = catalogImport.persist(
            Content(null, "Old title", tmdbId, ContentType.SERIES, "old", null, null),
            listOf(Genre(null, tmdbId, "Genre")),
            listOf(Person(null, tmdbId, "Actor")),
            listOf(Person(null, tmdbId + 1, "Creator"))
        )
        val second = catalogImport.persist(
            Content(null, "New title", tmdbId, ContentType.SERIES, "new", null, "/new.jpg"),
            listOf(Genre(null, tmdbId, "Genre")),
            listOf(Person(null, tmdbId, "Actor")),
            listOf(Person(null, tmdbId + 1, "Creator"))
        )

        assertEquals(first.id, second.id)
        assertEquals("New title", second.title)
        assertEquals(1, contentActors.findAllByContentId(requireNotNull(second.id)).toList().size)
        assertEquals(1, contentDirectors.findAllByContentId(requireNotNull(second.id)).toList().size)
    }

    companion object {
        @Container
        @JvmStatic
        val postgres = GenericContainer<Nothing>(DockerImageName.parse("pgvector/pgvector:pg16")).apply {
            withExposedPorts(5432)
            withEnv("POSTGRES_DB", "movie_recommendation_test")
            withEnv("POSTGRES_USER", "postgres")
            withEnv("POSTGRES_PASSWORD", "postgres")
        }

        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.r2dbc.url") { "r2dbc:postgresql://${postgres.host}:${postgres.getMappedPort(5432)}/movie_recommendation_test" }
            registry.add("spring.r2dbc.username") { "postgres" }
            registry.add("spring.r2dbc.password") { "postgres" }
            registry.add("spring.datasource.url") { "jdbc:postgresql://${postgres.host}:${postgres.getMappedPort(5432)}/movie_recommendation_test" }
            registry.add("spring.datasource.username") { "postgres" }
            registry.add("spring.datasource.password") { "postgres" }
            registry.add("app.kafka.enabled") { "false" }
        }
    }
}
