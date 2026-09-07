package com.gilrossi.movie_recommendation.mapper

import com.gilrossi.movie_recommendation.dto.TmdbCastMember
import com.gilrossi.movie_recommendation.dto.TmdbAggregateCastMember
import com.gilrossi.movie_recommendation.dto.TmdbCreator
import com.gilrossi.movie_recommendation.dto.TmdbCrewMember
import com.gilrossi.movie_recommendation.model.Person

object TmdbPersonMapper {

    fun toPerson(
        castMember: TmdbCastMember
    ): Person {

        return Person(
            id = null,
            tmdbId = castMember.id,
            name = castMember.name
        )
    }

    fun toPerson(
        crewMember: TmdbCrewMember
    ): Person? {

        val name = crewMember.name
            ?.takeIf { it.isNotBlank() }
            ?: return null

        return Person(
            id = null,
            tmdbId = crewMember.id,
            name = name
        )
    }

    fun toPerson(castMember: TmdbAggregateCastMember): Person = Person(
        id = null,
        tmdbId = castMember.id,
        name = castMember.name
    )

    fun toPerson(creator: TmdbCreator): Person = Person(
        id = null,
        tmdbId = creator.id,
        name = creator.name
    )
}
