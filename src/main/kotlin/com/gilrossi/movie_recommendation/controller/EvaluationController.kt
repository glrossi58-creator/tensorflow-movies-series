package com.gilrossi.movie_recommendation.controller

import com.gilrossi.movie_recommendation.evaluation.*
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/users/{userId}")
class EvaluationController(private val service: EvaluationService) {
    @GetMapping("/contents/{contentId}/evaluation")
    suspend fun content(@PathVariable userId: Long, @PathVariable contentId: Long) = service.content(userId, contentId)
    @PutMapping("/contents/{contentId}/evaluation")
    suspend fun update(@PathVariable userId: Long, @PathVariable contentId: Long, @Valid @RequestBody request: EvaluationUpdate) = service.update(userId, contentId, request)
    @GetMapping("/people/{personId}/evaluation")
    suspend fun person(@PathVariable userId: Long, @PathVariable personId: Long) = service.person(userId, personId)
    @GetMapping("/genres") suspend fun genres(@PathVariable userId: Long) = service.genres(userId)
    @GetMapping("/ratings/view") suspend fun history(@PathVariable userId: Long) = service.history(userId)
}
