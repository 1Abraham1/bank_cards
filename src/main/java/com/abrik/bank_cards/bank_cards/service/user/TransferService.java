package com.abrik.bank_cards.bank_cards.service.user;

import com.abrik.bank_cards.bank_cards.dto.transfer.CreateTransferRequest;
import com.abrik.bank_cards.bank_cards.dto.transfer.TransferResponse;
import com.abrik.bank_cards.bank_cards.dto.transfer.TransferStatus;
import com.abrik.bank_cards.bank_cards.entity.Card;
import com.abrik.bank_cards.bank_cards.entity.Transfer;
import com.abrik.bank_cards.bank_cards.exception.BadRequestException;
import com.abrik.bank_cards.bank_cards.exception.NotFoundException;
import com.abrik.bank_cards.bank_cards.repository.CardRepository;
import com.abrik.bank_cards.bank_cards.repository.TransferRepository;
import com.abrik.bank_cards.bank_cards.util.TransferUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferService {
    private final CardRepository cardRepository;
    private final TransferRepository transferRepository;
    private final TransferUtil transferUtil;
    private final Clock clock = Clock.systemUTC();

    @Transactional
    public TransferResponse transferOwnCards(Long userId, CreateTransferRequest request, String idempotencyKey) {

        var existing = transferRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
        if (existing.isPresent()) {
            var t = existing.get();
            return transferUtil.map(t);
        }

        if (request.getFromCardId().equals(request.getToCardId()))
            throw new BadRequestException("fromCardId must differ from toCardId");

        // Жёстко держим scale = 2 (для банковских карт)
        BigDecimal amount = request.getAmount().setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new BadRequestException("Amount must be > 0.00");

        // Детектируем порядок блокировки для избежания дедлоков
        UUID firstId = ( request.getFromCardId().toString()
                .compareTo(request.getToCardId().toString()) < 0 )
                ? request.getFromCardId() : request.getToCardId();

        UUID secondId = firstId.equals(request.getFromCardId())
                ? request.getToCardId() : request.getFromCardId();

        // Блокируем обе карты пользователю
        Card first = cardRepository.lockByIdAndUserId(firstId, userId)
                .orElseThrow(() -> new NotFoundException("Card not found"));
        Card second = cardRepository.lockByIdAndUserId(secondId, userId)
                .orElseThrow(() -> new NotFoundException("Card not found"));

        // Мапим обратно на from/to после блокировок в нужном порядке
        Card from = first.getId().equals(request.getFromCardId()) ? first : second;
        Card to   = first.getId().equals(request.getToCardId())   ? first : second;

        // Бизнес-валидации статусов и валют
        transferUtil.ensureCardActive(from, "from");
        transferUtil.ensureCardActive(to,   "to");

        String currency = request.getCurrency();
        if (!currency.equals(from.getCurrency()) || !currency.equals(to.getCurrency()))
            throw new BadRequestException("Currency mismatch");

        // Проверка баланса
        if (from.getBalance().compareTo(amount) < 0)
            throw new BadRequestException("Insufficient funds");

        // Списание/зачисление (атомарно в рамках транзакции)
        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));

        // Зафиксируем изменения карт
        cardRepository.saveAll(List.of(from, to));

        // 9) Запишем трансфер
        var transfer = new Transfer();
        transfer.setId(UUID.randomUUID());
        transfer.setUserId(userId);
        transfer.setFromCardId(from.getId());
        transfer.setToCardId(to.getId());
        transfer.setAmount(amount);
        transfer.setCurrency(currency);
        transfer.setStatus(TransferStatus.COMPLETED);
        transfer.setMessage(request.getMessage());
        transfer.setIdempotencyKey(idempotencyKey);
        transfer.setCreatedAt(Instant.now(clock));

        transfer = transferRepository.saveAndFlush(transfer);

        return transferUtil.map(transfer);
    }

    public Page<TransferResponse> listOwn(Long userId,
                                               TransferStatus status,
                                               Instant from,
                                               Instant to,
                                               UUID cardId,
                                               Pageable pageable) {
        var bounds = transferUtil.normalizeBounds(from, to);
        Pageable sortedPageable = transferUtil.ensureSort(pageable);
        Page<Transfer> page = transferRepository.searchUser(
                userId, status, bounds.from(), bounds.to(), cardId, sortedPageable);
        return page.map(transferUtil::map);
    }
}
