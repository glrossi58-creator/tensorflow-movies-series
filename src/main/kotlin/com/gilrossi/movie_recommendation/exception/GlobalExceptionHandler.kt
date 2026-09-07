package com.gilrossi.movie_recommendation.exception

import com.gilrossi.movie_recommendation.dto.response.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.server.ServerWebInputException
import org.springframework.dao.DataIntegrityViolationException

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(
        ContentNotFoundException::class,
        UserNotFoundException::class,
        RatingNotFoundException::class,
        GenreNotFoundException::class,
        PersonNotFoundException::class
    )
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleContentNotFound(
        exception: RuntimeException
    ): ErrorResponse {
        return ErrorResponse(
            status = 404,
            message = exception.message ?: "Conteúdo não encontrado."
        )
    }

    @ExceptionHandler(WebExchangeBindException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleValidationException(
        exception: WebExchangeBindException
    ): ErrorResponse{
        val message = exception.bindingResult
            .fieldErrors
            .firstOrNull()
            ?.defaultMessage
            ?: "Dados inválidos."

        return ErrorResponse(
            status = 400,
            message = message
        )
    }

    @ExceptionHandler(ServerWebInputException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleServerWebInputException(
        exception: ServerWebInputException
    ): ErrorResponse {
        return ErrorResponse(
            status = 400,
            message = "Dados inválidos no corpo da requisição."
        )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleIllegalArgument(exception: IllegalArgumentException) =
        ErrorResponse(400, exception.message ?: "Dados inválidos.")

    @ExceptionHandler(ConflictException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleConflict(exception: ConflictException) =
        ErrorResponse(409, exception.message ?: "Conflito de dados.")

    @ExceptionHandler(DataIntegrityViolationException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleDataIntegrityViolation() =
        ErrorResponse(409, "Os dados informados violam uma restrição de integridade.")

    @ExceptionHandler(TmdbResourceNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleTmdbNotFound(exception: TmdbResourceNotFoundException) =
        ErrorResponse(404, exception.message ?: "Recurso não encontrado no TMDB.")

    @ExceptionHandler(TmdbTimeoutException::class)
    @ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
    fun handleTmdbTimeout(exception: TmdbTimeoutException) =
        ErrorResponse(504, exception.message ?: "Timeout ao consultar o TMDB.")

    @ExceptionHandler(TmdbException::class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    fun handleTmdb(exception: TmdbException) =
        ErrorResponse(502, exception.message ?: "Falha ao consultar o TMDB.")

}
