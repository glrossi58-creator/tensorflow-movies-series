package com.gilrossi.movie_recommendation.mapper

import com.gilrossi.movie_recommendation.dto.response.TmdbMovieDetailsResponse
import com.gilrossi.movie_recommendation.dto.response.TmdbSeriesDetailsResponse
import com.gilrossi.movie_recommendation.model.Content
import com.gilrossi.movie_recommendation.model.ContentType
import java.time.LocalDate

object TmdbContentMapper {

    fun toContent(
        response: TmdbMovieDetailsResponse
    ): Content {

        return Content(
            id = null,
            tmdbId = response.id,
            type = ContentType.MOVIE,
            title = response.title,
            overview = response.overview,
            releaseDate = response.releaseDate
                ?.takeIf { it.isNotBlank() }
                ?.let { LocalDate.parse(it) },
            posterPath = response.posterPath
        )
    }

    fun toContent(response: TmdbSeriesDetailsResponse): Content = Content(
        id = null,
        tmdbId = response.id,
        type = ContentType.SERIES,
        title = response.name,
        overview = response.overview,
        releaseDate = response.firstAirDate
            ?.takeIf { it.isNotBlank() }
            ?.let(LocalDate::parse),
        posterPath = response.posterPath
    )
}
