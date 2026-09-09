package com.gilrossi.movie_recommendation.repository

import com.gilrossi.movie_recommendation.model.Rating
import com.gilrossi.movie_recommendation.model.RatingTargetType
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

@Repository
class RatingWriteRepository(private val db: DatabaseClient) {
    /** PostgreSQL resolves concurrent first writes atomically, without aborting the enclosing transaction. */
    suspend fun upsert(rating: Rating): Rating {
        val column = when {
            rating.targetType.isContent -> "content_id"
            rating.targetType.isPerson -> "person_id"
            else -> "genre_id"
        }
        return db.sql("""INSERT INTO rating(user_id,target_type,value,$column,created_at,updated_at)
            VALUES (:user,:type,:value,:target,:created,:updated)
            ON CONFLICT (user_id,target_type,$column) WHERE $column IS NOT NULL
            DO UPDATE SET value=EXCLUDED.value,updated_at=EXCLUDED.updated_at RETURNING *""")
            .bind("user",rating.userId).bind("type",rating.targetType.name).bind("value",rating.value)
            .bind("target",rating.targetId()).bind("created",rating.createdAt).bind("updated",rating.updatedAt)
            .map { row,_ -> Rating(
                row.get("id",Long::class.javaObjectType), row.get("user_id",Long::class.javaObjectType)!!,
                RatingTargetType.valueOf(row.get("target_type",String::class.java)!!), (row.get("value") as Number).toInt(),
                row.get("content_id",Long::class.javaObjectType), row.get("person_id",Long::class.javaObjectType), row.get("genre_id",Long::class.javaObjectType),
                row.get("created_at",OffsetDateTime::class.java)!!.toInstant(), row.get("updated_at",OffsetDateTime::class.java)!!.toInstant()
            ) }.one().awaitSingle()
    }
}
