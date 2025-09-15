package com.abrik.bank_cards.bank_cards.service;

import com.abrik.bank_cards.bank_cards.dto.card.*;
import com.abrik.bank_cards.bank_cards.dto.common.PageResponse;
import com.abrik.bank_cards.bank_cards.entity.Card;
import com.abrik.bank_cards.bank_cards.exception.BadRequestException;
import com.abrik.bank_cards.bank_cards.exception.NotFoundException;
import com.abrik.bank_cards.bank_cards.repository.CardRepository;
import com.abrik.bank_cards.bank_cards.service.user.CardService;
import com.abrik.bank_cards.bank_cards.util.CardUtil;
import com.abrik.bank_cards.bank_cards.util.PanCryptoUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock private CardRepository cardRepository;
    @Mock private CardUtil cardUtil;
    @Mock private PanCryptoUtil panCryptoUtil;

    @InjectMocks private CardService cardService;

    private Card cardWithStatus(Long userId, CardStatus status) {
        Card c = new Card();
        c.setId(UUID.randomUUID());
        c.setUserId(userId);
        c.setLast4("1234");
        c.setPanEncrypted("enc");
        c.setExpiryMonth((short) 9);
        c.setExpiryYear((short) 2030);
        c.setCurrency("USD");
        c.setBalance(new BigDecimal("100.00"));
        c.setStatus(status);
        c.setCreatedAt(Instant.now());
        c.setUpdatedAt(Instant.now());
        return c;
    }

     // createCard 

    @Test
    @DisplayName("createCard: валидирует, шифрует PAN, сохраняет и маппит в CardResponse")
    void createCard_success() {
        Long userId = 7L;
        CreateCardRequest req = new CreateCardRequest();
        req.setUserId(userId);
        req.setPan("5212345678901234");
        req.setExpiryMonth((short) 9);
        req.setExpiryYear((short) 2030);
        req.setCurrency("USD");
        req.setBalance(new BigDecimal("123.45"));

        when(panCryptoUtil.encrypt("5212345678901234")).thenReturn("ENCRYPTED");

        ArgumentCaptor<Card> saveCaptor = ArgumentCaptor.forClass(Card.class);
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        CardResponse expectedResponse = new CardResponse();
        expectedResponse.setCurrency("USD");
        expectedResponse.setBalance(new BigDecimal("123.45"));
        expectedResponse.setStatus(CardStatus.ACTIVE);
        when(cardUtil.toResponse(any(Card.class))).thenReturn(expectedResponse);

        CardResponse resp = cardService.createCard(userId, req);

        // Проверяем, что репо получило корректно собранную сущность
        verify(cardRepository).save(saveCaptor.capture());
        Card saved = saveCaptor.getValue();

        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getPanEncrypted()).isEqualTo("ENCRYPTED");
        assertThat(saved.getLast4()).isEqualTo("1234");
        assertThat(saved.getExpiryMonth()).isEqualTo((short) 9);
        assertThat(saved.getExpiryYear()).isEqualTo((short) 2030);
        assertThat(saved.getCurrency()).isEqualTo("USD");
        assertThat(saved.getBalance()).isEqualByComparingTo("123.45");
        assertThat(saved.getStatus()).isEqualTo(CardStatus.ACTIVE);
        assertThat(saved.getRequestedBlockAt()).isNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        // Валидации вызваны
        verify(cardUtil).validateExpiry((short) 9, (short) 2030);
        verify(cardUtil).validatePan("5212345678901234");
        verify(panCryptoUtil).encrypt("5212345678901234");

        // Маппинг в ответ — через util
        verify(cardUtil).toResponse(saved);
        assertThat(resp.getCurrency()).isEqualTo("USD");
        assertThat(resp.getBalance()).isEqualByComparingTo("123.45");
        assertThat(resp.getStatus()).isEqualTo(CardStatus.ACTIVE);
    }

     // listMyCards / findCards 

    @Test
    @DisplayName("listMyCards: корректно собирает паттерны поиска и делегирует в util.toCardPageResponse")
    void listMyCards_buildsPatternsAndDelegates() {
        Long userId = 5L;
        CardStatus status = CardStatus.ACTIVE;
        String search = " my card **** 1234 ";

        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        Page<Card> page = new PageImpl<>(List.of(cardWithStatus(userId, CardStatus.ACTIVE)));

        // Поведение утилит поиска
        when(cardUtil.normalizeQuery(search)).thenReturn("my card");
        when(cardUtil.extractDigits(search)).thenReturn("1234");
        when(cardUtil.isBlank("my card")).thenReturn(false);
        when(cardUtil.isBlank("1234")).thenReturn(false);

        when(cardRepository.search(eq(userId), eq(status), eq("%my card%"), eq("%1234%"), eq(pageable)))
                .thenReturn(page);

        PageResponse<CardResponse> expected = new PageResponse<>();
        when(cardUtil.toCardPageResponse(page)).thenReturn(expected);

        PageResponse<CardResponse> resp = cardService.listMyCards(userId, status, search, pageable);

        assertThat(resp).isSameAs(expected);
        verify(cardRepository).search(userId, status, "%my card%", "%1234%", pageable);
        verify(cardUtil).toCardPageResponse(page);
    }

    @Test
    @DisplayName("listMyCards: пустой search → паттерны null")
    void listMyCards_blankPatterns() {
        Long userId = 5L;
        Pageable pageable = PageRequest.of(0, 5);

        when(cardUtil.normalizeQuery(null)).thenReturn(null);
        when(cardUtil.extractDigits(null)).thenReturn(null);
        when(cardUtil.isBlank(null)).thenReturn(true);

        Page<Card> page = Page.empty(pageable);
        when(cardRepository.search(eq(userId), isNull(), isNull(), isNull(), eq(pageable))).thenReturn(page);

        when(cardUtil.toCardPageResponse(page)).thenReturn(new PageResponse<>());

        cardService.listMyCards(userId, null, null, pageable);

        verify(cardRepository).search(userId, null, null, null, pageable);
    }

     // requestBlock 

    @Nested
    class RequestBlockTests {

        @Test
        @DisplayName("requestBlock: первый запрос — устанавливает requestedBlockAt и updatedAt, сохраняет, возвращает StatusResponse")
        void requestBlock_firstTime_setsTimestampAndSaves() {
            Long userId = 11L;
            Card c = cardWithStatus(userId, CardStatus.ACTIVE);
            c.setRequestedBlockAt(null);

            when(cardRepository.findByIdAndUserId(c.getId(), userId)).thenReturn(Optional.of(c));

            StatusResponse resp = cardService.requestBlock(userId, c.getId());

            // Сохраняли карту при первом запросе
            verify(cardRepository).save(any(Card.class));

            assertThat(resp.getCardId()).isEqualTo(c.getId());
            assertThat(resp.getStatus()).isEqualTo(CardStatus.ACTIVE);
            assertThat(resp.getRequestedBlockAt()).isNotNull();

            // В самой карте тоже должен быть выставлен requestedBlockAt и обновлён updatedAt
            assertThat(c.getRequestedBlockAt()).isNotNull();
            assertThat(c.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("requestBlock: второй запрос — ничего повторно не сохраняет")
        void requestBlock_secondTime_noSave() {
            Long userId = 12L;
            Card c = cardWithStatus(userId, CardStatus.ACTIVE);
            c.setRequestedBlockAt(Instant.now());

            when(cardRepository.findByIdAndUserId(c.getId(), userId)).thenReturn(Optional.of(c));

            StatusResponse resp = cardService.requestBlock(userId, c.getId());

            verify(cardRepository, never()).save(any(Card.class));
            assertThat(resp.getRequestedBlockAt()).isEqualTo(c.getRequestedBlockAt());
        }

        @Test
        @DisplayName("requestBlock: BLOCKED → BadRequestException")
        void requestBlock_blocked() {
            Long userId = 13L;
            Card c = cardWithStatus(userId, CardStatus.BLOCKED);
            when(cardRepository.findByIdAndUserId(c.getId(), userId)).thenReturn(Optional.of(c));

            assertThrows(BadRequestException.class, () -> cardService.requestBlock(userId, c.getId()));
            verify(cardRepository, never()).save(any());
        }

        @Test
        @DisplayName("requestBlock: EXPIRED → BadRequestException")
        void requestBlock_expired() {
            Long userId = 14L;
            Card c = cardWithStatus(userId, CardStatus.EXPIRED);
            when(cardRepository.findByIdAndUserId(c.getId(), userId)).thenReturn(Optional.of(c));

            assertThrows(BadRequestException.class, () -> cardService.requestBlock(userId, c.getId()));
            verify(cardRepository, never()).save(any());
        }

        @Test
        @DisplayName("requestBlock: карта не найдена → NotFoundException")
        void requestBlock_notFound() {
            Long userId = 15L;
            UUID cardId = UUID.randomUUID();
            when(cardRepository.findByIdAndUserId(cardId, userId)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> cardService.requestBlock(userId, cardId));
        }
    }

     // getMyCard 

    @Test
    @DisplayName("getMyCard: успех → toResponse(card)")
    void getMyCard_success() {
        Long userId = 21L;
        Card c = cardWithStatus(userId, CardStatus.ACTIVE);
        CardResponse expected = new CardResponse();
        expected.setId(c.getId());

        when(cardRepository.findByIdAndUserId(c.getId(), userId)).thenReturn(Optional.of(c));
        when(cardUtil.toResponse(c)).thenReturn(expected);

        CardResponse resp = cardService.getMyCard(userId, c.getId());

        assertThat(resp).isSameAs(expected);
        verify(cardRepository).findByIdAndUserId(c.getId(), userId);
        verify(cardUtil).toResponse(c);
    }

    @Test
    @DisplayName("getMyCard: NotFound → исключение")
    void getMyCard_notFound() {
        Long userId = 22L;
        UUID id = UUID.randomUUID();
        when(cardRepository.findByIdAndUserId(id, userId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> cardService.getMyCard(userId, id));
    }

     // deleteCard 

    @Test
    @DisplayName("deleteCard: делегирует в репозиторий по cardId+userId")
    void deleteCard_delegates() {
        Long userId = 31L;
        UUID id = UUID.randomUUID();

        cardService.deleteCard(userId, id);

        verify(cardRepository).deleteByIdAndUserId(id, userId);
    }

     // updateMyCard 

    @Test
    @DisplayName("updateMyCard: успех — шифрует PAN, правит last4/expiry/updatedAt, сохраняет и toResponse")
    void updateMyCard_success() {
        Long userId = 41L;
        Card existing = cardWithStatus(userId, CardStatus.ACTIVE);
        existing.setLast4("0000");
        existing.setPanEncrypted("OLD");
        Instant oldUpdatedAt = existing.getUpdatedAt();

        UpdateCardRequest req = new UpdateCardRequest();
        req.setPan("5212345678909999");
        req.setExpiryMonth((short) 10);
        req.setExpiryYear((short) 2031);

        when(cardRepository.findByIdAndUserId(existing.getId(), userId)).thenReturn(Optional.of(existing));
        when(panCryptoUtil.encrypt("5212345678909999")).thenReturn("NEW_ENC");
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        CardResponse expected = new CardResponse();
        expected.setId(existing.getId());
        expected.setExpiryMonth((short) 10);
        expected.setExpiryYear((short) 2031);
        when(cardUtil.toResponse(any(Card.class))).thenReturn(expected);

        CardResponse resp = cardService.updateMyCard(userId, existing.getId(), req);

        ArgumentCaptor<Card> captor = ArgumentCaptor.forClass(Card.class);
        verify(cardRepository).save(captor.capture());
        Card saved = captor.getValue();

        assertThat(saved.getPanEncrypted()).isEqualTo("NEW_ENC");
        assertThat(saved.getLast4()).isEqualTo("9999");
        assertThat(saved.getExpiryMonth()).isEqualTo((short) 10);
        assertThat(saved.getExpiryYear()).isEqualTo((short) 2031);
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotEqualTo(oldUpdatedAt);

        verify(cardUtil).toResponse(saved);
        assertThat(resp.getId()).isEqualTo(existing.getId());
        assertThat(resp.getExpiryMonth()).isEqualTo((short) 10);
        assertThat(resp.getExpiryYear()).isEqualTo((short) 2031);
    }

    @Test
    @DisplayName("updateMyCard: BLOCKED → BadRequestException")
    void updateMyCard_blocked() {
        Long userId = 42L;
        Card c = cardWithStatus(userId, CardStatus.BLOCKED);
        when(cardRepository.findByIdAndUserId(c.getId(), userId)).thenReturn(Optional.of(c));

        UpdateCardRequest req = new UpdateCardRequest();
        req.setPan("5212345678901234");
        req.setExpiryMonth((short) 9);
        req.setExpiryYear((short) 2030);

        assertThrows(BadRequestException.class, () -> cardService.updateMyCard(userId, c.getId(), req));
        verify(cardRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateMyCard: EXPIRED → BadRequestException")
    void updateMyCard_expired() {
        Long userId = 43L;
        Card c = cardWithStatus(userId, CardStatus.EXPIRED);
        when(cardRepository.findByIdAndUserId(c.getId(), userId)).thenReturn(Optional.of(c));

        UpdateCardRequest req = new UpdateCardRequest();
        req.setPan("5212345678901234");
        req.setExpiryMonth((short) 9);
        req.setExpiryYear((short) 2030);

        assertThrows(BadRequestException.class, () -> cardService.updateMyCard(userId, c.getId(), req));
        verify(cardRepository, never()).save(any());
    }
}
