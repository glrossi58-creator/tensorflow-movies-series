package com.gilrossi.movie_recommendation.mapper

import com.gilrossi.movie_recommendation.dto.TmdbCreator
import com.gilrossi.movie_recommendation.dto.TmdbGenre
import com.gilrossi.movie_recommendation.dto.response.TmdbMovieDetailsResponse
import com.gilrossi.movie_recommendation.dto.response.TmdbSeriesDetailsResponse
import com.gilrossi.movie_recommendation.model.ContentType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TmdbMappersTest {
    @Test
    fun `maps movie details`() {
        val mapped = TmdbContentMapper.toContent(
            TmdbMovieDetailsResponse(603, "The Matrix", "Overview", listOf(TmdbGenre(28, "Action")), "1999-03-30", "/p.jpg", 8.2)
        )
        assertEquals(ContentType.MOVIE, mapped.type)
        assertEquals(LocalDate.of(1999, 3, 30), mapped.releaseDate)
        assertEquals(603, mapped.tmdbId)
    }

    @Test
    fun `maps series details and empty date`() {
        val mapped = TmdbContentMapper.toContent(
            TmdbSeriesDetailsResponse(1, "Series", null, emptyList(), "", null, listOf(TmdbCreator(4, "Creator")))
        )
        assertEquals(ContentType.SERIES, mapped.type)
        assertNull(mapped.releaseDate)
    }
}
