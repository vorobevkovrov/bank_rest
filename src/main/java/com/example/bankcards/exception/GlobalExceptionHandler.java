package com.example.bankcards.exception;

import com.example.bankcards.exception.exceptions.*;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

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
 * @author Maxim Vorobev
 * @version 1.0
 * @see ErrorResponse
 * @see ResponseEntity
 * @see org.springframework.web.bind.annotation.ExceptionHandler
 * @see org.springframework.web.bind.annotation.RestControllerAdvice
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
     * @param ex      исключение типа {@link ResourceNotFoundException}
     * @param request HTTP запрос, вызвавший исключение
     * @return {@link ResponseEntity} с {@link ErrorResponse} и HTTP статусом 404
     * @example <pre>
     * GET /api/cards/999999 → 404 Not Found
     * {
     *   "error": "Resource Not Found",
     *   "message": "Card not found with id: 999999"
     * }
     * </pre>
     * @see ResourceNotFoundException
     * @see HttpStatus#NOT_FOUND
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

        log.warn("Resource not found. Path: {}, Message: {}",
                getCurrentPath(request), ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    /**
     * Обрабатывает исключение {@link CardException}.
     *
     * <p>Используется для ошибок бизнес-логики, связанных с операциями с картами.
     * Например: неверный номер карты, карта заблокирована, превышение лимитов.</p>
     *
     * @param ex      исключение типа {@link CardException}
     * @param request HTTP запрос, вызвавший исключение
     * @return {@link ResponseEntity} с {@link ErrorResponse} и HTTP статусом 400
     * @example <pre>
     * POST /api/cards/block → 400 Bad Request
     * {
     *   "error": "Card Operation Error",
     *   "message": "Card is already blocked"
     * }
     * </pre>
     * @see CardException
     * @see HttpStatus#BAD_REQUEST
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
     * @param ex      исключение типа {@link AccessDeniedException}
     * @param request HTTP запрос, вызвавший исключение
     * @return {@link ResponseEntity} с {@link ErrorResponse} и HTTP статусом 403
     * @example <pre>
     * Клиент с ролью USER пытается получить доступ к админскому ресурсу → 403 Forbidden
     * </pre>
     * @see AccessDeniedException
     * @see HttpStatus#FORBIDDEN
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
     * @param ex      исключение типа {@link MethodArgumentNotValidException}
     * @param request HTTP запрос, вызвавший исключение
     * @return {@link ResponseEntity} с {@link ErrorResponse} и HTTP статусом 400
     * @example <pre>
     * POST /api/users с пустым email → 400 Bad Request
     * </pre>
     * @see MethodArgumentNotValidException
     * @see Valid
     * @see HttpStatus#BAD_REQUEST
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Invalid input parameters Validation errors occurred " + ex.getMessage())
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
     * @param ex      исключение любого типа
     * @param request HTTP запрос, вызвавший исключение
     * @return {@link ResponseEntity} с {@link ErrorResponse} и HTTP статусом 500
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
     * @param ex      исключение одного из указанных типов
     * @param request HTTP запрос, вызвавший исключение
     * @return {@link ResponseEntity} с {@link ErrorResponse} и HTTP статусом 401
     * @example <pre>
     * Запрос без заголовка Authorization → 401 Unauthorized
     * </pre>
     * @see MissingAuthorizationHeaderException
     * @see InvalidTokenException
     * @see HttpStatus#UNAUTHORIZED
     */
    @ExceptionHandler({MissingAuthorizationHeaderException.class, InvalidTokenException.class})
    public ResponseEntity<ErrorResponse> handleAuthExceptions(Exception ex, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(ex.getMessage())
                .message("An unexpected error occurred")
                .path(getCurrentPath(request))
                .build();
        log.warn("Unauthorized access: {}", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Обрабатывает исключение {@link UnauthorizedAccessException}.
     *
     * <p>Используется, когда пользователь пытается получить доступ к защищенному ресурсу
     * без успешной аутентификации. Отличие от {@link #handleAccessDenied(AccessDeniedException, HttpServletRequest)}:
     * 401 - нет аутентификации, 403 - есть аутентификация, но нет прав.</p>
     *
     * @param ex      исключение типа {@link UnauthorizedAccessException}
     * @param request HTTP запрос, вызвавший исключение
     * @return {@link ResponseEntity} с {@link ErrorResponse} и HTTP статусом 401
     * @see UnauthorizedAccessException
     * @see HttpStatus#UNAUTHORIZED
     */
    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedAccessException ex,
                                                            HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Forbidden")
                .message(ex.getMessage())
                .path(getCurrentPath(request))
                .build();
        log.error("Unauthorized access:  {}", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    /**
     * Обрабатывает исключение {@link CardRequestStatusException}.
     * <p>
     * Возникает при попытке выполнить операцию с запросом на блокировку карты,
     * когда запрос не находится в статусе PENDING (например, попытка одобрить
     * уже одобренный или отклоненный запрос).
     * </p>
     *
     * @param ex      исключение типа {@link CardRequestStatusException}, содержащее информацию о неверном статусе
     * @param request HTTP запрос, вызвавший исключение
     * @return {@link ResponseEntity} с {@link ErrorResponse} и HTTP статусом 400 (Bad Request)
     * @example <pre>
     * PATCH /api/admin/requests/123/approve для уже одобренного запроса → 400 Bad Request
     * {
     *   "error": "Request is not pending",
     *   "message": "Cannot approve request with status: APPROVED"
     * }
     * </pre>
     * @see CardRequestStatusException
     * @see HttpStatus#BAD_REQUEST
     */
    @ExceptionHandler(CardRequestStatusException.class)
    public ResponseEntity<ErrorResponse> handleCardRequestStatusException(CardRequestStatusException ex,
                                                                          HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Request is not pending")
                .message(ex.getMessage())
                .path(getCurrentPath(request))
                .build();
        log.error("Request is not pending  {}", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);

    }

    /**
     * Обрабатывает исключение {@link UserAlreadyExistsException}.
     * <p>
     * Возникает при попытке регистрации пользователя с именем, которое уже существует в системе.
     * </p>
     *
     * @param ex      исключение типа {@link UserAlreadyExistsException}, содержащее информацию о существующем пользователе
     * @param request HTTP запрос, вызвавший исключение
     * @return {@link ResponseEntity} с {@link ErrorResponse} и HTTP статусом 409 (Conflict)
     * @example <pre>
     * POST /api/auth/register с существующим username → 409 Conflict
     * {
     *   "error": "Conflict",
     *   "message": "User with username 'john_doe' already exists"
     * }
     * </pre>
     * @see UserAlreadyExistsException
     * @see HttpStatus#CONFLICT
     */
    @ExceptionHandler({UserAlreadyExistsException.class})
    public ResponseEntity<ErrorResponse> handleUserAlreadyExistsException(UserAlreadyExistsException ex,
                                                                          HttpServletRequest request) {
        log.error("Conflict: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Conflict")
                .message(ex.getMessage())
                .path(getCurrentPath(request))
                .build();
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);

    }

    /**
     * Обрабатывает исключение {@link CardBlockedException}.
     * <p>
     * Возникает при попытке выполнить операцию с заблокированной картой
     * (например, перевод средств, пополнение, изменение лимитов).
     * </p>
     *
     * @param ex      исключение типа {@link CardBlockedException}, содержащее информацию о заблокированной карте
     * @param request HTTP запрос, вызвавший исключение
     * @return {@link ResponseEntity} с {@link ErrorResponse} и HTTP статусом 409 (Conflict)
     * @example <pre>
     * POST /api/cards/123/transfer с заблокированной картой → 409 Conflict
     * {
     *   "error": "Conflict",
     *   "message": "Card 123 is blocked"
     * }
     * </pre>
     * @see CardBlockedException
     * @see HttpStatus#CONFLICT
     */
    @ExceptionHandler(CardBlockedException.class)
    public ResponseEntity<ErrorResponse> handleCardBlockedException(
            CardBlockedException ex, HttpServletRequest request) {
        log.error("Card blocked: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Conflict")
                .message(ex.getMessage())
                .path(getCurrentPath(request))
                .build();
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    /**
     * Обрабатывает исключение {@link InsufficientFundsException}.
     * <p>
     * Возникает при попытке выполнить операцию, требующую списания средств,
     * когда на карте недостаточно денег (например, перевод, оплата).
     * </p>
     *
     * @param ex      исключение типа {@link InsufficientFundsException}, содержащее информацию о недостаточных средствах
     * @param request HTTP запрос, вызвавший исключение
     * @return {@link ResponseEntity} с {@link ErrorResponse} и HTTP статусом 400 (Bad Request)
     * @example <pre>
     * POST /api/cards/123/transfer с суммой больше баланса → 400 Bad Request
     * {
     *   "error": "Bad Request",
     *   "message": "Insufficient funds on card 123. Available: 100.00, Required: 500.00"
     * }
     * </pre>
     * @see InsufficientFundsException
     * @see HttpStatus#BAD_REQUEST
     */
    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientFundsException(
            InsufficientFundsException ex, HttpServletRequest request) {
        log.error("Insufficient funds: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(ex.getMessage())
                .path(getCurrentPath(request))
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Обрабатывает исключение {@link CardNotActiveException}.
     * <p>
     * Возникает при попытке выполнить операцию с неактивной картой
     * (например, карта заблокирована администратором или просрочена).
     * </p>
     *
     * @param ex      исключение типа {@link CardNotActiveException}, содержащее информацию о неактивной карте
     * @param request HTTP запрос, вызвавший исключение
     * @return {@link ResponseEntity} с {@link ErrorResponse} и HTTP статусом 400 (Bad Request)
     * @example <pre>
     * POST /api/cards/123/transfer с неактивной картой → 400 Bad Request
     * {
     *   "error": "Bad Request",
     *   "message": "Card 123 is not active"
     * }
     * </pre>
     * @see CardNotActiveException
     * @see HttpStatus#BAD_REQUEST
     */
    @ExceptionHandler(CardNotActiveException.class)
    public ResponseEntity<ErrorResponse> handleCardNotActiveException(CardNotActiveException ex,
                                                                      HttpServletRequest request) {
        log.error("This card not active: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(ex.getMessage())
                .path(getCurrentPath(request))
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Обрабатывает исключение {@link ExpiredJwtException}.
     * <p>
     * Возникает при использовании просроченного JWT токена для доступа к защищенным ресурсам.
     * Клиент должен обновить токен или выполнить повторную аутентификацию.
     * </p>
     *
     * @param ex      исключение типа {@link ExpiredJwtException}, содержащее информацию о просроченном токене
     * @param request HTTP запрос, вызвавший исключение
     * @return {@link ResponseEntity} с {@link ErrorResponse} и HTTP статусом 401 (Unauthorized)
     * @example <pre>
     * GET /api/user/cards с истекшим токеном → 401 Unauthorized
     * {
     *   "error": "Token expired",
     *   "message": "JWT token has expired"
     * }
     * </pre>
     * @see ExpiredJwtException
     * @see HttpStatus#UNAUTHORIZED
     * @see com.example.bankcards.config.JwtAuthenticationFilter
     */
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ErrorResponse> handleExpiredJwtException(ExpiredJwtException ex, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Token expired")
                .message(ex.getMessage())
                .path(getCurrentPath(request))
                .build();
        log.error("Token expired {}", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Обрабатывает исключение {@link InvalidCredentialsException}.
     * <p>
     * Возникает при использовании недействительных учетных данных
     * (например, неверный username или password при аутентификации).
     * </p>
     *
     * @param ex      исключение типа {@link InvalidCredentialsException}
     * @param request HTTP запрос, вызвавший исключение
     * @return {@link ResponseEntity} с {@link ErrorResponse} и HTTP статусом 404 (Not Found)
     * @deprecated Этот метод, вероятно, содержит ошибку в сигнатуре параметра.
     *             Должен принимать {@link InvalidCredentialsException}, а не {@link ExpiredJwtException}.
     *             Рекомендуется исправить параметр метода.
     * @see InvalidCredentialsException
     * @see HttpStatus#NOT_FOUND
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentialsException(
            ExpiredJwtException ex, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Invalid username or password")
                .message(ex.getMessage())
                .path(getCurrentPath(request))
                .build();
        log.error("Invalid username or password {}", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    /**
     * Обрабатывает исключение {@link BadCredentialsException}.
     * <p>
     * Возникает при неверных учетных данных во время аутентификации
     * (стандартное исключение Spring Security).
     * </p>
     *
     * @param ex      исключение типа {@link BadCredentialsException}
     * @param request HTTP запрос, вызвавший исключение
     * @return {@link ResponseEntity} с {@link ErrorResponse} и HTTP статусом 401 (Unauthorized)
     * @example <pre>
     * POST /api/auth/login с неверным паролем → 401 Unauthorized
     * {
     *   "error": "Unauthorized",
     *   "message": "Invalid username or password"
     * }
     * </pre>
     * @see BadCredentialsException
     * @see HttpStatus#UNAUTHORIZED
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException ex,
                                                                       HttpServletRequest request) {
        log.error("Bad credentials: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Unauthorized")
                .message("Invalid username or password")
                .path(getCurrentPath(request))
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    /**
     * Обрабатывает исключение {@link SameCardTransferException}.
     * <p>
     * Возникает при попытке выполнить перевод средств с карты на ту же самую карту.
     * </p>
     *
     * @param ex      исключение типа {@link SameCardTransferException}
     * @param request HTTP запрос, вызвавший исключение
     * @return {@link ResponseEntity} с {@link ErrorResponse} и HTTP статусом 400 (Bad Request)
     * @example <pre>
     * POST /api/transfers с одинаковыми sourceCardId и destinationCardId → 400 Bad Request
     * {
     *   "error": "Bad Request",
     *   "message": "Cannot transfer from card 123 to itself"
     * }
     * </pre>
     * @see SameCardTransferException
     * @see HttpStatus#BAD_REQUEST
     */
    @ExceptionHandler({SameCardTransferException.class})
    public ResponseEntity<ErrorResponse> handleSameCardTransferException(SameCardTransferException ex,
                                                                         HttpServletRequest request) {
        log.error("Same card transfer attempt: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(ex.getMessage())
                .path(getCurrentPath(request))
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    /**
     * Обрабатывает исключения, возникающие при шифровании данных.
     * <p>
     * Данный обработчик вызывается при ошибках в процессе шифрования конфиденциальных данных,
     * таких как номера банковских карт. Ошибки шифрования могут возникать по следующим причинам:
     * <ul>
     *   <li>Неверный или поврежденный ключ шифрования</li>
     *   <li>Проблемы с алгоритмом шифрования</li>
     *   <li>Некорректные входные данные для шифрования</li>
     *   <li>Внутренние ошибки криптографической библиотеки</li>
     * </ul>
     * </p>
     *
     * <p>
     * <strong>Важно:</strong> Подробности ошибки логируются на сервере, но клиенту возвращается
     * обобщенное сообщение без технических деталей для обеспечения безопасности.
     * </p>
     *
     * @param ex      исключение типа {@link EncryptionException}, содержащее информацию об ошибке шифрования
     * @param request HTTP запрос, в процессе обработки которого возникла ошибка
     * @return {@link ResponseEntity} с {@link ErrorResponse} и HTTP статусом 500 (Internal Server Error)
     *
     * @see EncryptionException
     * @see HttpStatus#INTERNAL_SERVER_ERROR
     * @see com.example.bankcards.util.EncryptionService#encrypt(String)
     */
    @ExceptionHandler(EncryptionException.class)
    public ResponseEntity<ErrorResponse>handleEncryptionException(EncryptionException ex,
                                                                  HttpServletRequest request){
        log.error("Encryption error: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Failed to encrypt sensitive data")
                .message(ex.getMessage())
                .path(getCurrentPath(request))
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    /**
     * Обрабатывает исключения, возникающие при дешифровании данных.
     * <p>
     * Данный обработчик вызывается при ошибках в процессе дешифрования конфиденциальных данных,
     * например, при получении баланса карты или просмотре деталей карты. Ошибки дешифрования
     * могут возникать по следующим причинам:
     * <ul>
     *   <li>Неверный или поврежденный ключ шифрования</li>
     *   <li>Попытка дешифровать поврежденные или некорректные данные</li>
     *   <li>Несоответствие алгоритма шифрования</li>
     *   <li>Внутренние ошибки криптографической библиотеки</li>
     * </ul>
     * </p>
     *
     * <p>
     * <strong>Важно:</strong> Подробности ошибки логируются на сервере, но клиенту возвращается
     * обобщенное сообщение без технических деталей для обеспечения безопасности. В некоторых случаях
     * ошибка дешифрования может указывать на повреждение данных в базе, что требует вмешательства
     * администратора.
     * </p>
     *
     * @param ex      исключение типа {@link DecryptedException}, содержащее информацию об ошибке дешифрования
     * @param request HTTP запрос, в процессе обработки которого возникла ошибка
     * @return {@link ResponseEntity} с {@link ErrorResponse} и HTTP статусом 500 (Internal Server Error)

     * @see DecryptedException
     * @see HttpStatus#INTERNAL_SERVER_ERROR
     * @see com.example.bankcards.util.EncryptionService#decrypt(String)
     */
    @ExceptionHandler(DecryptedException.class)
    public ResponseEntity<ErrorResponse>handleDecryptionException(DecryptedException ex,
                                                                  HttpServletRequest request){
        log.error("Decryption error: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Failed to decrypt sensitive data")
                .message(ex.getMessage())
                .path(getCurrentPath(request))
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Извлекает текущий путь запроса из {@link HttpServletRequest}.
     *
     * <p>Используется для добавления информации о запросе в {@link ErrorResponse},
     * что упрощает отладку и анализ ошибок.</p>
     *
     * @param request HTTP запрос
     * @return полный URL запроса в виде строки
     * @see HttpServletRequest#getRequestURL()
     */
    private String getCurrentPath(HttpServletRequest request) {
        return request.getRequestURL().toString();
    }
}