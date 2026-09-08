package com.gilrossi.movie_recommendation.discovery

import com.fasterxml.jackson.annotation.JsonProperty
import com.gilrossi.movie_recommendation.model.RatingTargetType

enum class DiscoveryType { MOVIE, SERIES, PERSON, GENRE }
data class DiscoveryItem(
    val id: Long? = null,
    val tmdbId: Int? = null,
    val type: DiscoveryType,
    val title: String,
    val source: String,
    val posterPath: String? = null,
    val releaseDate: String? = null,
    val overview: String? = null,
    val roles: Set<RatingTargetType> = emptySet()
)
data class DiscoveryResponse(val results: List<DiscoveryItem>, val warning: String? = null)
data class TmdbDiscoveryResponse(val results: List<TmdbDiscoveryItem> = emptyList())
data class TmdbDiscoveryItem(
    val id: Int,
    val title: String? = null,
    val name: String? = null,
    val overview: String? = null,
    val adult: Boolean = false,
    @JsonProperty("media_type") val mediaType: String? = null,
    @JsonProperty("poster_path") val posterPath: String? = null,
    @JsonProperty("profile_path") val profilePath: String? = null,
    @JsonProperty("release_date") val releaseDate: String? = null,
    @JsonProperty("first_air_date") val firstAirDate: String? = null,
    @JsonProperty("known_for_department") val department: String? = null,
    @JsonProperty("genre_ids") val genreIds: List<Int> = emptyList()
)
data class TmdbPersonDetails(
    val id: Int, val name: String, val biography: String? = null,
    @JsonProperty("profile_path") val profilePath: String? = null
)
data class TmdbPersonCredits(val cast: List<TmdbPersonCredit> = emptyList(), val crew: List<TmdbPersonCredit> = emptyList())
data class TmdbPersonCredit(
    val id: Int,
    val job: String? = null,
    @JsonProperty("media_type") val mediaType: String? = null
)
