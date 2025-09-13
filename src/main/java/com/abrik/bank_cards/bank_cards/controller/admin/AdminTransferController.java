package com.abrik.bank_cards.bank_cards.controller.admin;

import com.abrik.bank_cards.bank_cards.dto.transfer.TransferResponse;
import com.abrik.bank_cards.bank_cards.dto.transfer.TransferStatus;
import com.abrik.bank_cards.bank_cards.service.admin.AdminTransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/transfers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminTransferController {
    private final AdminTransferService adminTransferService;

    @GetMapping
    public Page<TransferResponse> listAll(
            @RequestParam(required = false) TransferStatus status,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) UUID cardId,
            Pageable pageable
    ) {
        return adminTransferService.listAll(status, from, to, cardId, pageable);
    }
}
