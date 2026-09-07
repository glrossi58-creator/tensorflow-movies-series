package com.gilrossi.movie_recommendation.application.usecase

import com.gilrossi.movie_recommendation.application.port.out.CatalogImportPort
import com.gilrossi.movie_recommendation.application.port.out.TmdbClient
import com.gilrossi.movie_recommendation.dto.TmdbAggregateCastMember
import com.gilrossi.movie_recommendation.dto.TmdbCastMember
import com.gilrossi.movie_recommendation.dto.TmdbCreator
import com.gilrossi.movie_recommendation.dto.TmdbCrewMember
import com.gilrossi.movie_recommendation.dto.TmdbGenre
import com.gilrossi.movie_recommendation.dto.response.TmdbCreditsResponse
import com.gilrossi.movie_recommendation.dto.response.TmdbMovieDetailsResponse
import com.gilrossi.movie_recommendation.dto.response.TmdbSeriesAggregateCreditsResponse
import com.gilrossi.movie_recommendation.dto.response.TmdbSeriesDetailsResponse
import com.gilrossi.movie_recommendation.model.Content
import com.gilrossi.movie_recommendation.model.ContentType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ImportUseCasesTest {
    private val client = mockk<TmdbClient>()
    private val persistence = mockk<CatalogImportPort>()

    @Test
    fun `movie import selects ordered top ten actors and directors`() = runTest {
        val details = TmdbMovieDetailsResponse(603, "Matrix", null, listOf(TmdbGenre(1, "Action")), null, null, 8.0)
        val cast = (0..11).reversed().map { TmdbCastMember(it, "Actor $it", null, it) }
        coEvery { client.getMovieDetails(603) } returns details
        coEvery { client.getMovieCredits(603) } returns TmdbCreditsResponse(603, cast, listOf(TmdbCrewMember(90, "Director", "Director", null), TmdbCrewMember(91, "Writer", "Writer", null)))
        coEvery { persistence.persist(any(), any(), any(), any()) } answers { firstArg<Content>().copy(id = 8) }

        val result = ImportMovieUseCase(client, persistence).execute(603)

        assertEquals(ContentType.MOVIE, result.type)
        coVerify { persistence.persist(any(), any(), match { it.size == 10 && it.first().tmdbId == 0 && it.last().tmdbId == 9 }, match { it.single().tmdbId == 90 }) }
    }

    @Test
    fun `series import uses aggregate cast and creators`() = runTest {
        coEvery { client.getSeriesDetails(10) } returns TmdbSeriesDetailsResponse(10, "Show", null, emptyList(), null, null, listOf(TmdbCreator(5, "Creator")))
        coEvery { client.getSeriesAggregateCredits(10) } returns TmdbSeriesAggregateCreditsResponse(10, (0..10).map { TmdbAggregateCastMember(it, "A$it", 10 - it) })
        coEvery { persistence.persist(any(), any(), any(), any()) } answers { firstArg<Content>().copy(id = 1) }

        ImportSeriesUseCase(client, persistence).execute(10)

        coVerify { persistence.persist(match { it.type == ContentType.SERIES }, any(), match { it.size == 10 && it.first().tmdbId == 10 }, match { it.single().tmdbId == 5 }) }
    }
}
