package com.fooddelivery.service;

import com.fooddelivery.entity.RiderLedger;
import com.fooddelivery.repository.RiderLedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WalletService {
    private final RiderLedgerRepository legoRepo;

    public void addEntry(String riderId, Double amount, RiderLedger.LedgerType type, String orderId, String desc) {
        RiderLedger entry = RiderLedger.builder()
                .riderId(riderId)
                .amount(amount)
                .type(type)
                .orderId(orderId)
                .description(desc)
                .createdAt(LocalDateTime.now())
                .isSettled(false)
                .build();
        legoRepo.save(entry);
    }

    public Map<String, Object> getWalletSummary(String riderId) {
        List<RiderLedger> history = legoRepo.findByRiderIdOrderByCreatedAtDesc(riderId);
        
        // Calculate stats
        double totalEarnings = 0;
        double cashCollected = 0;
        double netPayable = 0;

        for (RiderLedger l : history) {
             // Total Earnings (Lifetime or Unsettled? User prompt implies Lifetime/Total in UI, but usually you show Current Balance)
             // UI shows: Total Balance (Net Payable), Completed Orders, Incentives.
             
             if (l.getType() == RiderLedger.LedgerType.EARNING || l.getType() == RiderLedger.LedgerType.INCENTIVE) {
                 totalEarnings += l.getAmount();
             }
             if (l.getType() == RiderLedger.LedgerType.COLLECTION) {
                 cashCollected += Math.abs(l.getAmount());
             }
             
             if (!l.isSettled()) {
                 netPayable += l.getAmount();
             }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("netPayable", netPayable);
        response.put("totalEarnings", totalEarnings);
        response.put("cashCollected", cashCollected);
        response.put("transactions", history.stream().limit(20).toList()); // Recent 20

        return response;
    }
}
