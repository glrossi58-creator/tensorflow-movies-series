package com.gilrossi.movie_recommendation.infrastructure.persistence

import com.gilrossi.movie_recommendation.application.port.out.CatalogImportPort
import com.gilrossi.movie_recommendation.model.Content
import com.gilrossi.movie_recommendation.model.ContentType
import com.gilrossi.movie_recommendation.model.Genre
import com.gilrossi.movie_recommendation.model.Person
import com.gilrossi.movie_recommendation.recommendation.ContentEmbeddingGenerator
import com.gilrossi.movie_recommendation.recommendation.ContentSignals
import com.gilrossi.movie_recommendation.recommendation.PgVectorService
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Component
class R2dbcCatalogImportAdapter(
    private val databaseClient: DatabaseClient,
    private val pgVectorService: PgVectorService,
    private val embeddingGenerator: ContentEmbeddingGenerator
) : CatalogImportPort {

    @Transactional
    override suspend fun persist(
        content: Content,
        genres: List<Genre>,
        actors: List<Person>,
        directorsOrCreators: List<Person>
    ): Content {
        requireNotNull(content.tmdbId) { "Importação TMDB exige tmdbId." }
        val savedContent = upsertContent(content)
        val contentId = requireNotNull(savedContent.id)

        val genreIds = genres.distinctBy(Genre::tmdbId).map { upsertGenre(it) }
        val actorIds = actors.distinctBy(Person::tmdbId).map { upsertPerson(it) }
        val directorIds = directorsOrCreators.distinctBy(Person::tmdbId).map { upsertPerson(it) }

        replaceRelations("content_genre", "genre_id", contentId, genreIds)
        replaceRelations("content_actor", "person_id", contentId, actorIds)
        replaceRelations("content_director", "person_id", contentId, directorIds)
        pgVectorService.saveContentEmbedding(
            contentId,
            embeddingGenerator.generate(
                ContentSignals(savedContent, genreIds.toSet(), actorIds.toSet(), directorIds.toSet())
            )
        )
        return savedContent
    }

    private suspend fun upsertContent(content: Content): Content = databaseClient.sql(
        """
        INSERT INTO content(title, tmdb_id, type, overview, release_date, poster_path)
        VALUES (:title, :tmdbId, :type, :overview, :releaseDate, :posterPath)
        ON CONFLICT (tmdb_id, type) DO UPDATE SET
            title = EXCLUDED.title,
            overview = EXCLUDED.overview,
            release_date = EXCLUDED.release_date,
            poster_path = EXCLUDED.poster_path
        RETURNING id, title, tmdb_id, type, overview, release_date, poster_path
        """.trimIndent()
    )
        .bind("title", content.title)
        .bind("tmdbId", requireNotNull(content.tmdbId))
        .bind("type", content.type.name)
        .bindNullable("overview", content.overview, String::class.java)
        .bindNullable("releaseDate", content.releaseDate, LocalDate::class.java)
        .bindNullable("posterPath", content.posterPath, String::class.java)
        .map { row, _ ->
            Content(
                id = row.get("id", Long::class.javaObjectType),
                title = requireNotNull(row.get("title", String::class.java)),
                tmdbId = row.get("tmdb_id", Int::class.javaObjectType),
                type = ContentType.valueOf(requireNotNull(row.get("type", String::class.java))),
                overview = row.get("overview", String::class.java),
                releaseDate = row.get("release_date", LocalDate::class.java),
                posterPath = row.get("poster_path", String::class.java)
            )
        }
        .one()
        .awaitSingle()

    private suspend fun upsertGenre(genre: Genre): Long = databaseClient.sql(
        """
        INSERT INTO genre(tmdb_id, name) VALUES (:tmdbId, :name)
        ON CONFLICT (tmdb_id) DO UPDATE SET name = EXCLUDED.name
        RETURNING id
        """.trimIndent()
    )
        .bind("tmdbId", genre.tmdbId)
        .bind("name", genre.name)
        .map { row, _ -> requireNotNull(row.get("id", Long::class.javaObjectType)) }
        .one().awaitSingle()

    private suspend fun upsertPerson(person: Person): Long = databaseClient.sql(
        """
        INSERT INTO person(tmdb_id, name) VALUES (:tmdbId, :name)
        ON CONFLICT (tmdb_id) DO UPDATE SET name = EXCLUDED.name
        RETURNING id
        """.trimIndent()
    )
        .bind("tmdbId", person.tmdbId)
        .bind("name", person.name)
        .map { row, _ -> requireNotNull(row.get("id", Long::class.javaObjectType)) }
        .one().awaitSingle()

    private suspend fun replaceRelations(
        table: String,
        relatedColumn: String,
        contentId: Long,
        relatedIds: List<Long>
    ) {
        databaseClient.sql("DELETE FROM $table WHERE content_id = :contentId")
            .bind("contentId", contentId)
            .fetch().rowsUpdated().awaitSingleOrNull()

        relatedIds.distinct().forEach { relatedId ->
            databaseClient.sql(
                "INSERT INTO $table(content_id, $relatedColumn) VALUES (:contentId, :relatedId) ON CONFLICT DO NOTHING"
            )
                .bind("contentId", contentId)
                .bind("relatedId", relatedId)
                .fetch().rowsUpdated().awaitSingleOrNull()
        }
    }

    private fun <T : Any> DatabaseClient.GenericExecuteSpec.bindNullable(
        name: String,
        value: T?,
        type: Class<T>
    ): DatabaseClient.GenericExecuteSpec = if (value == null) bindNull(name, type) else bind(name, value)
}
