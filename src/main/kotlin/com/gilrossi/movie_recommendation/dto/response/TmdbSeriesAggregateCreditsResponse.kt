package com.gilrossi.movie_recommendation.dto.response

import com.gilrossi.movie_recommendation.dto.TmdbAggregateCastMember

data class TmdbSeriesAggregateCreditsResponse(
    val id: Int,
    val cast: List<TmdbAggregateCastMember>
)