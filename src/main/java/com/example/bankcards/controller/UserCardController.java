package com.example.bankcards.controller;

import com.example.bankcards.dto.request.BlockCardRequest;
import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.dto.response.BalanceResponse;
import com.example.bankcards.dto.response.CardRequestResponse;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.dto.response.TransferResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.service.CardRequestService;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.TransactionService;
import com.example.bankcards.util.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User Card Management", description = "Operations related to user's cards management")
@SecurityRequirement(name = "Bearer Authentication")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ROLE_USER')")
@RestController
@RequestMapping("/api/v1/cards")
public class UserCardController {
    private final CardService cardService;
    private final JwtService jwtService;
    private final TransactionService transactionService;
    private final CardRequestService cardRequestService;

    /**
     * Retrieve the authenticated user's cards with pagination and optional search.
     *
     * @param request  HTTP request containing the JWT token in the `Authorization` header
     * @param pageable Spring `Pageable` object defining page size and sort order
     * @return `Page<CardResponse>` containing the card information
     */
    @Operation(
            summary = "Get all user's cards",
            description = "Retrieve paginated list of user's cards with sorting",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved cards"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized")
            }
    )
    @GetMapping("/all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<CardResponse>> getAllCards(@Parameter(hidden = true) HttpServletRequest request,
                                                          @PageableDefault(size = 20,
                                                                  sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("public void GetAllCards user");
        String authorizationHeader = request.getHeader("Authorization");
        Page<CardResponse> cards = cardService.getCardsByUserId(jwtService.extractUserId(authorizationHeader.
                substring(7)), pageable);
        return ResponseEntity.status(HttpStatus.FOUND).body(cards);
    }

    @Operation(
            summary = "Get card balance",
            description = "Retrieve balance for specific card",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved balance"),
                    @ApiResponse(responseCode = "403", description = "Access forbidden"),
                    @ApiResponse(responseCode = "404", description = "Card not found")
            }
    )
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/{cardId}/balance")
    public ResponseEntity<BalanceResponse> getBalance(@Parameter(description = "Card ID") @PathVariable Long cardId,
                                                      @AuthenticationPrincipal UserDetails userDetails) {
        log.info("public void getBalance(Card card) {}", cardId);
        return ResponseEntity.status(HttpStatus.FOUND).body(cardService.getCardBalance(cardId, userDetails));
    }

    @Operation(
            summary = "Block card",
            description = "Request to block user's card",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Card blocked successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request"),
                    @ApiResponse(responseCode = "403", description = "Access forbidden")
            }
    )

    @PostMapping("/block")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CardRequestResponse> requestCardBlock(
            @Valid @RequestBody BlockCardRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        CardRequestResponse response = cardRequestService.requestCardBlock(
                request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Transfer between cards",
            description = "Transfer funds between user's own cards",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Transfer successful"),
                    @ApiResponse(responseCode = "400", description = "Invalid transfer request"),
                    @ApiResponse(responseCode = "403", description = "Access forbidden")
            }
    )
    @PostMapping("/transfer")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TransferResponse> transferBetweenOwnCards(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Transfer request details",
                    required = true
            )
            @Valid @RequestBody TransferRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        TransferResponse response = transactionService.transferBetweenOwnCards(request, userDetails);
        return ResponseEntity.ok(response);
    }
}
