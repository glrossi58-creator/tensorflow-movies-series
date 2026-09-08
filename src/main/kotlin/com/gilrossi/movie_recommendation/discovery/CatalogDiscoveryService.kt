package com.gilrossi.movie_recommendation.discovery

import com.gilrossi.movie_recommendation.application.port.out.TmdbClient
import com.gilrossi.movie_recommendation.application.usecase.ImportMovieUseCase
import com.gilrossi.movie_recommendation.application.usecase.ImportSeriesUseCase
import com.gilrossi.movie_recommendation.exception.TmdbException
import com.gilrossi.movie_recommendation.exception.UserNotFoundException
import com.gilrossi.movie_recommendation.model.Content
import com.gilrossi.movie_recommendation.model.ContentType
import com.gilrossi.movie_recommendation.recommendation.RecommendationDataService
import com.gilrossi.movie_recommendation.repository.AppUserRepository
import com.gilrossi.movie_recommendation.repository.ContentRepository
import com.gilrossi.movie_recommendation.repository.RatingRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class CatalogDiscoveryService(
    private val tmdb: TmdbClient, private val contents: ContentRepository, private val ratings: RatingRepository,
    private val users: AppUserRepository, private val data: RecommendationDataService,
    private val movies: ImportMovieUseCase, private val series: ImportSeriesUseCase
) {
    private val lock = Mutex()
    private var popularCache: Pair<Instant, List<TmdbDiscoveryItem>>? = null
    private var lastFailure: Pair<Instant, String>? = null

    suspend fun import(tmdbId: Int, type: DiscoveryType): Content {
        require(tmdbId > 0 && type in setOf(DiscoveryType.MOVIE, DiscoveryType.SERIES))
        val contentType = ContentType.valueOf(type.name)
        return contents.findByTmdbIdAndType(tmdbId, contentType)
            ?: if (type == DiscoveryType.MOVIE) movies.execute(tmdbId) else series.execute(tmdbId)
    }

    private suspend fun popular(): List<TmdbDiscoveryItem> = lock.withLock {
        popularCache?.takeIf { it.first.isAfter(Instant.now()) }?.let { return@withLock it.second }
        lastFailure?.takeIf { it.first.isAfter(Instant.now()) }?.let { throw com.gilrossi.movie_recommendation.exception.TmdbUnavailableException(it.second) }
        try {
            val results = coroutineScope {
                listOf(DiscoveryType.MOVIE, DiscoveryType.SERIES).map { type -> async {
                    tmdb.popular(type, 1).results.filterNot { it.adult }.map { it.copy(mediaType = if (type == DiscoveryType.MOVIE) "movie" else "tv") }
                } }.awaitAll().flatten()
            }
            val diversified = mutableListOf<TmdbDiscoveryItem>()
            val remaining = results.toMutableList()
            while (remaining.isNotEmpty()) {
                val last = diversified.lastOrNull()
                val next = remaining.maxBy { item ->
                    (if (last?.mediaType != item.mediaType) 10 else 0) + item.genreIds.count { it !in last?.genreIds.orEmpty() }
                }
                diversified += next
                remaining.remove(next)
            }
            popularCache = Instant.now().plusSeconds(900) to diversified
            diversified
        } catch (e: TmdbException) {
            lastFailure = Instant.now().plusSeconds(30) to e.message.orEmpty()
            throw e
        }
    }

    suspend fun quick(userId: Long, skip: Set<String>, limit: Int = 40): DiscoveryResponse {
        require(limit in 1..100 && skip.size <= 500) { "Limite de busca inválido." }
        if (!users.existsById(userId)) throw UserNotFoundException()
        val rated = ratings.findAllByUserId(userId).toList().mapNotNull { it.contentId }.toSet()
        val signals = data.allSignals()
        val catalog = signals.map { it.content }
        val byTmdb = catalog.filter { it.tmdbId != null }.associateBy { it.tmdbId to it.type }
        val local = signals.filter { it.content.id !in rated && "LOCAL:${it.content.id}" !in skip }.toMutableList()
        val ordered = mutableListOf<DiscoveryItem>()
        var previous: com.gilrossi.movie_recommendation.recommendation.ContentSignals? = null
        while (local.isNotEmpty() && ordered.size < limit) {
            val selected = local.maxBy { candidate ->
                (if (candidate.content.type != previous?.content?.type) 10 else 0) + candidate.genreIds.count { it !in previous?.genreIds.orEmpty() }
            }
            val c = selected.content
            ordered += DiscoveryItem(c.id, c.tmdbId, DiscoveryType.valueOf(c.type.name), c.title, "LOCAL", c.posterPath, c.releaseDate?.toString(), c.overview)
            previous = selected
            local.remove(selected)
        }
        var warning: String? = null
        if (ordered.size < limit) try {
            popular().forEach { item ->
                val type = if (item.mediaType == "movie") DiscoveryType.MOVIE else DiscoveryType.SERIES
                if ((item.id to ContentType.valueOf(type.name)) !in byTmdb && "TMDB:$type:${item.id}" !in skip)
                    ordered += item.toDiscovery(type)
            }
        } catch (e: TmdbException) { warning = e.message }
        return DiscoveryResponse(ordered.take(limit), warning)
    }

    /** Populate real unseen candidates only when the local catalog is exhausted. No ratings are fabricated. */
    suspend fun ensureCandidates(userIds: List<Long>) {
        val rated = ratings.findAllByUserIdIn(userIds).toList().mapNotNull { it.contentId }.toSet()
        val catalog = contents.findAll().toList()
        if (catalog.count { it.id !in rated } >= 12) return
        val keys = catalog.map { it.tmdbId to it.type }.toSet()
        try {
            val candidates = popular().filter { item ->
                (item.id to if (item.mediaType == "movie") ContentType.MOVIE else ContentType.SERIES) !in keys
            }.take(12)
            candidates.chunked(3).forEach { batch -> coroutineScope {
                batch.map { item -> async { import(item.id, if (item.mediaType == "movie") DiscoveryType.MOVIE else DiscoveryType.SERIES) } }.awaitAll()
            } }
        } catch (_: TmdbException) { /* The existing local catalog remains usable while TMDB is unavailable. */ }
    }
}
