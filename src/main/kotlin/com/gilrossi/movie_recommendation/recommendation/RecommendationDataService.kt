package com.gilrossi.movie_recommendation.recommendation

import com.gilrossi.movie_recommendation.repository.ContentActorRepository
import com.gilrossi.movie_recommendation.repository.ContentDirectorRepository
import com.gilrossi.movie_recommendation.repository.ContentGenreRepository
import com.gilrossi.movie_recommendation.repository.ContentRepository
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Component

@Component
class RecommendationDataService(
    private val contentRepository: ContentRepository,
    private val genreRepository: ContentGenreRepository,
    private val actorRepository: ContentActorRepository,
    private val directorRepository: ContentDirectorRepository
) {
    suspend fun allSignals(): List<ContentSignals> = contentRepository.findAll().toList().map { content ->
        val id = requireNotNull(content.id)
        ContentSignals(
            content,
            genreRepository.findAllByContentId(id).toList().map { it.genreId }.toSet(),
            actorRepository.findAllByContentId(id).toList().map { it.personId }.toSet(),
            directorRepository.findAllByContentId(id).toList().map { it.personId }.toSet()
        )
    }
}
