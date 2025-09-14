package com.abrik.bank_cards.bank_cards.controller.user;

import com.abrik.bank_cards.bank_cards.dto.card.*;
import com.abrik.bank_cards.bank_cards.dto.common.PageResponse;
import com.abrik.bank_cards.bank_cards.security.MyUserDetails;
import com.abrik.bank_cards.bank_cards.service.user.CardService;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {
    private final CardService cardService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CardResponse createCard(@AuthenticationPrincipal MyUserDetails myUserDetails,
                                   @RequestBody CreateCardRequest request) {
        return cardService.createCard(myUserDetails.getUserId(), request);
    }

    @PatchMapping("/{cardId}")
    public CardResponse updateCard(@AuthenticationPrincipal MyUserDetails myUserDetails,
                                @RequestBody UpdateCardRequest request,
                                @PathVariable UUID cardId) {
        return cardService.updateMyCard(myUserDetails.getUserId(), cardId, request);
    }

    @GetMapping
    public PageResponse<CardResponse> listMyCards(
            @AuthenticationPrincipal MyUserDetails myUserDetails,
            @RequestParam(required = false) CardStatus status,
            @RequestParam(required = false) String search,
            @ParameterObject Pageable pageable) {
        return cardService.listMyCards(myUserDetails.getUserId(), status, search, pageable);
    }

    @GetMapping("/{cardId}")
    public CardResponse getCard(@AuthenticationPrincipal MyUserDetails myUserDetails,
                                @PathVariable UUID cardId) {
        return cardService.getMyCard(myUserDetails.getUserId(), cardId);
    }

    @PostMapping("/{cardId}/request-block")
    public StatusResponse requestBlock(@AuthenticationPrincipal MyUserDetails myUserDetails,
                                       @PathVariable UUID cardId) {
        return cardService.requestBlock(myUserDetails.getUserId(), cardId);
    }

    @DeleteMapping("/{cardId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCard(@AuthenticationPrincipal MyUserDetails myUserDetails,
                            @PathVariable UUID cardId) {
        cardService.deleteCard(myUserDetails.getUserId(), cardId);
    }
}
