package com.gilrossi.movie_recommendation.repository

import com.gilrossi.movie_recommendation.application.port.out.CatalogImportPort
import com.gilrossi.movie_recommendation.application.usecase.RatingUseCase
import com.gilrossi.movie_recommendation.application.usecase.RecommendationUseCase
import com.gilrossi.movie_recommendation.discovery.*
import com.gilrossi.movie_recommendation.dto.request.RatingRequest
import com.gilrossi.movie_recommendation.evaluation.*
import com.gilrossi.movie_recommendation.exception.ConflictException
import com.gilrossi.movie_recommendation.model.*
import com.gilrossi.movie_recommendation.recommendation.ModelLifecycleService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.Instant
import java.util.UUID
import kotlin.test.*

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class ProductIntegrationTest @Autowired constructor(
    private val users: AppUserRepository, private val ratings: RatingRepository, private val contents: ContentRepository,
    private val rate: RatingUseCase, private val imports: CatalogImportPort, private val evaluations: EvaluationService,
    private val models: ModelLifecycleService, private val discovery: DiscoveryService, private val local: LocalDiscoveryRepository,
    private val recommendations: RecommendationUseCase, private val db: DatabaseClient
) {
    private suspend fun user(): Long {
        val now = Instant.now()
        return users.save(AppUser(null, "Test profile", "${UUID.randomUUID()}@example.test", now, now)).id!!
    }
    private suspend fun content(type: ContentType = ContentType.MOVIE): Content = contents.save(Content(null, "Test ${UUID.randomUUID()}", null, type, null, null, null))
    private suspend fun direct(user: Long, amount: Int): List<Content> = (0 until amount).map { i ->
        val c = content(if (i % 2 == 0) ContentType.MOVIE else ContentType.SERIES)
        rate.rate(user, RatingRequest(RatingTargetType.valueOf(c.type.name), c.id!!, i % 5 + 1))
        c
    }

    @ParameterizedTest @EnumSource(RatingTargetType::class)
    fun `all six rating types normalize and isolate profiles`(type: RatingTargetType) = runBlocking {
        val gil = user(); val aliny = user()
        val tmdb = (System.nanoTime() % 1000000000).toInt()
        val saved = imports.persist(Content(null, "Roles $tmdb", tmdb, ContentType.MOVIE, null, null, null),
            listOf(Genre(null, tmdb, "Genre $tmdb")), listOf(Person(null, tmdb, "Person $tmdb")), listOf(Person(null, tmdb, "Person $tmdb")))
        imports.persist(Content(null, "Series $tmdb", tmdb, ContentType.SERIES, null, null, null), emptyList(), emptyList(), listOf(Person(null, tmdb, "Person $tmdb")))
        val view = evaluations.content(gil, saved.id!!)
        val id = when(type) {
            RatingTargetType.MOVIE -> saved.id
            RatingTargetType.SERIES -> contents.findByTmdbIdAndType(tmdb, ContentType.SERIES)!!.id!!
            RatingTargetType.GENRE -> view.genres.single().id
            else -> view.actors.single().id
        }
        val first = rate.rate(gil, RatingRequest(type, id, 5))
        val second = rate.rate(aliny, RatingRequest(type, id, 1))
        assertEquals(1.0, first.normalizedValue)
        assertEquals(0.0, second.normalizedValue)
        assertEquals(5, rate.list(gil).single().value)
        assertEquals(1, rate.list(aliny).single().value)
        assertEquals(if (type.isContent) 1 else 0, models.status(gil).directRatingCount)
        assertEquals(0, models.status(gil).modelVersion)
    }

    @Test fun `batch view null ratings autosave updates and atomic rollback`() = runBlocking {
        val user = user()
        val tmdb = (System.nanoTime() % 1000000000).toInt()
        val c = imports.persist(Content(null, "Evaluation $tmdb", tmdb, ContentType.MOVIE, null, null, null),
            listOf(Genre(null, tmdb, "Genre")), (0..9).map { Person(null, tmdb + it, "Actor $it") }, listOf(Person(null, tmdb+11, "Director")))
        val initial = evaluations.content(user, c.id!!)
        assertNull(initial.contentRating)
        assertEquals(10, initial.actors.size)
        assertTrue(initial.actors.all { it.rating == null })
        val updated = evaluations.update(user, c.id, EvaluationUpdate(contentRating=4, actors=listOf(EntityRating(initial.actors.first().id, 2)), genres=listOf(EntityRating(initial.genres.first().id, 5))))
        assertEquals(0.75, updated.contentRating!!.normalizedValue)
        assertEquals(0.25, updated.actors.first().rating!!.normalizedValue)
        assertFailsWith<IllegalArgumentException> { evaluations.update(user,c.id,EvaluationUpdate(contentRating=1, genres=listOf(EntityRating(Long.MAX_VALUE, 2)))) }
        assertEquals(4, evaluations.content(user,c.id).contentRating!!.value)
        val again = evaluations.update(user,c.id,EvaluationUpdate(contentRating=5))
        assertEquals(updated.contentRating.id, again.contentRating!!.id)
        assertEquals(3, rate.list(user).size)
    }

    @Test fun `simultaneous first autosaves preserve a single rating`() = runBlocking {
        val user = user(); val c = content()
        coroutineScope {
            val first = async { rate.rate(user, RatingRequest(RatingTargetType.MOVIE,c.id!!,4)) }
            val second = async { rate.rate(user, RatingRequest(RatingTargetType.MOVIE,c.id!!,5)) }
            assertEquals(first.await().id, second.await().id)
        }
        assertEquals(1,rate.list(user).size)
        assertEquals(1,models.status(user).directRatingCount)
    }

    @Test fun `scale and mismatched content types are rejected without writes`() = runBlocking {
        val user = user(); val c = content()
        listOf(0,6).forEach { value -> assertFailsWith<IllegalArgumentException> { rate.rate(user,RatingRequest(RatingTargetType.MOVIE,c.id!!,value)) } }
        assertFailsWith<IllegalArgumentException> { rate.rate(user,RatingRequest(RatingTargetType.SERIES,c.id!!,5)) }
        assertTrue(rate.list(user).isEmpty())
    }

    @Test fun `local legacy content is searched without tmdb configuration`() = runBlocking {
        val c = content()
        val found = discovery.search(c.title, DiscoveryType.MOVIE)
        assertEquals(c.id, found.results.single().id)
        assertEquals("LOCAL", found.results.single().source)
        assertNull(found.warning)
    }

    @Test fun `person roles do not treat writer as creator`() = runBlocking {
        val credits = TmdbPersonCredits(crew=listOf(TmdbPersonCredit(10,"Writer","tv")))
        assertTrue(discovery.rolesFromCredits(credits).isEmpty())
        val credits2 = TmdbPersonCredits(cast=listOf(TmdbPersonCredit(10)),crew=listOf(TmdbPersonCredit(11,"Director","movie")))
        assertEquals(setOf(RatingTargetType.ACTOR, RatingTargetType.DIRECTOR), discovery.rolesFromCredits(credits2))
    }

    @Test fun `minimum eight recommended twenty five real train dirty and ten new auto retrain`() = runBlocking {
        val gil = user(); val aliny = user()
        direct(gil,7)
        assertEquals("COLLECTING_DATA",models.status(gil).status)
        assertFalse(models.status(gil).canTrain)
        assertFailsWith<IllegalArgumentException> { models.train(gil) }
        direct(gil,1)
        assertEquals("READY",models.status(gil).status)
        assertEquals(8,models.status(gil).minimumDirectRatings)
        assertEquals(25,models.status(gil).recommendedDirectRatings)
        assertTrue(models.status(gil).canTrain)
        val trained = models.train(gil)
        assertTrue(trained.trainingLoss.isFinite() && trained.validationLoss.isFinite())
        assertTrue(trained.trainingSamples > 0 && trained.validationSamples > 0)
        assertEquals("TRAINED",models.status(gil).status)
        assertNotNull(models.trainedModel(gil))
        assertEquals(8,models.status(gil).ratingsUsedInLastTraining)
        assertEquals(0,models.status(gil).newRatingsSinceLastTraining)
        val first = rate.list(gil).first()
        rate.rate(gil,RatingRequest(first.targetType,first.targetId,if(first.value==5) 1 else 5))
        assertEquals("DIRTY",models.status(gil).status)
        assertEquals(0,models.status(gil).newRatingsSinceLastTraining)
        direct(gil,9)
        models.settings(gil,true)
        models.autoTrain()
        assertEquals(1,models.status(gil).modelVersion)
        direct(gil,1)
        models.autoTrain()
        assertEquals(2,models.status(gil).modelVersion)
        assertEquals("TRAINED",models.status(gil).status)
        assertEquals(0,models.status(gil).newRatingsSinceLastTraining)
        assertEquals(0,models.status(aliny).modelVersion)
        direct(gil,7)
        assertEquals(25,models.status(gil).directRatingCount)
        assertEquals("DIRTY",models.status(gil).status)
    }

    @Test fun `training lock rejects duplicate without disturbing active state`() = runBlocking {
        val user = user(); direct(user,8)
        db.sql("UPDATE user_model_state SET status='TRAINING',training_started_at=now() WHERE user_id=:id").bind("id",user).fetch().rowsUpdated().awaitSingle()
        assertFailsWith<ConflictException> { models.train(user) }
        assertEquals("TRAINING", models.status(user).status)
        assertFalse(models.status(user).canTrain)
    }

    @Test fun `missing saved weights mark an error and permit manual recovery`() = runBlocking {
        val user = user(); direct(user,8)
        models.train(user)
        db.sql("UPDATE user_model_state SET model_path=:path WHERE user_id=:id")
            .bind("path", "./build/nonexistent-${UUID.randomUUID()}.json").bind("id", user).fetch().rowsUpdated().awaitSingle()
        assertNull(models.trainedModel(user))
        assertEquals("ERROR", models.status(user).status)
        assertTrue(models.status(user).canTrain)
        models.train(user)
        assertEquals("TRAINED", models.status(user).status)
    }

    @Test fun `individual cold start and joint recommendations exclude watched and do not train`() = runBlocking {
        val gil = user(); val aliny = user()
        val watched = direct(gil,8)
        val alinyWatched = direct(aliny,1)
        repeat(15) { content() }
        val individual = recommendations.recommend(gil)
        assertTrue(individual.isNotEmpty())
        assertTrue(individual.none { it.content.id in watched.map { c -> c.id } })
        assertTrue(individual.all { it.score in 0.0..1.0 && it.strategy == "BASELINE" })
        assertEquals(0, models.status(gil).modelVersion)
        val cold = recommendations.recommend(user())
        assertTrue(cold.all { it.strategy == "BASELINE_COLD_START" })
        val joint = recommendations.recommendTogether(listOf(gil,aliny))
        assertTrue(joint.isNotEmpty())
        assertTrue(joint.none { it.content.id in (watched+alinyWatched).map { c -> c.id } })
        assertTrue(joint.all { it.individualScores.keys == setOf(gil,aliny) && it.score in 0.0..1.0 })
    }

    @Test fun `genre preferences refresh vector and neutral reset clears stale vector`() = runBlocking {
        val user = user(); val tmdb=(System.nanoTime()%1000000000).toInt()
        val c=imports.persist(Content(null,"Embedding $tmdb",tmdb,ContentType.MOVIE,null,null,null),listOf(Genre(null,tmdb,"Vector genre")),emptyList(),emptyList())
        val genre=evaluations.content(user,c.id!!).genres.single().id
        rate.rate(user,RatingRequest(RatingTargetType.GENRE,genre,5))
        val dimensions=db.sql("SELECT vector_dims(embedding) AS dimensions FROM app_user WHERE id=:id").bind("id",user)
            .map { row,_ -> (row.get("dimensions") as Number).toInt() }.one().awaitSingle()
        assertEquals(8,dimensions)
        rate.rate(user,RatingRequest(RatingTargetType.GENRE,genre,3))
        val cleared=db.sql("SELECT embedding IS NULL AS cleared FROM app_user WHERE id=:id").bind("id",user)
            .map { row,_ -> row.get("cleared",Boolean::class.javaObjectType)!! }.one().awaitSingle()
        assertTrue(cleared)
        assertEquals(0,models.status(user).directRatingCount)
    }

    companion object {
        @Container @JvmStatic val postgres=GenericContainer<Nothing>(DockerImageName.parse("pgvector/pgvector:pg16")).apply {
            withExposedPorts(5432); withEnv("POSTGRES_DB","product_test");withEnv("POSTGRES_USER","postgres");withEnv("POSTGRES_PASSWORD","postgres")
        }
        @DynamicPropertySource @JvmStatic fun properties(r: DynamicPropertyRegistry) {
            r.add("spring.r2dbc.url") { "r2dbc:postgresql://${postgres.host}:${postgres.getMappedPort(5432)}/product_test" }
            r.add("spring.r2dbc.username") { "postgres" }; r.add("spring.r2dbc.password") { "postgres" }
            listOf("spring.datasource.url","spring.flyway.url").forEach { key -> r.add(key) { "jdbc:postgresql://${postgres.host}:${postgres.getMappedPort(5432)}/product_test" } }
            r.add("spring.flyway.user") { "postgres" }; r.add("spring.flyway.password") { "postgres" }
            r.add("app.kafka.enabled") { "false" };r.add("tmdb.read-access-token") { "" }
            r.add("recommendation.ml.model-directory") { "./build/product-models" }
            r.add("recommendation.ml.auto-train-enabled") { "false" };r.add("recommendation.ml.auto-check-ms") { "3600000" }
        }
    }
}
