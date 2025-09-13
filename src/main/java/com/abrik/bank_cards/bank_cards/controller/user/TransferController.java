package com.abrik.bank_cards.bank_cards.controller.user;

import com.abrik.bank_cards.bank_cards.dto.transfer.CreateTransferRequest;
import com.abrik.bank_cards.bank_cards.dto.transfer.TransferResponse;
import com.abrik.bank_cards.bank_cards.dto.transfer.TransferStatus;
import com.abrik.bank_cards.bank_cards.security.MyUserDetails;
import com.abrik.bank_cards.bank_cards.service.user.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {
    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<TransferResponse> create(
            @AuthenticationPrincipal MyUserDetails myUserDetails,
            @Valid @RequestBody CreateTransferRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idemKeyHeader) {

        String idempotencyKey = (idemKeyHeader == null || idemKeyHeader.isBlank())
                ? UUID.randomUUID().toString()
                : idemKeyHeader.trim();

        TransferResponse resp = transferService.transferOwnCards(myUserDetails.getUserId(), request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    // 1) Список СВОИХ переводов
    @GetMapping
    public Page<TransferResponse> listOwn(
            @AuthenticationPrincipal MyUserDetails myUserDetails,
            @RequestParam(required = false) TransferStatus status,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) UUID cardId,
            Pageable pageable
    ) {
        return transferService.listOwn(myUserDetails.getUserId(), status, from, to, cardId, pageable);
    }
}
