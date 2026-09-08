package com.gilrossi.movie_recommendation.evaluation

import com.gilrossi.movie_recommendation.application.usecase.RatingUseCase
import com.gilrossi.movie_recommendation.discovery.LocalDiscoveryRepository
import com.gilrossi.movie_recommendation.discovery.DiscoveryItem
import com.gilrossi.movie_recommendation.dto.request.RatingRequest
import com.gilrossi.movie_recommendation.dto.response.RatingResponse
import com.gilrossi.movie_recommendation.exception.ContentNotFoundException
import com.gilrossi.movie_recommendation.exception.UserNotFoundException
import com.gilrossi.movie_recommendation.model.*
import com.gilrossi.movie_recommendation.repository.*
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class RatedEntity(val id: Long, val name: String, val targetType: RatingTargetType, val rating: RatingResponse?, val imagePath: String? = null)
data class ContentEvaluation(
    val content: Content, val contentRating: RatingResponse?, val genres: List<RatedEntity>, val actors: List<RatedEntity>,
    val directors: List<RatedEntity>, val creators: List<RatedEntity>
)
data class PersonEvaluation(val person: DiscoveryItem, val roles: List<RatedEntity>)
data class EntityRating(@field:Positive val targetId: Long, @field:Min(1) @field:Max(5) val value: Int)
data class EvaluationUpdate(
    @field:Min(1) @field:Max(5) val contentRating: Int? = null,
    @field:Valid @field:Size(max = 50) val genres: List<EntityRating>? = null,
    @field:Valid @field:Size(max = 10) val actors: List<EntityRating>? = null,
    @field:Valid @field:Size(max = 30) val directors: List<EntityRating>? = null,
    @field:Valid @field:Size(max = 30) val creators: List<EntityRating>? = null
)
data class RatingView(val rating: RatingResponse, val title: String, val posterPath: String?)

@Service
class EvaluationService(
    private val users: AppUserRepository, private val contents: ContentRepository, private val genres: GenreRepository,
    private val ratings: RatingUseCase, private val people: LocalDiscoveryRepository, private val db: DatabaseClient
) {
    suspend fun content(userId: Long, contentId: Long): ContentEvaluation {
        val allRatings = ratings.list(userId).associateBy { it.targetType to it.targetId }
        val content = contents.findById(contentId) ?: throw ContentNotFoundException("Conteúdo não encontrado.")
        val genreList = db.sql("SELECT g.id,g.name FROM genre g JOIN content_genre cg ON cg.genre_id=g.id WHERE cg.content_id=:id ORDER BY g.name")
            .bind("id", contentId).map { row, _ ->
                val id = row.get("id", java.lang.Long::class.java)!!.toLong()
                RatedEntity(id, row.get("name", String::class.java)!!, RatingTargetType.GENRE, allRatings[RatingTargetType.GENRE to id])
            }.all().collectList().awaitSingle()
        val actors = relatedPeople(contentId, true, RatingTargetType.ACTOR, allRatings)
        val role = if (content.type == ContentType.MOVIE) RatingTargetType.DIRECTOR else RatingTargetType.CREATOR
        val leads = relatedPeople(contentId, false, role, allRatings)
        return ContentEvaluation(content, allRatings[RatingTargetType.valueOf(content.type.name) to contentId], genreList, actors,
            if (role == RatingTargetType.DIRECTOR) leads else emptyList(), if (role == RatingTargetType.CREATOR) leads else emptyList())
    }

    private suspend fun relatedPeople(contentId: Long, actor: Boolean, role: RatingTargetType, all: Map<Pair<RatingTargetType, Long>, RatingResponse>): List<RatedEntity> {
        val table = if (actor) "content_actor" else "content_director"
        val order = if (actor) "r.cast_order NULLS LAST, r.id LIMIT 10" else "r.id"
        return db.sql("SELECT p.id,p.name,p.profile_path FROM person p JOIN $table r ON r.person_id=p.id WHERE r.content_id=:id ORDER BY $order")
            .bind("id", contentId).map { row, _ ->
                val id = row.get("id", java.lang.Long::class.java)!!.toLong()
                RatedEntity(id, row.get("name", String::class.java)!!, role, all[role to id], row.get("profile_path", String::class.java))
            }.all().collectList().awaitSingle()
    }

    @Transactional
    suspend fun update(userId: Long, contentId: Long, request: EvaluationUpdate): ContentEvaluation {
        val view = content(userId, contentId)
        val commands = mutableListOf<RatingRequest>()
        request.contentRating?.let { commands += RatingRequest(RatingTargetType.valueOf(view.content.type.name), contentId, it) }
        fun collect(values: List<EntityRating>?, allowed: List<RatedEntity>, role: RatingTargetType) {
            require(values.orEmpty().map { it.targetId }.distinct().size == values.orEmpty().size) { "Há alvos repetidos na avaliação." }
            values.orEmpty().forEach { r ->
                require(allowed.any { it.id == r.targetId }) { "O alvo ${r.targetId} não pertence a este conteúdo no papel $role." }
                commands += RatingRequest(role, r.targetId, r.value)
            }
        }
        collect(request.genres, view.genres, RatingTargetType.GENRE)
        collect(request.actors, view.actors, RatingTargetType.ACTOR)
        collect(request.directors, view.directors, RatingTargetType.DIRECTOR)
        collect(request.creators, view.creators, RatingTargetType.CREATOR)
        commands.forEach { ratings.rate(userId, it) }
        return content(userId, contentId)
    }

    suspend fun person(userId: Long, personId: Long): PersonEvaluation {
        val all = ratings.list(userId).filter { it.targetId == personId }.associateBy { it.targetType }
        val person = people.person(personId)
        return PersonEvaluation(person, person.roles.map { RatedEntity(personId, person.title, it, all[it], person.posterPath) })
    }

    suspend fun genres(userId: Long): List<RatedEntity> {
        val all = ratings.list(userId).filter { it.targetType == RatingTargetType.GENRE }.associateBy { it.targetId }
        return genres.findAll().toList().sortedBy { it.name }.map { RatedEntity(it.id!!, it.name, RatingTargetType.GENRE, all[it.id]) }
    }

    suspend fun history(userId: Long): List<RatingView> {
        if (!users.existsById(userId)) throw UserNotFoundException()
        val names = db.sql("""SELECT r.id, COALESCE(c.title,p.name,g.name) AS title, COALESCE(c.poster_path,p.profile_path) AS poster
            FROM rating r LEFT JOIN content c ON c.id=r.content_id LEFT JOIN person p ON p.id=r.person_id LEFT JOIN genre g ON g.id=r.genre_id WHERE r.user_id=:id""")
            .bind("id", userId).map { row, _ -> row.get("id", java.lang.Long::class.java)!!.toLong() to
                (row.get("title", String::class.java).orEmpty() to row.get("poster", String::class.java)) }.all().collectList().awaitSingle().toMap()
        return ratings.list(userId).sortedByDescending { it.updatedAt }.map { RatingView(it, names[it.id]?.first.orEmpty(), names[it.id]?.second) }
    }
}
