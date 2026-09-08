package com.gilrossi.movie_recommendation.controller

import com.gilrossi.movie_recommendation.discovery.*
import com.gilrossi.movie_recommendation.dto.request.UserRequest
import com.gilrossi.movie_recommendation.service.UserService
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

data class ProfileRequest(@field:NotBlank @field:Size(max = 150) val name: String)

@RestController
class DiscoveryController(private val discovery: DiscoveryService, private val catalog: CatalogDiscoveryService, private val users: UserService) {
    @GetMapping("/discovery/search")
    suspend fun search(@RequestParam q: String, @RequestParam(required = false) type: DiscoveryType?) = discovery.search(q, type)
    @PostMapping("/discovery/import/{type}/{tmdbId}")
    suspend fun import(@PathVariable type: DiscoveryType, @PathVariable tmdbId: Int): Any =
        if (type == DiscoveryType.PERSON) discovery.importPerson(tmdbId) else catalog.import(tmdbId, type)
    @GetMapping("/users/{userId}/quick-rating")
    suspend fun quick(@PathVariable userId: Long, @RequestParam(defaultValue = "") skip: String, @RequestParam(defaultValue = "40") limit: Int) =
        catalog.quick(userId, skip.split(',').filter(String::isNotBlank).toSet(), limit)
    @PostMapping("/profiles") @ResponseStatus(HttpStatus.CREATED)
    suspend fun createProfile(@RequestBody @Valid request: ProfileRequest) =
        users.create(UserRequest(request.name, "profile-${UUID.randomUUID()}@local.invalid"))
    @GetMapping("/health") fun health() = mapOf("status" to "UP")
}
