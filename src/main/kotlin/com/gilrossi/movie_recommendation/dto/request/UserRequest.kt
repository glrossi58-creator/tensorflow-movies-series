package com.gilrossi.movie_recommendation.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UserRequest(
    @field:NotBlank(message = "O nome é obrigatório.")
    @field:Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.")
    val name: String,
    @field:NotBlank(message = "O e-mail é obrigatório.")
    @field:Email(message = "O e-mail deve ser válido.")
    @field:Size(max = 320, message = "O e-mail deve ter no máximo 320 caracteres.")
    val email: String
)
