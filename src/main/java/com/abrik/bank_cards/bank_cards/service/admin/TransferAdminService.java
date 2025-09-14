package com.abrik.bank_cards.bank_cards.service.admin;

import com.abrik.bank_cards.bank_cards.dto.transfer.TransferResponse;
import com.abrik.bank_cards.bank_cards.dto.transfer.TransferStatus;
import com.abrik.bank_cards.bank_cards.entity.Transfer;
import com.abrik.bank_cards.bank_cards.repository.TransferRepository;
import com.abrik.bank_cards.bank_cards.util.TransferUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferAdminService {
    private final TransferRepository transferRepository;
    private final TransferUtil transferUtil;

    public Page<TransferResponse> listAll(TransferStatus status,
                                          Instant from,
                                          Instant to,
                                          UUID cardId,
                                          Pageable pageable) {
        var bounds = transferUtil.normalizeBounds(from, to);
        Pageable sortedPageable = transferUtil.ensureSort(pageable);
        Page<Transfer> page = transferRepository.searchAdmin(
                status, bounds.from(), bounds.to(), cardId, sortedPageable);
        return page.map(transferUtil::map);
    }
}
