package com.gilrossi.movie_recommendation.service

import com.gilrossi.movie_recommendation.dto.request.ContentRequest
import com.gilrossi.movie_recommendation.exception.ContentNotFoundException
import com.gilrossi.movie_recommendation.model.Content
import com.gilrossi.movie_recommendation.model.ContentType
import com.gilrossi.movie_recommendation.repository.ContentRepository
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service

@Service
class ContentService (
    private val contentRepository: ContentRepository
){

    suspend fun getContents(): List<Content> {

        return contentRepository.findAll().toList()

    }

    suspend fun getContentById (id: Long): Content{

        return contentRepository.findById(id)
            ?: throw ContentNotFoundException("Conteúdo não encontrado.")

    }

    suspend fun createContent(request: ContentRequest): Content {

        val content = Content(
            id = null,
            title = request.title,
            tmdbId = request.tmdbId,
            type = request.type,
            overview = request.overview,
            releaseDate = request.releaseDate,
            posterPath = request.posterPath
        )

        return contentRepository.save(content)
    }

    suspend fun updateContent(
        id: Long,
        request: ContentRequest
    ): Content {

        val content = contentRepository.findById(id) ?: throw ContentNotFoundException("Conteúdo não encontrado.")

        val updatedContent = content.copy(
            title = request.title,
            tmdbId = request.tmdbId,
            type = request.type,
            overview = request.overview,
            releaseDate = request.releaseDate,
            posterPath = request.posterPath
        )

        return contentRepository.save(updatedContent)
    }

    suspend fun deleteContent (
        id: Long
    ) {
        val content = contentRepository.findById(id) ?: throw ContentNotFoundException("Conteúdo não encontrado.")

        contentRepository.delete(content)
    }

}