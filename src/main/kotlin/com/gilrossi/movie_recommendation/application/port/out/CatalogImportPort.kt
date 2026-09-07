package com.gilrossi.movie_recommendation.application.port.out

import com.gilrossi.movie_recommendation.model.Content
import com.gilrossi.movie_recommendation.model.Genre
import com.gilrossi.movie_recommendation.model.Person

interface CatalogImportPort {
    suspend fun persist(
        content: Content,
        genres: List<Genre>,
        actors: List<Person>,
        directorsOrCreators: List<Person>
    ): Content
}
