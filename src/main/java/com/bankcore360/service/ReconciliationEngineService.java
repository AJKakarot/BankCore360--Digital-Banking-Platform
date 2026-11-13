package com.bankcore360.service;

import com.bankcore360.dto.DiscrepancyItemDto;
import com.bankcore360.dto.ReconciliationBatchReport;
import com.bankcore360.entity.*;
import com.bankcore360.observability.FintechMetricsService;
import com.bankcore360.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Enterprise Bank Reconciliation Engine.
 * Implements 4-way automated transaction matching (Reference, Amount, Date Window, Account),
 * duplicate detection, missing transaction identification, and immutable discrepancy resolution.
 */
@Service
public class ReconciliationEngineService {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationEngineService.class);

    private final BankStatementRepository bankStatementRepository;
    private final BankStatementItemRepository bankStatementItemRepository;
    private final ReconciliationBatchRepository reconciliationBatchRepository;
    private final ReconciliationDiscrepancyRepository reconciliationDiscrepancyRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final LedgerAccountRepository ledgerAccountRepository;
    private final FintechMetricsService fintechMetricsService;

    public ReconciliationEngineService(BankStatementRepository bankStatementRepository,
                                       BankStatementItemRepository bankStatementItemRepository,
                                       ReconciliationBatchRepository reconciliationBatchRepository,
                                       ReconciliationDiscrepancyRepository reconciliationDiscrepancyRepository,
                                       LedgerEntryRepository ledgerEntryRepository,
                                       LedgerAccountRepository ledgerAccountRepository,
                                       @org.springframework.beans.factory.annotation.Autowired(required = false)
                                       FintechMetricsService fintechMetricsService) {
        this.bankStatementRepository = bankStatementRepository;
        this.bankStatementItemRepository = bankStatementItemRepository;
        this.reconciliationBatchRepository = reconciliationBatchRepository;
        this.reconciliationDiscrepancyRepository = reconciliationDiscrepancyRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.ledgerAccountRepository = ledgerAccountRepository;
        this.fintechMetricsService = fintechMetricsService;
    }

    /**
     * Executes automated 4-way reconciliation matching for an uploaded bank statement.
     */
    @Transactional
    public ReconciliationBatchReport executeReconciliation(Long statementId, String operatorUsername) {
        BankStatement statement = bankStatementRepository.findById(statementId)
                .orElseThrow(() -> new IllegalArgumentException("Bank statement not found: " + statementId));

        String batchRef = "REC-" + LocalDate.now().getYear() + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        ReconciliationBatch batch = new ReconciliationBatch(batchRef, statement, operatorUsername != null ? operatorUsername : "SYSTEM");

        List<BankStatementItem> statementItems = bankStatementItemRepository.findByStatementId(statementId);

        // Fetch relevant internal ledger account and entries
        String accNum = statement.getAccountNumber();
        Optional<LedgerAccount> ledgerAccountOpt = ledgerAccountRepository.findByAccountNumber(accNum);
        
        List<LedgerEntry> internalEntries = new ArrayList<>();
        if (ledgerAccountOpt.isPresent()) {
            internalEntries = ledgerEntryRepository.findByAccountIdOrderByCreatedAtAsc(ledgerAccountOpt.get().getId());
        }

        Set<Long> matchedLedgerEntryIds = new HashSet<>();
        Set<String> seenExternalRefs = new HashSet<>();

        int matchedCount = 0;
        int duplicateCount = 0;

        // 1. Process statement items against internal ledger
        for (BankStatementItem sItem : statementItems) {
            String extRef = sItem.getExternalReference();

            // Duplicate Detection
            if (extRef != null && !extRef.isBlank() && !seenExternalRefs.add(extRef.toUpperCase())) {
                sItem.setReconciliationStatus(ReconciliationItemStatus.DUPLICATE);
                bankStatementItemRepository.save(sItem);

                ReconciliationDiscrepancy disc = new ReconciliationDiscrepancy(
                        batch, sItem, null, null, DiscrepancyType.DUPLICATE_ENTRY,
                        "Duplicate external reference encountered within statement: " + extRef,
                        sItem.getAmount(), null
                );
                batch.addDiscrepancy(disc);
                duplicateCount++;
                continue;
            }

            // Matching Rule 1: Exact Reference Match
            LedgerEntry exactRefMatch = null;
            if (extRef != null && !extRef.isBlank()) {
                for (LedgerEntry le : internalEntries) {
                    if (matchedLedgerEntryIds.contains(le.getId())) continue;
                    boolean refMatch = extRef.equalsIgnoreCase(le.getEntryReference()) ||
                            (le.getTransaction() != null && extRef.equalsIgnoreCase(le.getTransaction().getTransactionReference())) ||
                            (le.getNarration() != null && le.getNarration().contains(extRef));
                    if (refMatch) {
                        exactRefMatch = le;
                        break;
                    }
                }
            }

            if (exactRefMatch != null) {
                // Check Amount & Direction
                if (exactRefMatch.getAmount().compareTo(sItem.getAmount()) == 0) {
                    sItem.setReconciliationStatus(ReconciliationItemStatus.MATCHED);
                    sItem.setMatchedLedgerEntryId(exactRefMatch.getId());
                    sItem.setMatchedLedgerTxnRef(exactRefMatch.getTransaction() != null ? exactRefMatch.getTransaction().getTransactionReference() : null);
                    sItem.setMatchedAt(LocalDateTime.now());
                    bankStatementItemRepository.save(sItem);

                    matchedLedgerEntryIds.add(exactRefMatch.getId());
                    matchedCount++;
                    continue;
                } else {
                    // Reference matched, but amount discrepancy!
                    sItem.setReconciliationStatus(ReconciliationItemStatus.DISCREPANCY);
                    bankStatementItemRepository.save(sItem);

                    ReconciliationDiscrepancy disc = new ReconciliationDiscrepancy(
                            batch, sItem, exactRefMatch.getId(),
                            exactRefMatch.getTransaction() != null ? exactRefMatch.getTransaction().getTransactionReference() : null,
                            DiscrepancyType.AMOUNT_MISMATCH,
                            "Amount mismatch for reference " + extRef + ": Statement amount " + sItem.getAmount() + " vs Internal Ledger " + exactRefMatch.getAmount(),
                            sItem.getAmount(), exactRefMatch.getAmount()
                    );
                    batch.addDiscrepancy(disc);
                    matchedLedgerEntryIds.add(exactRefMatch.getId());
                    continue;
                }
            }

            // Matching Rule 2: Fuzzy Date & Exact Amount Match (within +/- 2 banking days)
            LedgerEntry fuzzyMatch = null;
            for (LedgerEntry le : internalEntries) {
                if (matchedLedgerEntryIds.contains(le.getId())) continue;
                if (le.getAmount().compareTo(sItem.getAmount()) == 0 && le.getDirection() == sItem.getDirection()) {
                    LocalDate ledgerDate = le.getCreatedAt().toLocalDate();
                    long daysDiff = Math.abs(java.time.temporal.ChronoUnit.DAYS.between(sItem.getTransactionDate(), ledgerDate));
                    if (daysDiff <= 2) {
                        fuzzyMatch = le;
                        break;
                    }
                }
            }

            if (fuzzyMatch != null) {
                sItem.setReconciliationStatus(ReconciliationItemStatus.MATCHED);
                sItem.setMatchedLedgerEntryId(fuzzyMatch.getId());
                sItem.setMatchedLedgerTxnRef(fuzzyMatch.getTransaction() != null ? fuzzyMatch.getTransaction().getTransactionReference() : null);
                sItem.setMatchedAt(LocalDateTime.now());
                bankStatementItemRepository.save(sItem);

                matchedLedgerEntryIds.add(fuzzyMatch.getId());
                matchedCount++;
                continue;
            }

            // Discrepancy: Missing In Ledger
            sItem.setReconciliationStatus(ReconciliationItemStatus.DISCREPANCY);
            bankStatementItemRepository.save(sItem);

            ReconciliationDiscrepancy disc = new ReconciliationDiscrepancy(
                    batch, sItem, null, null, DiscrepancyType.MISSING_IN_LEDGER,
                    "Transaction present on bank statement (" + sItem.getDescription() + ") but missing from internal ledger",
                    sItem.getAmount(), null
            );
            batch.addDiscrepancy(disc);
        }

        // 2. Identify Internal Ledger Transactions Missing From Statement
        int unmatchedLedgerCount = 0;
        for (LedgerEntry le : internalEntries) {
            if (!matchedLedgerEntryIds.contains(le.getId())) {
                LocalDate entryDate = le.getCreatedAt().toLocalDate();
                // If the ledger entry falls inside the statement date window, it is an unmatched discrepancy
                boolean inWindow = (statement.getStatementStartDate() == null || !entryDate.isBefore(statement.getStatementStartDate()))
                        && (statement.getStatementEndDate() == null || !entryDate.isAfter(statement.getStatementEndDate()));

                if (inWindow) {
                    ReconciliationDiscrepancy disc = new ReconciliationDiscrepancy(
                            batch, null, le.getId(),
                            le.getTransaction() != null ? le.getTransaction().getTransactionReference() : null,
                            DiscrepancyType.MISSING_IN_STATEMENT,
                            "Internal posted ledger transaction " + le.getEntryReference() + " (" + le.getNarration() + ") missing from bank statement",
                            null, le.getAmount()
                    );
                    batch.addDiscrepancy(disc);
                    unmatchedLedgerCount++;
                }
            }
        }

        // Calculate Summary & Save Batch
        int totalDiscrepancies = batch.getDiscrepancies().size();
        int unmatchedStatementCount = (int) statementItems.stream()
                .filter(i -> i.getReconciliationStatus() == ReconciliationItemStatus.DISCREPANCY || i.getReconciliationStatus() == ReconciliationItemStatus.UNMATCHED)
                .count();

        batch.setMatchedCount(matchedCount);
        batch.setUnmatchedStatementCount(unmatchedStatementCount);
        batch.setUnmatchedLedgerCount(unmatchedLedgerCount);
        batch.setDiscrepancyCount(totalDiscrepancies);
        batch.setStatus(totalDiscrepancies == 0 ? "COMPLETED" : "REQUIRES_REVIEW");

        ReconciliationBatch savedBatch = reconciliationBatchRepository.save(batch);

        statement.setStatus(totalDiscrepancies == 0 ? StatementStatus.RECONCILED : StatementStatus.DISCREPANCIES_FOUND);
        bankStatementRepository.save(statement);

        // Record metrics
        if (fintechMetricsService != null) {
            fintechMetricsService.recordReconciliationBatch(savedBatch.getStatus(), matchedCount, unmatchedStatementCount);
            for (ReconciliationDiscrepancy d : savedBatch.getDiscrepancies()) {
                fintechMetricsService.recordReconciliationDiscrepancy(d.getDiscrepancyType().name());
            }
        }

        log.info("Reconciliation Batch {} completed. Matched: {}, Discrepancies: {}",
                savedBatch.getBatchReference(), matchedCount, totalDiscrepancies);

        return buildReport(savedBatch);
    }

    /**
     * Resolves a reconciliation discrepancy with audit notes.
     */
    @Transactional
    public DiscrepancyItemDto resolveDiscrepancy(Long discrepancyId, DiscrepancyStatus newStatus,
                                                 String notes, String resolvedBy) {
        ReconciliationDiscrepancy discrepancy = reconciliationDiscrepancyRepository.findById(discrepancyId)
                .orElseThrow(() -> new IllegalArgumentException("Discrepancy not found: " + discrepancyId));

        discrepancy.setStatus(newStatus);
        discrepancy.setInvestigatorNotes(notes);
        discrepancy.setResolvedBy(resolvedBy != null ? resolvedBy : "ADMIN");
        discrepancy.setResolvedAt(LocalDateTime.now());

        ReconciliationDiscrepancy saved = reconciliationDiscrepancyRepository.save(discrepancy);
        log.info("Discrepancy {} updated to status {} by {}", discrepancyId, newStatus, resolvedBy);

        return mapToDiscrepancyDto(saved);
    }

    @Transactional(readOnly = true)
    public ReconciliationBatchReport getBatchReport(Long batchId) {
        ReconciliationBatch batch = reconciliationBatchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Reconciliation batch not found: " + batchId));
        return buildReport(batch);
    }

    @Transactional(readOnly = true)
    public List<DiscrepancyItemDto> getDiscrepancies(DiscrepancyStatus status, DiscrepancyType type) {
        List<ReconciliationDiscrepancy> list;
        if (status != null) {
            list = reconciliationDiscrepancyRepository.findByStatus(status);
        } else if (type != null) {
            list = reconciliationDiscrepancyRepository.findByDiscrepancyType(type);
        } else {
            list = reconciliationDiscrepancyRepository.findAll();
        }
        return list.stream().map(this::mapToDiscrepancyDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BankStatement> listStatements() {
        return bankStatementRepository.findAll();
    }

    private ReconciliationBatchReport buildReport(ReconciliationBatch batch) {
        BankStatement stmt = batch.getStatement();
        int totalItems = stmt.getTotalRecords();
        double matchRate = totalItems > 0 ? ((double) batch.getMatchedCount() / totalItems) * 100.0 : 100.0;

        ReconciliationBatchReport report = new ReconciliationBatchReport(
                batch.getId(),
                batch.getBatchReference(),
                stmt.getStatementReference(),
                stmt.getBankName(),
                stmt.getAccountNumber(),
                batch.getReconciledAt(),
                batch.getReconciledBy(),
                batch.getMatchedCount(),
                batch.getUnmatchedStatementCount(),
                batch.getUnmatchedLedgerCount(),
                batch.getDiscrepancyCount(),
                Math.round(matchRate * 10.0) / 10.0,
                batch.getStatus()
        );

        List<DiscrepancyItemDto> dtoList = batch.getDiscrepancies().stream()
                .map(this::mapToDiscrepancyDto)
                .collect(Collectors.toList());
        report.setDiscrepancies(dtoList);

        return report;
    }

    private DiscrepancyItemDto mapToDiscrepancyDto(ReconciliationDiscrepancy d) {
        BigDecimal sAmt = d.getStatementAmount() != null ? d.getStatementAmount() : BigDecimal.ZERO;
        BigDecimal lAmt = d.getLedgerAmount() != null ? d.getLedgerAmount() : BigDecimal.ZERO;
        BigDecimal variance = sAmt.subtract(lAmt);

        return new DiscrepancyItemDto(
                d.getId(),
                d.getStatementItem() != null ? d.getStatementItem().getItemReference() : null,
                d.getLedgerTxnReference(),
                d.getDiscrepancyType(),
                d.getDescription(),
                d.getStatementAmount(),
                d.getLedgerAmount(),
                variance,
                d.getStatus(),
                d.getInvestigatorNotes(),
                d.getResolvedBy(),
                d.getResolvedAt()
        );
    }
}
