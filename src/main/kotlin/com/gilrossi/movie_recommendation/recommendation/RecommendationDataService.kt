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
    suspend fun allSignals(): List<ContentSignals> {
        val genres = genreRepository.findAll().toList().groupBy { it.contentId }
        val actors = actorRepository.findAll().toList().groupBy { it.contentId }
        val directors = directorRepository.findAll().toList().groupBy { it.contentId }
        return contentRepository.findAll().toList().map { content ->
            ContentSignals(content, genres[content.id].orEmpty().map { it.genreId }.toSet(),
                actors[content.id].orEmpty().map { it.personId }.toSet(), directors[content.id].orEmpty().map { it.personId }.toSet())
        }
    }
}
