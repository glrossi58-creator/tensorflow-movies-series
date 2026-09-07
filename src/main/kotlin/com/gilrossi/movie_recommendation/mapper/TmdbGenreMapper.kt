package com.gilrossi.movie_recommendation.mapper

import com.gilrossi.movie_recommendation.dto.TmdbGenre
import com.gilrossi.movie_recommendation.model.Genre

object TmdbGenreMapper {

    fun toGenre(
        tmdbGenre: TmdbGenre
    ): Genre {

        return Genre(
            id = null,
            tmdbId = tmdbGenre.id,
            name = tmdbGenre.name
        )
    }
}