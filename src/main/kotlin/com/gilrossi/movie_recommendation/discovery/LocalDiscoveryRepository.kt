package com.gilrossi.movie_recommendation.discovery

import com.gilrossi.movie_recommendation.exception.PersonNotFoundException
import com.gilrossi.movie_recommendation.model.RatingTargetType
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class LocalDiscoveryRepository(private val db: DatabaseClient) {
    suspend fun search(query: String, type: DiscoveryType?): List<DiscoveryItem> {
        val pattern = "%${query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")}%"
        val results = mutableListOf<DiscoveryItem>()
        if (type == null || type == DiscoveryType.MOVIE || type == DiscoveryType.SERIES) {
            results += db.sql("SELECT id, tmdb_id, type, title, poster_path, release_date::text, overview FROM content WHERE title ILIKE :query AND (:type = '' OR type = :type) ORDER BY title LIMIT 40")
                .bind("query", pattern).bind("type", type?.name ?: "").map { row, _ -> DiscoveryItem(
                    id = row.get("id", java.lang.Long::class.java)?.toLong(), tmdbId = row.get("tmdb_id", Integer::class.java)?.toInt(),
                    type = DiscoveryType.valueOf(row.get("type", String::class.java)!!), title = row.get("title", String::class.java)!!, source = "LOCAL",
                    posterPath = row.get("poster_path", String::class.java), releaseDate = row.get("release_date", String::class.java), overview = row.get("overview", String::class.java)
                ) }.all().collectList().awaitSingle()
        }
        if (type == null || type == DiscoveryType.PERSON) {
            results += db.sql("$PERSON_SELECT WHERE p.name ILIKE :query GROUP BY p.id ORDER BY p.name LIMIT 30")
                .bind("query", pattern).map { row, _ -> mapPerson(row) }.all().collectList().awaitSingle()
        }
        if (type == null || type == DiscoveryType.GENRE) {
            results += db.sql("SELECT id, tmdb_id, name FROM genre WHERE name ILIKE :query ORDER BY name LIMIT 50")
                .bind("query", pattern).map { row, _ -> DiscoveryItem(
                    id = row.get("id", java.lang.Long::class.java)!!.toLong(), tmdbId = row.get("tmdb_id", Integer::class.java)!!.toInt(),
                    type = DiscoveryType.GENRE, title = row.get("name", String::class.java)!!, source = "LOCAL"
                ) }.all().collectList().awaitSingle()
        }
        return results
    }

    suspend fun person(id: Long): DiscoveryItem = db.sql("$PERSON_SELECT WHERE p.id = :id GROUP BY p.id")
        .bind("id", id).map { row, _ -> mapPerson(row) }.one().awaitSingleOrNull() ?: throw PersonNotFoundException()

    suspend fun roles(id: Long): Set<RatingTargetType> = person(id).roles

    @Transactional
    suspend fun importPerson(person: TmdbPersonDetails, roles: Set<RatingTargetType>): DiscoveryItem {
        val id = db.sql("""INSERT INTO person(tmdb_id,name,profile_path,biography) VALUES (:tmdb,:name,:photo,:bio)
            ON CONFLICT (tmdb_id) DO UPDATE SET name=EXCLUDED.name, profile_path=EXCLUDED.profile_path, biography=EXCLUDED.biography RETURNING id""")
            .bind("tmdb", person.id).bind("name", person.name)
            .let { if (person.profilePath == null) it.bindNull("photo", String::class.java) else it.bind("photo", person.profilePath) }
            .let { if (person.biography == null) it.bindNull("bio", String::class.java) else it.bind("bio", person.biography) }
            .map { row, _ -> row.get("id", java.lang.Long::class.java)!!.toLong() }.one().awaitSingle()
        roles.forEach { role ->
            db.sql("INSERT INTO person_role(person_id,role,source) VALUES (:id,:role,'TMDB_CREDITS') ON CONFLICT DO NOTHING")
                .bind("id", id).bind("role", role.name).fetch().rowsUpdated().awaitSingle()
        }
        return person(id)
    }

    private fun mapPerson(row: io.r2dbc.spi.Row) = DiscoveryItem(
        id = row.get("id", java.lang.Long::class.java)!!.toLong(), tmdbId = row.get("tmdb_id", Integer::class.java)!!.toInt(),
        type = DiscoveryType.PERSON, title = row.get("name", String::class.java)!!, source = "LOCAL",
        posterPath = row.get("profile_path", String::class.java), overview = row.get("biography", String::class.java),
        roles = row.get("roles", String::class.java).orEmpty().split(',').filter(String::isNotEmpty).map(RatingTargetType::valueOf).toSet()
    )
    companion object {
        private const val PERSON_SELECT = """SELECT p.*, string_agg(DISTINCT r.role, ',') AS roles FROM person p LEFT JOIN (
            SELECT person_id, role FROM person_role UNION SELECT person_id, 'ACTOR' FROM content_actor
            UNION SELECT cd.person_id, CASE WHEN c.type='MOVIE' THEN 'DIRECTOR' ELSE 'CREATOR' END FROM content_director cd JOIN content c ON c.id=cd.content_id
        ) r ON r.person_id=p.id"""
    }
}
