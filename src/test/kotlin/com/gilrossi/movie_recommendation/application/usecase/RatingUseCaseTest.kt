package com.gilrossi.movie_recommendation.application.usecase

import com.gilrossi.movie_recommendation.application.port.out.RatingEventPublisher
import com.gilrossi.movie_recommendation.dto.request.RatingRequest
import com.gilrossi.movie_recommendation.model.Rating
import com.gilrossi.movie_recommendation.model.RatingTargetType
import com.gilrossi.movie_recommendation.recommendation.RatingNormalizer
import com.gilrossi.movie_recommendation.repository.AppUserRepository
import com.gilrossi.movie_recommendation.repository.ContentRepository
import com.gilrossi.movie_recommendation.repository.GenreRepository
import com.gilrossi.movie_recommendation.repository.PersonRepository
import com.gilrossi.movie_recommendation.repository.RatingRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RatingUseCaseTest {
    private val ratings = mockk<RatingRepository>()
    private val users = mockk<AppUserRepository>()
    private val contents = mockk<ContentRepository>()
    private val people = mockk<PersonRepository>()
    private val genres = mockk<GenreRepository>()
    private val publisher = mockk<RatingEventPublisher>(relaxed = true)
    private val useCase = RatingUseCase(ratings, users, contents, people, genres, RatingNormalizer(), publisher)

    @Test
    fun `creates actor rating and publishes event with normalized response`() = runTest {
        coEvery { users.existsById(1) } returns true
        coEvery { people.existsById(9) } returns true
        coEvery { ratings.findByUserIdAndTargetTypeAndPersonId(1, RatingTargetType.ACTOR, 9) } returns null
        coEvery { ratings.save(any()) } answers { firstArg<Rating>().copy(id = 33) }

        val response = useCase.rate(1, RatingRequest(RatingTargetType.ACTOR, 9, 4))

        assertEquals(0.75, response.normalizedValue)
        coVerify { publisher.publish(match { it.ratingId == 33L && it.targetType == RatingTargetType.ACTOR }) }
    }

    @Test
    fun `updates creator independently from actor role`() = runTest {
        coEvery { users.existsById(1) } returns true
        coEvery { people.existsById(9) } returns true
        coEvery { ratings.findByUserIdAndTargetTypeAndPersonId(1, RatingTargetType.CREATOR, 9) } returns null
        coEvery { ratings.save(any()) } answers { firstArg<Rating>().copy(id = 34) }

        useCase.rate(1, RatingRequest(RatingTargetType.CREATOR, 9, 2))

        coVerify { ratings.findByUserIdAndTargetTypeAndPersonId(1, RatingTargetType.CREATOR, 9) }
    }
}
