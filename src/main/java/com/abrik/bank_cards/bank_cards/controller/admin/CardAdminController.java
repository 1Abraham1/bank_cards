package com.abrik.bank_cards.bank_cards.controller.admin;

import com.abrik.bank_cards.bank_cards.dto.card.CardResponse;
import com.abrik.bank_cards.bank_cards.dto.card.CardStatus;
import com.abrik.bank_cards.bank_cards.dto.card.CreateCardRequest;
import com.abrik.bank_cards.bank_cards.dto.card.RequestStatusResponse;
import com.abrik.bank_cards.bank_cards.dto.common.PageResponse;
import com.abrik.bank_cards.bank_cards.service.admin.CardAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequestMapping("/api/admin/cards")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CardAdminController {
    private final CardAdminService adminCardService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CardResponse create(@RequestBody @Valid CreateCardRequest request) {
        return adminCardService.createCard(request);
    }

    @GetMapping
    public PageResponse<CardResponse> list(@RequestParam(required = false) CardStatus status,
                                           @RequestParam(required = false) String search,
                                           @ParameterObject Pageable pageable) {
        return adminCardService.listAllCards(status, search, pageable);
    }

    @GetMapping("/{cardId}")
    public CardResponse get(@PathVariable UUID cardId) {
        return adminCardService.getCard(cardId);
    }

    @PostMapping("/{cardId}/block")
    public RequestStatusResponse block(@PathVariable UUID cardId) {
        return adminCardService.blockCard(cardId);
    }

    @PostMapping("/{cardId}/activate")
    public RequestStatusResponse activate(@PathVariable UUID cardId) {
        return adminCardService.activateCard(cardId);
    }

    @DeleteMapping("/{cardId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID cardId) {
        adminCardService.deleteCard(cardId);
    }
}
