package com.gilrossi.movie_recommendation.dto.response

import com.gilrossi.movie_recommendation.dto.TmdbCastMember
import com.gilrossi.movie_recommendation.dto.TmdbCrewMember

data class TmdbCreditsResponse(
    val id: Int,              // id do FILME (o TMDB repete aqui)
    val cast: List<TmdbCastMember>,
    val crew: List<TmdbCrewMember>
)