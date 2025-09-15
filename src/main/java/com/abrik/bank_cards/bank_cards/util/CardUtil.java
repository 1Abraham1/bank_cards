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
import java.util.regex.Pattern;

@Component
public class CardUtil {
    private final Clock clock = Clock.systemUTC();
    private final PanCryptoUtil panCryptoUtil;

    public CardUtil(PanCryptoUtil panCryptoUtil) {
        this.panCryptoUtil = panCryptoUtil;
    }

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
                panCryptoUtil.decrypt(card.getPanEncrypted()),
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

    private static final Pattern PAN_PATTERN = Pattern.compile("^\\d{13,19}$");

    /**
     * Валидирует PAN:
     * 1) обязательность
     * 2) нормализация (удаление пробелов/дефисов)
     * 3) проверка по регулярке (13–19 цифр)
     * 4) проверка по модулю Луна
     * Возвращает нормализованный PAN (без пробелов и дефисов).
     */
    public String validatePan(String rawPan) {
        if (rawPan == null || rawPan.isBlank()) {
            throw new BadRequestException("PAN обязателен");
        }

        // 1) Нормализация: убираем пробелы и дефисы
        String pan = rawPan.replaceAll("[\\s-]", "");

        // 2) Регулярка: только цифры, длина 13–19
        if (!PAN_PATTERN.matcher(pan).matches()) {
            throw new BadRequestException("PAN должен состоять из 13–19 цифр");
        }

        return pan;
    }
}
