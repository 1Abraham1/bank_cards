package com.abrik.bank_cards.bank_cards.util;

import com.abrik.bank_cards.bank_cards.dto.card.CardStatus;
import com.abrik.bank_cards.bank_cards.dto.transfer.Bounds;
import com.abrik.bank_cards.bank_cards.dto.transfer.TransferResponse;
import com.abrik.bank_cards.bank_cards.entity.Card;
import com.abrik.bank_cards.bank_cards.entity.Transfer;
import com.abrik.bank_cards.bank_cards.exception.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class TransferUtil {

    public void ensureCardActive(Card c, String label) {
        if (c.getStatus() != CardStatus.ACTIVE)
            throw new BadRequestException("Card '" + label + "' is not ACTIVE");
    }

    public TransferResponse map(Transfer t) {
        return new TransferResponse(
                t.getId(),
                t.getUserId(),
                t.getFromCardId(),
                t.getToCardId(),
                t.getAmount(),
                t.getCurrency(),
                t.getStatus(),
                t.getMessage(),
                t.getCreatedAt()
        );
    }

    public Bounds normalizeBounds(Instant from, Instant to) {
        if (from == null && to == null) {
            Instant toDef = Instant.now();
            Instant fromDef = toDef.minus(90, ChronoUnit.DAYS);
            return new Bounds(fromDef, toDef);
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("'from' must be <= 'to'");
        }
        return new Bounds(from, to);
    }

    public Pageable ensureSort(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        return pageable;
    }
}
