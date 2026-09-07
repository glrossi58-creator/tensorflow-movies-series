package com.gilrossi.movie_recommendation.exception

sealed class TmdbException(message: String) : RuntimeException(message)

class TmdbTimeoutException : TmdbException("TMDB não respondeu dentro do tempo limite.")

class TmdbUnavailableException(message: String = "TMDB está temporariamente indisponível.") :
    TmdbException(message)

class TmdbResourceNotFoundException : TmdbException("Recurso não encontrado no TMDB.")

class TmdbConfigurationException : TmdbException(
    "Integração TMDB não configurada. Defina TMDB_READ_ACCESS_TOKEN."
)
