package com.abrik.bank_cards.bank_cards.service.admin;

import com.abrik.bank_cards.bank_cards.dto.card.CardResponse;
import com.abrik.bank_cards.bank_cards.dto.card.CardStatus;
import com.abrik.bank_cards.bank_cards.dto.card.CreateCardRequest;
import com.abrik.bank_cards.bank_cards.dto.card.RequestStatusResponse;
import com.abrik.bank_cards.bank_cards.dto.common.PageResponse;
import com.abrik.bank_cards.bank_cards.entity.Card;
import com.abrik.bank_cards.bank_cards.entity.User;
import com.abrik.bank_cards.bank_cards.exception.BadRequestException;
import com.abrik.bank_cards.bank_cards.exception.NotFoundException;
import com.abrik.bank_cards.bank_cards.repository.CardRepository;
import com.abrik.bank_cards.bank_cards.repository.UserRepository;
import com.abrik.bank_cards.bank_cards.service.user.CardService;
import com.abrik.bank_cards.bank_cards.util.CardUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminCardService {
    private final CardService cardService;
    private final CardUtil cardUtil;
    private final UserRepository userRepository;
    private final CardRepository cardRepository;

    public CardResponse createCard(CreateCardRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found"));
        return cardService.createCard(user.getId(), request);
    }

    public PageResponse<CardResponse> listAllCards(CardStatus status,
                                                   String search,
                                                   Pageable pageable) {
        Page<Card> page = findCards(status, search, pageable);
        return cardUtil.toCardPageResponse(page);
    }

    private Page<Card> findCards(CardStatus status, String search, Pageable pageable) {
        String q = cardUtil.normalizeQuery(search);
        String digits = cardUtil.extractDigits(search);

        String qPattern = cardUtil.isBlank(q) ? null : "%" + q + "%";
        String last4Pattern = cardUtil.isBlank(digits) ? null : "%" + digits + "%";

        return cardRepository.search(null, status, qPattern, last4Pattern, pageable);
    }

    public CardResponse getCard(UUID cardId) {
        Card card;

        card = cardRepository.findById(cardId)
                .orElseThrow(() -> new NotFoundException("Card not found"));

        return cardUtil.toResponse(card);
    }

    @Transactional
    public void deleteCard(UUID cardId) {
        cardRepository.deleteById(cardId);
    }

    @Transactional
    public RequestStatusResponse blockCard(UUID cardId) {
        return changeStatus(cardId, CardStatus.BLOCKED);
    }

    @Transactional
    public RequestStatusResponse activateCard(UUID cardId) {
        return changeStatus(cardId, CardStatus.ACTIVE);
    }

    private RequestStatusResponse changeStatus(UUID cardId, CardStatus newStatus) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new NotFoundException("Card not found"));

        if (card.getStatus() == newStatus) {
            throw new BadRequestException("Card is already " + newStatus);
        }
        if (card.getStatus() == CardStatus.EXPIRED) {
            throw new BadRequestException("Card is EXPIRED and cannot be " + newStatus);
        }

        if (newStatus == CardStatus.ACTIVE)
            card.setRequestedBlockAt(null);

        card.setStatus(newStatus);
        cardRepository.saveAndFlush(card);

        return new RequestStatusResponse(
                card.getId(),
                card.getRequestedBlockAt(),
                card.getStatus()
        );
    }
}
