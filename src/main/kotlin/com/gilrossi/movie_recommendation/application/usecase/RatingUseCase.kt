package com.gilrossi.movie_recommendation.application.usecase

import com.gilrossi.movie_recommendation.application.port.out.RatingEventPublisher
import com.gilrossi.movie_recommendation.dto.request.RatingRequest
import com.gilrossi.movie_recommendation.dto.response.RatingResponse
import com.gilrossi.movie_recommendation.event.RatingEvent
import com.gilrossi.movie_recommendation.exception.ContentNotFoundException
import com.gilrossi.movie_recommendation.exception.GenreNotFoundException
import com.gilrossi.movie_recommendation.exception.PersonNotFoundException
import com.gilrossi.movie_recommendation.exception.RatingNotFoundException
import com.gilrossi.movie_recommendation.exception.UserNotFoundException
import com.gilrossi.movie_recommendation.model.ContentType
import com.gilrossi.movie_recommendation.model.Rating
import com.gilrossi.movie_recommendation.model.RatingTargetType
import com.gilrossi.movie_recommendation.recommendation.RatingNormalizer
import com.gilrossi.movie_recommendation.repository.AppUserRepository
import com.gilrossi.movie_recommendation.repository.ContentRepository
import com.gilrossi.movie_recommendation.repository.GenreRepository
import com.gilrossi.movie_recommendation.repository.PersonRepository
import com.gilrossi.movie_recommendation.repository.RatingRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class RatingUseCase(
    private val ratingRepository: RatingRepository,
    private val userRepository: AppUserRepository,
    private val contentRepository: ContentRepository,
    private val personRepository: PersonRepository,
    private val genreRepository: GenreRepository,
    private val normalizer: RatingNormalizer,
    private val eventPublisher: RatingEventPublisher
) {
    suspend fun list(userId: Long): List<RatingResponse> {
        requireUser(userId)
        return ratingRepository.findAllByUserId(userId).map(::toResponse).toList()
    }

    suspend fun rate(userId: Long, request: RatingRequest): RatingResponse {
        requireUser(userId)
        validateTarget(request)
        normalizer.normalize(request.value)
        val existing = findExisting(userId, request)
        val now = Instant.now()
        val candidate = Rating(
            id = existing?.id,
            userId = userId,
            targetType = request.targetType,
            value = request.value,
            contentId = request.targetId.takeIf { request.targetType.isContent },
            personId = request.targetId.takeIf { request.targetType.isPerson },
            genreId = request.targetId.takeIf { request.targetType == RatingTargetType.GENRE },
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )
        val saved = try {
            ratingRepository.save(candidate)
        } catch (_: DuplicateKeyException) {
            val concurrent = findExisting(userId, request) ?: throw RatingNotFoundException()
            ratingRepository.save(candidate.copy(id = concurrent.id, createdAt = concurrent.createdAt))
        }
        eventPublisher.publish(
            RatingEvent(UUID.randomUUID(), requireNotNull(saved.id), userId, saved.targetType, saved.targetId(), saved.value, now)
        )
        return toResponse(saved)
    }

    suspend fun delete(userId: Long, ratingId: Long) {
        requireUser(userId)
        val rating = ratingRepository.findById(ratingId)
            ?.takeIf { it.userId == userId }
            ?: throw RatingNotFoundException()
        ratingRepository.delete(rating)
    }

    private suspend fun validateTarget(request: RatingRequest) {
        when {
            request.targetType.isContent -> {
                val content = contentRepository.findById(request.targetId) ?: throw ContentNotFoundException("Conteúdo não encontrado.")
                val expected = if (request.targetType == RatingTargetType.MOVIE) ContentType.MOVIE else ContentType.SERIES
                require(content.type == expected) { "O tipo do rating não corresponde ao conteúdo." }
            }
            request.targetType.isPerson -> {
                if (!personRepository.existsById(request.targetId)) throw PersonNotFoundException()
            }
            else -> {
                if (!genreRepository.existsById(request.targetId)) throw GenreNotFoundException()
            }
        }
    }

    private suspend fun findExisting(userId: Long, request: RatingRequest): Rating? = when {
        request.targetType.isContent -> ratingRepository.findByUserIdAndTargetTypeAndContentId(userId, request.targetType, request.targetId)
        request.targetType.isPerson -> ratingRepository.findByUserIdAndTargetTypeAndPersonId(userId, request.targetType, request.targetId)
        else -> ratingRepository.findByUserIdAndTargetTypeAndGenreId(userId, request.targetType, request.targetId)
    }

    private suspend fun requireUser(userId: Long) {
        if (!userRepository.existsById(userId)) throw UserNotFoundException()
    }

    private fun toResponse(rating: Rating) = RatingResponse(
        id = requireNotNull(rating.id), userId = rating.userId, targetType = rating.targetType,
        targetId = rating.targetId(), value = rating.value,
        normalizedValue = normalizer.normalize(rating.value), updatedAt = rating.updatedAt
    )
}
