package com.abrik.bank_cards.bank_cards.service.user;

import com.abrik.bank_cards.bank_cards.dto.card.*;
import com.abrik.bank_cards.bank_cards.dto.common.PageResponse;
import com.abrik.bank_cards.bank_cards.entity.Card;
import com.abrik.bank_cards.bank_cards.exception.BadRequestException;
import com.abrik.bank_cards.bank_cards.exception.NotFoundException;
import com.abrik.bank_cards.bank_cards.repository.CardRepository;
import com.abrik.bank_cards.bank_cards.util.CardUtil;
import com.abrik.bank_cards.bank_cards.util.PanCryptoUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardService {
    private final CardRepository cardRepository;
    private final CardUtil cardUtil;
    private final PanCryptoUtil panCryptoUtil;
    private final Clock clock = Clock.systemUTC();

    public CardResponse createCard(Long userId, CreateCardRequest request) {
        cardUtil.validateExpiry(request.getExpiryMonth(), request.getExpiryYear());

        String pan = request.getPan();
        cardUtil.validatePan(pan);

        String last4 = pan.substring(pan.length() - 4);
        String panEncrypted = panCryptoUtil.encrypt(pan);

        Card card = new Card();
        card.setId(UUID.randomUUID());
        card.setUserId(userId);
        card.setLast4(last4);
        card.setPanEncrypted(panEncrypted);
        card.setExpiryMonth(request.getExpiryMonth());
        card.setExpiryYear(request.getExpiryYear());
        card.setStatus(CardStatus.ACTIVE);
        card.setBalance(request.getBalance());
        card.setCurrency(request.getCurrency());
        card.setRequestedBlockAt(null);

        Instant now = Instant.now();
        card.setCreatedAt(now);
        card.setUpdatedAt(now);

        Card saved = cardRepository.save(card);
        return cardUtil.toResponse(saved);
    }

    /** для юзера */
    public PageResponse<CardResponse> listMyCards(Long userId,
                                                  CardStatus status,
                                                  String search,
                                                  Pageable pageable) {
        Page<Card> page = findCards(userId, status, search, pageable);
        return cardUtil.toCardPageResponse(page);
    }

    private Page<Card> findCards(Long userId, CardStatus status, String search, Pageable pageable) {
        String q = cardUtil.normalizeQuery(search);
        String digits = cardUtil.extractDigits(search);

        String qPattern = cardUtil.isBlank(q) ? null : "%" + q + "%";
        String last4Pattern = cardUtil.isBlank(digits) ? null : "%" + digits + "%";

        return cardRepository.search(userId, status, qPattern, last4Pattern, pageable);
    }

    @Transactional
    public StatusResponse requestBlock(Long userId, UUID cardId) {
        Card card = cardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new NotFoundException("Card not found"));

        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new BadRequestException("Card is already BLOCKED");
        }
        if (card.getStatus() == CardStatus.EXPIRED) {
            throw new BadRequestException("Card is EXPIRED and cannot be blocked");
        }

        if (card.getRequestedBlockAt() == null) {
            Instant now = Instant.now(clock);
            card.setRequestedBlockAt(now);
            card.setUpdatedAt(now);
            cardRepository.save(card);
        }

        return new StatusResponse(
                card.getId(),
                card.getRequestedBlockAt(),
                card.getStatus()
        );
    }

    public CardResponse getMyCard(Long userId, UUID cardId) {
        Card card;

        card = cardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new NotFoundException("Card not found"));

        return cardUtil.toResponse(card);
    }

    @Transactional
    public void deleteCard(Long userId, UUID cardId) {
        cardRepository.deleteByIdAndUserId(cardId, userId);
    }

    public CardResponse updateMyCard(Long userId, UUID cardId, UpdateCardRequest request) {
        Card card = cardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new NotFoundException("Card not found"));

        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new BadRequestException("Card is BLOCKED and cannot be changed");
        }
        if (card.getStatus() == CardStatus.EXPIRED) {
            throw new BadRequestException("Card is EXPIRED and cannot be changed");
        }

        String pan = request.getPan();
        String panEncrypted = panCryptoUtil.encrypt(pan);
        String last4 = pan.substring(pan.length() - 4);
        card.setUpdatedAt(Instant.now(clock));
        card.setPanEncrypted(panEncrypted);
        card.setLast4(last4);
        card.setExpiryMonth(request.getExpiryMonth());
        card.setExpiryYear(request.getExpiryYear());

        Card saved = cardRepository.save(card);

        return cardUtil.toResponse(saved);
    }
}
