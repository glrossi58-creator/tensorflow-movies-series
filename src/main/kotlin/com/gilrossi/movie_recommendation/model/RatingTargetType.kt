package com.gilrossi.movie_recommendation.model

enum class RatingTargetType {
    MOVIE,
    SERIES,
    ACTOR,
    DIRECTOR,
    CREATOR,
    GENRE;

    val isContent: Boolean get() = this == MOVIE || this == SERIES
    val isPerson: Boolean get() = this == ACTOR || this == DIRECTOR || this == CREATOR
}
