package com.abrik.bank_cards.bank_cards.util;

import com.abrik.bank_cards.bank_cards.dto.card.CardResponse;
import com.abrik.bank_cards.bank_cards.dto.common.PageResponse;
import com.abrik.bank_cards.bank_cards.entity.Card;
import com.abrik.bank_cards.bank_cards.exception.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.YearMonth;
import java.util.List;

@Component
public class CardUtil {
    private final Clock clock = Clock.systemUTC();

    public void validateExpiry(short month, short year) {
        if (month < 1 || month > 12) {
            throw new BadRequestException("Месяц истечения должен быть в диапазоне 1–12");
        }
        YearMonth now = YearMonth.now(clock);
        YearMonth exp = YearMonth.of(year, month);
        if (exp.isBefore(now)) {
            throw new BadRequestException("Срок действия карты уже истёк");
        }
    }

    public boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    public String normalizeQuery(String search) {
        if (search == null) return null;
        String trimmed = search.trim().toLowerCase();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public String extractDigits(String search) {
        if (search == null) return null;
        String digits = search.replaceAll("\\D+", "");
        return digits.isEmpty() ? null : digits;
    }

    public PageResponse<CardResponse> toCardPageResponse(Page<Card> page) {
        List<CardResponse> content = page.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.of(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious()
        );
    }

    public CardResponse toResponse(Card card) {
        if (card == null) {
            return null;
        }

        return new CardResponse(
                card.getId(),
                card.getUserId(),
                maskCardNumber(card.getLast4()),
                card.getExpiryMonth(),
                card.getExpiryYear(),
                card.getStatus(),
                card.getBalance(),
                card.getCurrency(),
                card.getRequestedBlockAt() != null,
                card.getCreatedAt(),
                card.getUpdatedAt()
        );
    }

    private static String maskCardNumber(String last4) {
        if (last4 == null || last4.isBlank()) {
            return "**** **** **** ****";
        }
        return "**** **** **** " + last4;
    }
}
