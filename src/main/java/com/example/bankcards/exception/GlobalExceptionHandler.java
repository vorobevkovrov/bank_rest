package com.example.bankcards.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

//TODO читаемая ошибка на протухший токен

/**
 * Глобальный обработчик исключений для REST API приложения BankCards.
 *
 * <p>Этот класс обрабатывает исключения, возникающие в контроллерах, и преобразует их
 * в структурированные HTTP-ответы с соответствующими статус-кодами и сообщениями.
 * Все исключения логируются с соответствующим уровнем логирования для последующего анализа.</p>
 *
 * <h2>Основные функции:</h2>
 * <ul>
 *   <li>Единообразная обработка всех исключений приложения</li>
 *   <li>Преобразование исключений в стандартный формат {@link ErrorResponse}</li>
 *   <li>Автоматическое логирование ошибок с контекстной информацией</li>
 *   <li>Возврат соответствующих HTTP статус-кодов клиенту</li>
 *   <li>Защита от раскрытия внутренней информации об ошибках</li>
 * </ul>
 *
 * <h2>Пример использования:</h2>
 * <pre>
 * {@code
 * // В контроллере выбрасывается исключение
 * throw new ResourceNotFoundException("Card not found with id: 123");
 *
 * // GlobalExceptionHandler автоматически обрабатывает его и возвращает:
 * // HTTP 404 с JSON телом ErrorResponse
 * }
 * </pre>
 *
 * <h2>Формат ответа об ошибке:</h2>
 * <pre>
 * {
 *   "timestamp": "2024-01-15T10:30:45.123",
 *   "status": 404,
 *   "error": "Resource Not Found",
 *   "message": "Card not found with id: 123",
 *   "path": "/api/cards/123"
 * }
 * </pre>
 *
 * @see ErrorResponse
 * @see ResponseEntity
 * @see org.springframework.web.bind.annotation.ExceptionHandler
 * @see org.springframework.web.bind.annotation.RestControllerAdvice
 *
 * @author [Имя разработчика/команды]
 * @version 1.0
 * @since 2024-01-15
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Логгер для записи информации об исключениях.
     * Используется для записи контекстной информации об ошибках с разными уровнями логирования:
     * <ul>
     *   <li>WARN - для ожидаемых бизнес-исключений</li>
     *   <li>ERROR - для критических и неожиданных ошибок</li>
     * </ul>
     */

    /**
     * Обрабатывает исключение {@link ResourceNotFoundException}.
     *
     * <p>Используется, когда запрашиваемый ресурс не найден в системе.
     * Например: карта, пользователь или заявка с указанным ID не существует.</p>
     *
     * @param ex исключение типа {@link ResourceNotFoundException}
     * @param request HTTP запрос, вызвавший исключение
     * @return {@link ResponseEntity} с {@link ErrorResponse} и HTTP статусом 404
     *
     * @see ResourceNotFoundException
     * @see HttpStatus#NOT_FOUND
     *
     * @example
     * <pre>
     * GET /api/cards/999999 → 404 Not Found
     * {
     *   "error": "Resource Not Found",
     *   "message": "Card not found with id: 999999"
     * }
     * </pre>
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Resource Not Found")
                .message(ex.getMessage())
                .path(getCurrentPath(request))
                .build();

        log.warn("Resource not found: {}", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    /**
     * Обрабатывает исключение {@link CardException}.
     *
     * <p>Используется для ошибок бизнес-логики, связанных с операциями с картами.
     * Например: неверный номер карты, карта заблокирована, превышение лимитов.</p>
     *
     * @param ex исключение типа {@link CardException}
     * @param request HTTP запрос, вызвавший исключение
     * @return {@link ResponseEntity} с {@link ErrorResponse} и HTTP статусом 400
     *
     * @see CardException
     * @see HttpStatus#BAD_REQUEST
     *
     * @example
     * <pre>
     * POST /api/cards/block → 400 Bad Request
     * {
     *   "error": "Card Operation Error",
     *   "message": "Card is already blocked"
     * }
     * </pre>
     */
    @ExceptionHandler(CardException.class)
    public ResponseEntity<ErrorResponse> handleCardException(CardException ex, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Card Operation Error")
                .message(ex.getMessage())
                .path(getCurrentPath(request))
                .build();

        log.warn("Card operation error: {}", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Обрабатывает исключение {@link AccessDeniedException}.
     *
     * <p>Используется, когда аутентифицированный пользователь пытается получить доступ
     * к ресурсу, для которого у него недостаточно прав.
     * Отличие от {@link #handleUnauthorized(UnauthorizedAccessException, HttpServletRequest)}:
     * 403 - есть аутентификация, но нет прав; 401 - нет аутентификации.</p>
     *
     * @param ex исключение типа {@link AccessDeniedException}
     * @param request HTTP запрос, вызвавший исключение
     * @return {@link ResponseEntity} с {@link ErrorResponse} и HTTP статусом 403
     *
     * @see AccessDeniedException
     * @see HttpStatus#FORBIDDEN
     *
     * @example
     * <pre>
     * Клиент с ролью USER пытается получить доступ к админскому ресурсу → 403 Forbidden
     * </pre>
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Access Denied")
                .message("You don't have permission to access this resource")
                .path(getCurrentPath(request))
                .build();

        log.warn("Access denied: {}", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    /**
     * Обрабатывает исключения валидации входных данных.
     *
     * <p>Вызывается Spring Framework при ошибках валидации входных параметров методов контроллеров,
     * аннотированных {@code @Valid}. Обрабатывает ошибки, возникающие при нарушении ограничений,
     * определенных в DTO классах (например, {@code @NotNull}, {@code @Size}, {@code @Email}).</p>
     *
     * @param ex исключение типа {@link MethodArgumentNotValidException}
     * @param request HTTP запрос, вызвавший исключение
     * @return {@link ResponseEntity} с {@link ErrorResponse} и HTTP статусом 400
     *
     * @see MethodArgumentNotValidException
     * @see Valid
     * @see HttpStatus#BAD_REQUEST
     *
     * @example
     * <pre>
     * POST /api/users с пустым email → 400 Bad Request
     * </pre>
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Validation errors occurred " + ex.getMessage())
                .path(getCurrentPath(request))
                .build();

        log.warn("Validation errors: {}", error);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Обрабатывает все неперехваченные исключения.
     *
     * <p>Этот метод является обработчиком последней инстанции для всех исключений,
     * которые не были обработаны более специфичными методами.
     * В целях безопасности детали исключения не раскрываются клиенту.</p>
     *
     * <p><b>Важно:</b> Все такие исключения логируются с полным stack trace
     * для последующего анализа разработчиками.</p>
     *
     * @param ex исключение любого типа
     * @param request HTTP запрос, вызвавший исключение
     * @return {@link ResponseEntity} с {@link ErrorResponse} и HTTP статусом 500
     *
     * @see Exception
     * @see HttpStatus#INTERNAL_SERVER_ERROR
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions(Exception ex, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("An unexpected error occurred")
                .path(getCurrentPath(request))
                .build();

        log.error("Unexpected error: ", ex);
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Обрабатывает исключения аутентификации.
     *
     * <p>Групповой обработчик для двух типов исключений, связанных с аутентификацией:
     * <ul>
     *   <li>{@link MissingAuthorizationHeaderException} - отсутствует заголовок Authorization</li>
     *   <li>{@link InvalidTokenException} - невалидный или просроченный JWT токен</li>
     * </ul></p>
     *
     * @param ex исключение одного из указанных типов
     * @param request HTTP запрос, вызвавший исключение
     * @return {@link ResponseEntity} с {@link ErrorResponse} и HTTP статусом 401
     *
     * @see MissingAuthorizationHeaderException
     * @see InvalidTokenException
     * @see HttpStatus#UNAUTHORIZED
     *
     * @example
     * <pre>
     * Запрос без заголовка Authorization → 401 Unauthorized
     * </pre>
     */
    @ExceptionHandler({MissingAuthorizationHeaderException.class, InvalidTokenException.class})
    public ResponseEntity<ErrorResponse> handleAuthExceptions(Exception ex, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("An unexpected error occurred")
                .path(getCurrentPath(request))
                .build();
        log.error("Unexpected error: ", ex);
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Обрабатывает исключение {@link UnauthorizedAccessException}.
     *
     * <p>Используется, когда пользователь пытается получить доступ к защищенному ресурсу
     * без успешной аутентификации. Отличие от {@link #handleAccessDenied(AccessDeniedException, HttpServletRequest)}:
     * 401 - нет аутентификации, 403 - есть аутентификация, но нет прав.</p>
     *
     * @param ex исключение типа {@link UnauthorizedAccessException}
     * @param request HTTP запрос, вызвавший исключение
     * @return {@link ResponseEntity} с {@link ErrorResponse} и HTTP статусом 401
     *
     * @see UnauthorizedAccessException
     * @see HttpStatus#UNAUTHORIZED
     */
    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedAccessException ex, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Forbidden")
                .message(ex.getMessage())
                .path(getCurrentPath(request))
                .build();
        log.error("Unauthorized access:  {}", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Извлекает текущий путь запроса из {@link HttpServletRequest}.
     *
     * <p>Используется для добавления информации о запросе в {@link ErrorResponse},
     * что упрощает отладку и анализ ошибок.</p>
     *
     * @param request HTTP запрос
     * @return полный URL запроса в виде строки
     *
     * @see HttpServletRequest#getRequestURL()
     */
    private String getCurrentPath(HttpServletRequest request) {
        return request.getRequestURL().toString();
    }
}