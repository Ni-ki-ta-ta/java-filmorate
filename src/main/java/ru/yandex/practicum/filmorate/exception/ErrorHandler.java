package ru.yandex.practicum.filmorate.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidationException(
            final ValidationException e
    ) {
        log.error("Ошибка валидации: {}", e.getMessage());

        return Map.of(
                "error", e.getMessage()
        );
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleMethodArgumentNotValidException(
            final MethodArgumentNotValidException e
    ) {
        String errorMessage = e.getBindingResult()
                .getAllErrors()
                .getFirst()
                .getDefaultMessage();

        log.error("Ошибка валидации: {}", errorMessage);

        return Map.of(
                "error", errorMessage
        );
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFoundException(
            final NotFoundException e
    ) {
        log.error("Ошибка поиска объекта: {}", e.getMessage());

        return Map.of(
                "error", e.getMessage()
        );
    }
}