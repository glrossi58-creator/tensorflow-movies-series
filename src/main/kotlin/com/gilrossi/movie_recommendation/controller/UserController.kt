package com.gilrossi.movie_recommendation.controller

import com.gilrossi.movie_recommendation.dto.request.UserRequest
import com.gilrossi.movie_recommendation.model.AppUser
import com.gilrossi.movie_recommendation.service.UserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users")
class UserController(private val service: UserService) {
    @GetMapping suspend fun list(): List<AppUser> = service.list()
    @GetMapping("/{id}") suspend fun get(@PathVariable id: Long): AppUser = service.get(id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun create(@RequestBody @Valid request: UserRequest): AppUser = service.create(request)

    @PutMapping("/{id}")
    suspend fun update(@PathVariable id: Long, @RequestBody @Valid request: UserRequest): AppUser = service.update(id, request)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    suspend fun delete(@PathVariable id: Long) = service.delete(id)
}
