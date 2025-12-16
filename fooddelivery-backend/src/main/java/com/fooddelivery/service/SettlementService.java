package com.fooddelivery.service;

import com.fooddelivery.entity.RiderLedger;
import com.fooddelivery.repository.RiderLedgerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementService {

    private final RiderLedgerRepository ledgerRepo;

    /**
     * Daily Settlement Job
     * Runs every day at 02:00 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void runDailySettlement() {
        log.info("Starting Daily Settlement Job...");

        // Logic to settle all riders...
        // For now, this is a placeholder as per user request to implement architecture.
        // Real implementation would iterate distinct riders from unsettled transactions.
    }

    // Manual Trigger for Demo
    @Transactional
    public void processSettlementForRider(String riderId) {
        log.info("Processing settlement for rider: {}", riderId);

        List<RiderLedger> entries = ledgerRepo.findByRiderIdAndIsSettledFalse(riderId);
        if (entries.isEmpty())
            return;

        double totalCredit = 0; // Earnings
        double totalDebit = 0; // Collections

        for (RiderLedger entry : entries) {
            if (entry.getAmount() > 0) {
                totalCredit += entry.getAmount();
            } else {
                totalDebit += Math.abs(entry.getAmount());
            }
        }

        double netPayable = totalCredit - totalDebit;

        log.info("Rider {}: Earnings={}, Collections={}, Net={}", riderId, totalCredit, totalDebit, netPayable);

        String ref = "SETTLEMENT-" + System.currentTimeMillis();

        if (netPayable > 0) {
            // Payout to Rider
            performPayout(riderId, netPayable, ref);
        } else if (netPayable < 0) {
            // Rider owes us. Carry forward? Or Request Payment?
            // For now, we mark as settled only if we claim it?
            // Usually we assume negative balance carries forward until positive.
            // So we DO NOT settle negative balances unless there is a mechanism to collect
            // from rider.
            // Pseudocode says: if (payable > 0) { payout; markSettled; }
            log.info("Negative balance. Carrying forward.");
            return;
        }

        // Mark as settled
        for (RiderLedger entry : entries) {
            entry.setSettled(true);
            entry.setSettlementReference(ref);
        }
        ledgerRepo.saveAll(entries);

        // Add PAYOUT entry to ledger (to zero out the balance visually?)
        // Actually, if we mark them settled, they disappear from "Net Payable".
        // But for history, we might want a PAYOUT entry.
        RiderLedger payoutEntry = RiderLedger.builder()
                .riderId(riderId)
                .amount(-netPayable) // Debit the payout from wallet to zero it
                .type(RiderLedger.LedgerType.PAYOUT)
                .description("Daily Payout " + ref)
                .isSettled(true) // Immediately settled as it IS the settlement
                .settlementReference(ref)
                .createdAt(LocalDateTime.now())
                .build();
        ledgerRepo.save(payoutEntry);
    }

    private void performPayout(String riderId, double amount, String ref) {
        // Integrate with Bank API / Razorpay Route
        log.info("INITIATING BANK TRANSFER: Rider={}, Amount={}, Ref={}", riderId, amount, ref);
    }
}
