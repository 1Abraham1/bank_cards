package com.abrik.bank_cards.bank_cards.service;

import com.abrik.bank_cards.bank_cards.dto.transfer.Bounds;
import com.abrik.bank_cards.bank_cards.dto.transfer.CreateTransferRequest;
import com.abrik.bank_cards.bank_cards.dto.transfer.TransferResponse;
import com.abrik.bank_cards.bank_cards.dto.transfer.TransferStatus;
import com.abrik.bank_cards.bank_cards.entity.Card;
import com.abrik.bank_cards.bank_cards.entity.Transfer;
import com.abrik.bank_cards.bank_cards.exception.BadRequestException;
import com.abrik.bank_cards.bank_cards.exception.NotFoundException;
import com.abrik.bank_cards.bank_cards.repository.CardRepository;
import com.abrik.bank_cards.bank_cards.repository.TransferRepository;
import com.abrik.bank_cards.bank_cards.service.user.TransferService;
import com.abrik.bank_cards.bank_cards.util.TransferUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock CardRepository cardRepository;
    @Mock TransferRepository transferRepository;
    @Mock TransferUtil transferUtil;

    @InjectMocks
    TransferService transferService;

    private static Card card(UUID id, Long userId, String currency, BigDecimal balance, boolean active) {
        Card c = new Card();
        c.setId(id);
        c.setUserId(userId);
        c.setCurrency(currency);
        c.setBalance(balance);
        // emulation of ensureCardActive: пусть ACTIVE — true, иначе EXPIRED/BLOCKED проверяются в util
        return c;
    }

    private static CreateTransferRequest req(UUID from, UUID to, String amount, String curr, String msg) {
        CreateTransferRequest r = new CreateTransferRequest();
        r.setFromCardId(from);
        r.setToCardId(to);
        r.setAmount(new BigDecimal(amount));
        r.setCurrency(curr);
        r.setMessage(msg);
        return r;
    }

    //  ИДЕМПОТЕНТНОСТЬ 

    @Test
    @DisplayName("transferOwnCards: повтор по Idempotency-Key возвращает уже сохранённый трансфер без повторного списания")
    void transfer_idempotency_hit() {
        Long userId = 10L;
        String idem = UUID.randomUUID().toString();

        Transfer saved = new Transfer();
        saved.setId(UUID.randomUUID());
        saved.setUserId(userId);
        saved.setAmount(new BigDecimal("100.00"));
        saved.setCurrency("USD");
        saved.setStatus(TransferStatus.COMPLETED);

        when(transferRepository.findByUserIdAndIdempotencyKey(userId, idem))
                .thenReturn(Optional.of(saved));

        TransferResponse mapped = new TransferResponse();
        mapped.setId(saved.getId());
        when(transferUtil.map(saved)).thenReturn(mapped);

        TransferResponse resp = transferService.transferOwnCards(
                userId, req(UUID.randomUUID(), UUID.randomUUID(), "100.00", "USD", "x"), idem);

        assertThat(resp.getId()).isEqualTo(saved.getId());

        verifyNoInteractions(cardRepository);      // ни блокировок, ни saveAll
        verify(transferRepository, never()).saveAndFlush(any());
    }

    //  ВАЛИДАЦИИ ВХОДА 

    @Test
    @DisplayName("transferOwnCards: fromCardId == toCardId → 400 BadRequest")
    void transfer_same_cards() {
        Long userId = 11L;
        UUID id = UUID.randomUUID();

        assertThrows(BadRequestException.class, () ->
                transferService.transferOwnCards(userId, req(id, id, "10.00", "USD", null), "k"));
    }

    @Test
    @DisplayName("transferOwnCards: amount <= 0 → 400 BadRequest")
    void transfer_non_positive_amount() {
        Long userId = 12L;
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();

        assertThrows(BadRequestException.class, () ->
                transferService.transferOwnCards(userId, req(a, b, "0", "USD", null), "k"));
        assertThrows(BadRequestException.class, () ->
                transferService.transferOwnCards(userId, req(a, b, "-1", "USD", null), "k"));
    }

    //  NOT FOUND при блокировках 

    @Test
    @DisplayName("transferOwnCards: первая карта не найдена (lock) → 404 NotFound")
    void transfer_first_lock_not_found() {
        Long userId = 13L;
        UUID from = UUID.randomUUID(), to = UUID.randomUUID();

        when(transferRepository.findByUserIdAndIdempotencyKey(anyLong(), anyString()))
                .thenReturn(Optional.empty());

        when(cardRepository.lockByIdAndUserId(any(UUID.class), eq(userId))).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                transferService.transferOwnCards(userId, req(from, to, "10.00", "USD", null), "k"));
    }

    @Test
    @DisplayName("transferOwnCards: вторая карта не найдена (lock) → 404 NotFound")
    void transfer_second_lock_not_found() {
        Long userId = 14L;

        UUID from = UUID.randomUUID(), to = UUID.randomUUID();

        when(transferRepository.findByUserIdAndIdempotencyKey(anyLong(), anyString()))
                .thenReturn(Optional.empty());

        // first найден...
        when(cardRepository.lockByIdAndUserId(any(UUID.class), eq(userId)))
                .thenReturn(Optional.of(card(UUID.randomUUID(), userId, "USD", new BigDecimal("100.00"), true)))
                .thenReturn(Optional.empty()); // …а second — нет

        assertThrows(NotFoundException.class, () ->
                transferService.transferOwnCards(userId, req(from, to, "10.00", "USD", null), "k"));
    }

    //  БИЗНЕС-ВАЛИДАЦИИ 

    @Test
    @DisplayName("transferOwnCards: mismatch валют → 400 BadRequest")
    void transfer_currency_mismatch() {
        Long userId = 15L;
        UUID from = UUID.randomUUID(), to = UUID.randomUUID();

        when(transferRepository.findByUserIdAndIdempotencyKey(anyLong(), anyString()))
                .thenReturn(Optional.empty());

        Card fromCard = card(from, userId, "USD", new BigDecimal("100.00"), true);
        Card toCard   = card(to,   userId, "EUR", new BigDecimal("5.00"),   true);

        when(cardRepository.lockByIdAndUserId(any(UUID.class), eq(userId)))
                .thenReturn(Optional.of(fromCard))
                .thenReturn(Optional.of(toCard));

        // ensureCardActive просто не кидает
        doNothing().when(transferUtil).ensureCardActive(any(Card.class), anyString());

        assertThrows(BadRequestException.class, () ->
                transferService.transferOwnCards(userId, req(from, to, "10.00", "USD", null), "k"));

        verify(cardRepository, never()).saveAll(anyCollection());
        verify(transferRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("transferOwnCards: недостаточно средств → 400 BadRequest")
    void transfer_insufficient_funds() {
        Long userId = 16L;
        UUID from = UUID.randomUUID(), to = UUID.randomUUID();

        when(transferRepository.findByUserIdAndIdempotencyKey(anyLong(), anyString()))
                .thenReturn(Optional.empty());

        Card fromCard = card(from, userId, "USD", new BigDecimal("9.99"), true);
        Card toCard   = card(to,   userId, "USD", new BigDecimal("0.01"), true);

        when(cardRepository.lockByIdAndUserId(any(UUID.class), eq(userId)))
                .thenReturn(Optional.of(fromCard))
                .thenReturn(Optional.of(toCard));

        doNothing().when(transferUtil).ensureCardActive(any(Card.class), anyString());

        assertThrows(BadRequestException.class, () ->
                transferService.transferOwnCards(userId, req(from, to, "10.00", "USD", null), "k"));

        verify(cardRepository, never()).saveAll(anyCollection());
        verify(transferRepository, never()).saveAndFlush(any());
    }

    //  УСПЕХ (включая округление и сохранение) 

    @Test
    @DisplayName("transferOwnCards: успех — округление HALF_UP до 2 знаков, обновление балансов, сохранение трансфера")
    void transfer_success_with_rounding_and_persistence() {
        Long userId = 17L;
        // Выберем UUID так, чтобы порядок блокировок мог меняться — это не влияет на корректность
        UUID from = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000000");
        UUID to   = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000000");

        when(transferRepository.findByUserIdAndIdempotencyKey(anyLong(), anyString()))
                .thenReturn(Optional.empty());

        Card fromCard = card(from, userId, "USD", new BigDecimal("1000.00"), true);
        Card toCard   = card(to,   userId, "USD", new BigDecimal("0.00"),   true);

        when(cardRepository.lockByIdAndUserId(eq(from), eq(userId))).thenReturn(Optional.of(fromCard));
        when(cardRepository.lockByIdAndUserId(eq(to),   eq(userId))).thenReturn(Optional.of(toCard));
        doNothing().when(transferUtil).ensureCardActive(any(Card.class), anyString());

        // Сохраняемые карты
        ArgumentCaptor<Collection<Card>> saveAllCaptor = ArgumentCaptor.forClass(Collection.class);
        when(cardRepository.saveAll(anyList())).then(returnsFirstArg());

        // Сохраняемый трансфер
        ArgumentCaptor<Transfer> transferCaptor = ArgumentCaptor.forClass(Transfer.class);
        when(transferRepository.saveAndFlush(transferCaptor.capture()))
                .thenAnswer(inv -> inv.getArgument(0));

        TransferResponse mapped = new TransferResponse();
        mapped.setStatus(TransferStatus.COMPLETED);
        when(transferUtil.map(any(Transfer.class))).thenReturn(mapped);

        // amount с 3 знаками после запятой — должен округлиться до 2 по HALF_UP
        CreateTransferRequest r = req(from, to, "100.005", "USD", "rounding");
        String idem = UUID.randomUUID().toString();

        TransferResponse resp = transferService.transferOwnCards(userId, r, idem);

        // Балансы обновились правильно
        verify(cardRepository).saveAll(saveAllCaptor.capture());
        Collection<Card> savedCards = saveAllCaptor.getValue();
        assertThat(savedCards).hasSize(2);

        // найдём конкретные карты
        Card savedFrom = savedCards.stream().filter(c -> c.getId().equals(from)).findFirst().orElseThrow();
        Card savedTo   = savedCards.stream().filter(c -> c.getId().equals(to)).findFirst().orElseThrow();

        assertThat(savedFrom.getBalance()).isEqualByComparingTo("899.99"); // 1000 - 100.01
        assertThat(savedTo.getBalance()).isEqualByComparingTo("100.01");   // 0 + 100.01

        // Проверим поля сохранённого Transfer
        Transfer persisted = transferCaptor.getValue();
        assertThat(persisted.getUserId()).isEqualTo(userId);
        assertThat(persisted.getFromCardId()).isEqualTo(from);
        assertThat(persisted.getToCardId()).isEqualTo(to);
        assertThat(persisted.getAmount()).isEqualByComparingTo("100.01"); // округлено
        assertThat(persisted.getCurrency()).isEqualTo("USD");
        assertThat(persisted.getStatus()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(persisted.getIdempotencyKey()).isEqualTo(idem);
        assertThat(persisted.getCreatedAt()).isNotNull();

        // map вызван и ответ вернулся ура
        verify(transferUtil).map(persisted);
        assertThat(resp.getStatus()).isEqualTo(TransferStatus.COMPLETED);
    }

    // listOwn

    @Test
    @DisplayName("listOwn: нормализация интервалов, ensureSort и маппинг страницы")
    void listOwn_mapsPage() {
        Long userId = 20L;
        TransferStatus status = TransferStatus.COMPLETED;
        Instant from = Instant.now().minusSeconds(3600);
        Instant to   = Instant.now().plusSeconds(3600);
        UUID cardId  = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

         var bounds = new Bounds(from, to);
         when(transferUtil.normalizeBounds(from, to)).thenReturn(bounds);
        Pageable sorted = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        when(transferUtil.ensureSort(pageable)).thenReturn(sorted);

        Transfer entity = new Transfer();
        entity.setId(UUID.randomUUID());
        entity.setStatus(TransferStatus.COMPLETED);

        Page<Transfer> repoPage = new PageImpl<>(List.of(entity), sorted, 1);
        when(transferRepository.searchUser(eq(userId), eq(status), any(), any(), eq(cardId), eq(sorted)))
                .thenReturn(repoPage);

        TransferResponse mapped = new TransferResponse();
        mapped.setId(entity.getId());
        mapped.setStatus(TransferStatus.COMPLETED);
        when(transferUtil.map(entity)).thenReturn(mapped);

        Page<TransferResponse> page = transferService.listOwn(userId, status, from, to, cardId, pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().getId()).isEqualTo(entity.getId());
        assertThat(page.getContent().getFirst().getStatus()).isEqualTo(TransferStatus.COMPLETED);

        verify(transferUtil).normalizeBounds(from, to);
        verify(transferUtil).ensureSort(pageable);
        verify(transferRepository).searchUser(userId, status, bounds.from(), bounds.to(), cardId, sorted);
        verify(transferUtil).map(entity);
    }
}
