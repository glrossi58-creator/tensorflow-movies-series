package com.gilrossi.movie_recommendation.discovery

import com.gilrossi.movie_recommendation.application.port.out.TmdbClient
import com.gilrossi.movie_recommendation.exception.TmdbException
import com.gilrossi.movie_recommendation.model.RatingTargetType
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Service
class DiscoveryService(private val local: LocalDiscoveryRepository, private val tmdb: TmdbClient) {
    private val cache = ConcurrentHashMap<String, Pair<Instant, List<DiscoveryItem>>>()
    suspend fun search(query: String, type: DiscoveryType? = null): DiscoveryResponse {
        val q = query.trim()
        require(q.length in 2..150) { "Digite entre 2 e 150 caracteres para buscar." }
        val catalog = local.search(q, type)
        if (catalog.isNotEmpty() || type == DiscoveryType.GENRE) return DiscoveryResponse(catalog)
        return try {
            val key = "$type:${q.lowercase()}"
            val cached = cache[key]?.takeIf { it.first.isAfter(Instant.now()) }?.second
            val external = cached ?: tmdb.searchUniversal(q, type).results.filterNot { it.adult }.mapNotNull { item ->
                val kind = type ?: when (item.mediaType) { "movie" -> DiscoveryType.MOVIE; "tv" -> DiscoveryType.SERIES; "person" -> DiscoveryType.PERSON; else -> null }
                kind?.let { item.toDiscovery(it) }
            }.also {
                if (cache.size >= 200) cache.clear()
                cache[key] = Instant.now().plusSeconds(120) to it
            }
            DiscoveryResponse(external)
        } catch (e: TmdbException) { DiscoveryResponse(catalog, e.message) }
    }

    suspend fun importPerson(tmdbId: Int): DiscoveryItem {
        require(tmdbId > 0)
        val details = tmdb.getPersonDetails(tmdbId)
        val credits = tmdb.getPersonCredits(tmdbId)
        val roles = rolesFromCredits(credits).toMutableSet()
        // Creator is confirmed against created_by on actual series. Writer is never a creator shortcut.
        credits.crew.filter { it.mediaType == "tv" && it.job == "Creator" }.distinctBy { it.id }.forEach { credit ->
            if (tmdb.getSeriesDetails(credit.id).createdBy.any { it.id == tmdbId }) roles += RatingTargetType.CREATOR
        }
        return local.importPerson(details, roles)
    }

    fun rolesFromCredits(credits: TmdbPersonCredits): Set<RatingTargetType> = buildSet {
        if (credits.cast.isNotEmpty()) add(RatingTargetType.ACTOR)
        if (credits.crew.any { it.job == "Director" }) add(RatingTargetType.DIRECTOR)
    }
}

fun TmdbDiscoveryItem.toDiscovery(type: DiscoveryType) = DiscoveryItem(
    tmdbId = id, type = type, title = title ?: name.orEmpty(), source = "TMDB", posterPath = posterPath ?: profilePath,
    releaseDate = releaseDate ?: firstAirDate, overview = overview,
    roles = if (type == DiscoveryType.PERSON && department == "Acting") setOf(RatingTargetType.ACTOR) else emptySet()
)
