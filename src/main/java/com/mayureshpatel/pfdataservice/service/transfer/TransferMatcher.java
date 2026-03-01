package com.mayureshpatel.pfdataservice.service.transfer;

import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.dto.transaction.TransferSuggestionDto;
import com.mayureshpatel.pfdataservice.mapper.TransactionDtoMapper;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class TransferMatcher {

    public List<TransferSuggestionDto> findMatches(List<Transaction> transactions) {
        List<TransferSuggestionDto> suggestions = new ArrayList<>();
        Set<Long> matchedIds = new HashSet<>();

        for (int i = 0; i < transactions.size(); i++) {
            Transaction t1 = transactions.get(i);
            if (matchedIds.contains(t1.getId())) continue;

            for (int j = i + 1; j < transactions.size(); j++) {
                Transaction t2 = transactions.get(j);
                if (matchedIds.contains(t2.getId())) continue;

                long daysDiff = Math.abs(ChronoUnit.DAYS.between(t1.getTransactionDate(), t2.getTransactionDate()));

                if (daysDiff > 3) {
                    break;
                }

                if (t1.getAmount().compareTo(t2.getAmount()) == 0) {
                    if (t1.getType() != t2.getType()) {
                        if (!t1.getAccount().getId().equals(t2.getAccount().getId())) {
                            suggestions.add(new TransferSuggestionDto(
                                    TransactionDtoMapper.toDto(t1),
                                    TransactionDtoMapper.toDto(t2),
                                    0.9 - (daysDiff * 0.1)
                            ));

                            matchedIds.add(t1.getId());
                            matchedIds.add(t2.getId());
                            break;
                        }
                    }
                }
            }
        }
        return suggestions;
    }
}
