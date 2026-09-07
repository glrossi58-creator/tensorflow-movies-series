package com.gilrossi.movie_recommendation.service

import com.gilrossi.movie_recommendation.dto.request.UserRequest
import com.gilrossi.movie_recommendation.exception.ConflictException
import com.gilrossi.movie_recommendation.exception.UserNotFoundException
import com.gilrossi.movie_recommendation.model.AppUser
import com.gilrossi.movie_recommendation.repository.AppUserRepository
import kotlinx.coroutines.flow.toList
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class UserService(private val repository: AppUserRepository) {
    suspend fun list(): List<AppUser> = repository.findAll().toList()

    suspend fun get(id: Long): AppUser = repository.findById(id) ?: throw UserNotFoundException()

    suspend fun create(request: UserRequest): AppUser {
        val now = Instant.now()
        val email = request.email.trim().lowercase()
        if (repository.findByEmailIgnoreCase(email) != null) throw ConflictException("E-mail já cadastrado.")
        return try {
            repository.save(AppUser(null, request.name.trim(), email, now, now))
        } catch (_: DuplicateKeyException) {
            throw ConflictException("E-mail já cadastrado.")
        }
    }

    suspend fun update(id: Long, request: UserRequest): AppUser {
        val current = get(id)
        val email = request.email.trim().lowercase()
        val owner = repository.findByEmailIgnoreCase(email)
        if (owner != null && owner.id != id) throw ConflictException("E-mail já cadastrado.")
        return try {
            repository.save(current.copy(name = request.name.trim(), email = email, updatedAt = Instant.now()))
        } catch (_: DuplicateKeyException) {
            throw ConflictException("E-mail já cadastrado.")
        }
    }

    suspend fun delete(id: Long) = repository.delete(get(id))
}
