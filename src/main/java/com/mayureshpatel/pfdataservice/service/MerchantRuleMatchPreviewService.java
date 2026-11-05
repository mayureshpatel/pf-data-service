package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.category.MatchType;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.merchant.MerchantRule;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleMatchPreviewDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleMatchPreviewRequest;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleMatchRowDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleRefDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleVerdict;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.mapper.MerchantDtoMapper;
import com.mayureshpatel.pfdataservice.repository.merchant.MerchantRuleRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import com.mayureshpatel.pfdataservice.service.categorization.RuleMatcher;
import com.mayureshpatel.pfdataservice.service.categorization.RuleOrdering;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * The PF-905 live match preview for a draft merchant rule: which of the user's transactions the
 * draft would match, and for each whether the draft would assign a merchant, lose it to a saved
 * rule ranked ahead of it, or leave it alone because it already has a merchant or an exact
 * description link. Read-only; nothing is saved or changed. The merchant parallel of
 * {@link RuleMatchPreviewService}.
 *
 * <p>A transaction that already has a merchant is never touched, and an exact description link
 * always beats a rule, so those two are checked before rules are compared. The draft is ranked
 * among the saved rules by {@link RuleOrdering} and matched by the shared {@link RuleMatcher}, and
 * links are looked up through {@link MerchantService#findMatchingMerchantIds} (the same key import
 * uses), so the preview and reality cannot drift.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MerchantRuleMatchPreviewService {

    /** The most rows a preview returns; the counts always cover every match. */
    public static final int MAX_ROWS = 50;

    /** The id a new draft ranks with, so it sorts after saved rules it fully ties with. */
    private static final long NEW_DRAFT_ID = Long.MAX_VALUE;

    /** Verdict order (will-assign first), then newest first, then highest id for stability. */
    private static final Comparator<MerchantRuleMatchRowDto> ROW_ORDER = Comparator
            .comparing(MerchantRuleMatchRowDto::verdict)
            .thenComparing(MerchantRuleMatchRowDto::date, Comparator.nullsLast(Comparator.<OffsetDateTime>reverseOrder()))
            .thenComparing(MerchantRuleMatchRowDto::transactionId, Comparator.nullsLast(Comparator.<Long>reverseOrder()));

    private final MerchantRuleRepository merchantRuleRepository;
    private final TransactionRepository transactionRepository;
    private final MerchantService merchantService;
    private final RuleMatcher ruleMatcher;

    /**
     * Previews a draft rule against the calling user's whole ledger. A draft with no non-blank
     * keyword matches nothing, so it returns an empty preview without reading any transaction.
     * Otherwise candidates come from the description prefilter and only those the draft itself
     * matches are kept; each gets a verdict.
     *
     * @param userId  the calling user's id; only their rules, merchants, links and transactions are read
     * @param request the draft rule
     * @return the counts over all matches and up to {@link #MAX_ROWS} rows
     * @throws ResourceNotFoundException if {@code ruleId} is not one of the caller's own rules, or
     *                                   {@code merchantId} is not one of the caller's own merchants
     */
    public MerchantRuleMatchPreviewDto preview(Long userId, MerchantRuleMatchPreviewRequest request) {
        // ownership is checked before anything else so an empty draft cannot probe ids either
        List<MerchantRule> savedRules = merchantRuleRepository.findByUserId(userId);
        if (request.ruleId() != null && savedRules.stream().noneMatch(rule -> request.ruleId().equals(rule.getId()))) {
            throw new ResourceNotFoundException("Rule not found");
        }
        Merchant target = request.merchantId() != null
                ? merchantService.requireOwnedMerchant(userId, request.merchantId())
                : null;

        List<String> keywords = nonBlank(request.keywords());
        if (keywords.isEmpty()) {
            return MerchantRuleMatchPreviewDto.empty(target != null ? target.getName() : null);
        }

        MerchantRule draft = toDraft(request, keywords, target);
        List<MerchantRule> ranked = RuleOrdering.rank(savedRules, draft, request.ruleId());

        List<Transaction> matched = transactionRepository.findCandidatesByDescriptionKeywords(userId, keywords)
                .stream()
                .filter(transaction -> matches(transaction, draft))
                .toList();
        Map<String, Long> linked = linkedDescriptions(userId, matched);

        List<MerchantRuleMatchRowDto> rows = matched.stream()
                .map(transaction -> toRow(transaction, ranked, draft, linked))
                .sorted(ROW_ORDER)
                .toList();

        return new MerchantRuleMatchPreviewDto(
                target != null ? target.getName() : null,
                rows.size(),
                count(rows, MerchantRuleVerdict.WILL_ASSIGN),
                count(rows, MerchantRuleVerdict.LOSES_TO_OTHER_RULE),
                count(rows, MerchantRuleVerdict.ALREADY_HAS_MERCHANT),
                count(rows, MerchantRuleVerdict.LINKED),
                rows.size() > MAX_ROWS,
                rows.stream().limit(MAX_ROWS).toList());
    }

    /**
     * Builds the draft as a rule: defaults follow {@code MerchantRuleService.createRule} (match
     * type {@code OR}, priority 0). An edited draft keeps the saved rule's id so it ranks exactly
     * where the saved rule would after the edit.
     */
    private static MerchantRule toDraft(MerchantRuleMatchPreviewRequest request, List<String> keywords, Merchant target) {
        return MerchantRule.builder()
                .id(request.ruleId() != null ? request.ruleId() : NEW_DRAFT_ID)
                .keywords(keywords)
                .matchType(request.matchType() != null ? request.matchType() : MatchType.OR)
                .priority(request.priority() != null ? request.priority() : 0)
                .merchant(target)
                .minAmount(request.minAmount())
                .maxAmount(request.maxAmount())
                .build();
    }

    /**
     * Looks up, in one batch, which of the merchant-less matched transactions have an exact
     * description link. A transaction with a merchant is never looked up, since its verdict does
     * not depend on links.
     */
    private Map<String, Long> linkedDescriptions(Long userId, List<Transaction> matched) {
        List<String> descriptions = matched.stream()
                .filter(transaction -> transaction.getMerchant() == null)
                .map(Transaction::getDescription)
                .distinct()
                .toList();
        return merchantService.findMatchingMerchantIds(userId, descriptions);
    }

    /**
     * Decides one matched transaction's verdict and lists the saved rules that also match it. A
     * transaction with a merchant is informational only, then an exact link beats any rule, and
     * for the rest the first matching rule in the effective order wins.
     */
    private MerchantRuleMatchRowDto toRow(Transaction transaction, List<MerchantRule> ranked,
                                          MerchantRule draft, Map<String, Long> linked) {
        List<MerchantRule> matching = ranked.stream()
                .filter(rule -> matches(transaction, rule))
                .toList();

        // rules compare by id; the draft id is unique in the ranking because rank() drops the saved
        // version of an edited rule, and a new draft uses an id no saved rule can have
        List<MerchantRuleRefDto> otherRules = matching.stream()
                .filter(rule -> !draft.equals(rule))
                .map(MerchantRuleMatchPreviewService::toRef)
                .toList();

        MerchantRuleVerdict verdict;
        MerchantRuleRefDto winner = null;
        if (transaction.getMerchant() != null) {
            verdict = MerchantRuleVerdict.ALREADY_HAS_MERCHANT;
        } else if (linked.containsKey(transaction.getDescription())) {
            verdict = MerchantRuleVerdict.LINKED;
        } else if (draft.equals(matching.get(0))) {
            verdict = MerchantRuleVerdict.WILL_ASSIGN;
        } else {
            verdict = MerchantRuleVerdict.LOSES_TO_OTHER_RULE;
            winner = toRef(matching.get(0));
        }

        return new MerchantRuleMatchRowDto(
                transaction.getId(),
                transaction.getTransactionDate(),
                transaction.getDescription(),
                transaction.getAmount(),
                transaction.getAccount() != null ? transaction.getAccount().getName() : null,
                MerchantDtoMapper.toDto(transaction.getMerchant()),
                verdict,
                otherRules,
                winner);
    }

    /** Runs the shared matcher for one rule against one transaction. */
    private boolean matches(Transaction transaction, MerchantRule rule) {
        return ruleMatcher.matches(transaction.getDescription(), transaction.getAmount(), rule);
    }

    /** Maps a saved rule to the compact reference shown on a preview row. */
    static MerchantRuleRefDto toRef(MerchantRule rule) {
        return new MerchantRuleRefDto(
                rule.getId(),
                rule.getKeywords(),
                rule.getMerchant() != null ? rule.getMerchant().getName() : null,
                rule.getPriority());
    }

    /** Drops null and blank keywords, which can never match anything. */
    private static List<String> nonBlank(List<String> keywords) {
        if (keywords == null) {
            return List.of();
        }
        return keywords.stream()
                .filter(keyword -> keyword != null && !keyword.isBlank())
                .toList();
    }

    /** Counts the rows with the given verdict. */
    private static int count(List<MerchantRuleMatchRowDto> rows, MerchantRuleVerdict verdict) {
        return (int) rows.stream().filter(row -> row.verdict() == verdict).count();
    }
}
