package com.gilrossi.movie_recommendation.exception

class UserNotFoundException(message: String = "Usuário não encontrado.") : RuntimeException(message)
class RatingNotFoundException(message: String = "Avaliação não encontrada.") : RuntimeException(message)
class GenreNotFoundException(message: String = "Gênero não encontrado.") : RuntimeException(message)
class PersonNotFoundException(message: String = "Pessoa não encontrada.") : RuntimeException(message)
class ConflictException(message: String) : RuntimeException(message)
