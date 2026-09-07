package com.gilrossi.movie_recommendation.application.usecase

import com.gilrossi.movie_recommendation.application.port.out.CatalogImportPort
import com.gilrossi.movie_recommendation.application.port.out.TmdbClient
import com.gilrossi.movie_recommendation.mapper.TmdbContentMapper
import com.gilrossi.movie_recommendation.mapper.TmdbGenreMapper
import com.gilrossi.movie_recommendation.mapper.TmdbPersonMapper
import com.gilrossi.movie_recommendation.model.Content
import org.springframework.stereotype.Service

@Service
class ImportMovieUseCase(
    private val tmdbClient: TmdbClient,
    private val catalogImportPort: CatalogImportPort
) {
    suspend fun execute(tmdbId: Int): Content {
        val details = tmdbClient.getMovieDetails(tmdbId)
        val credits = tmdbClient.getMovieCredits(tmdbId)

        return catalogImportPort.persist(
            content = TmdbContentMapper.toContent(details),
            genres = details.genres.map(TmdbGenreMapper::toGenre),
            actors = credits.cast.sortedBy { it.order }.take(MAX_ACTORS).map(TmdbPersonMapper::toPerson),
            directorsOrCreators = credits.crew
                .filter { it.job == DIRECTOR_JOB }
                .mapNotNull(TmdbPersonMapper::toPerson)
        )
    }

    private companion object {
        const val MAX_ACTORS = 10
        const val DIRECTOR_JOB = "Director"
    }
}
