package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.category.MatchType;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.merchant.MerchantRule;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleMatchPreviewDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleMatchPreviewRequest;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleMatchRowDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleOverlapDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleOverlapOutcome;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleOverlapReason;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
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
 *
 * <p>PF-981 adds overlaps: where the draft meets another merchant. For every matched transaction,
 * whatever merchant it has today, the description is resolved the way import would resolve it (an
 * exact link first, then {@link ImportMerchantResolver#firstMatchingRule} over the ranked rules);
 * when another merchant takes it, or the draft takes it from a rule pointing at another merchant,
 * the description joins a group that says who wins and why, in {@link RuleOrdering}'s own terms.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MerchantRuleMatchPreviewService {

    /** The most rows a preview returns; the counts always cover every match. */
    public static final int MAX_ROWS = 50;

    /** The id a new draft ranks with, so it sorts after saved rules it fully ties with. */
    private static final long NEW_DRAFT_ID = Long.MAX_VALUE;

    /** Other-merchant wins first, then the biggest groups, then by merchant name for stability. */
    private static final Comparator<MerchantRuleOverlapDto> OVERLAP_ORDER = Comparator
            .comparing((MerchantRuleOverlapDto overlap) -> overlap.outcome() != MerchantRuleOverlapOutcome.OTHER_WINS)
            .thenComparing(MerchantRuleOverlapDto::transactionCount, Comparator.reverseOrder())
            .thenComparing(MerchantRuleOverlapDto::merchantName, Comparator.nullsLast(Comparator.<String>naturalOrder()));

    /** Verdict order (will-assign first), then newest first, then highest id for stability. */
    private static final Comparator<MerchantRuleMatchRowDto> ROW_ORDER = Comparator
            .comparing(MerchantRuleMatchRowDto::verdict)
            .thenComparing(MerchantRuleMatchRowDto::date, Comparator.nullsLast(Comparator.<OffsetDateTime>reverseOrder()))
            .thenComparing(MerchantRuleMatchRowDto::transactionId, Comparator.nullsLast(Comparator.<Long>reverseOrder()));

    private final MerchantRuleRepository merchantRuleRepository;
    private final TransactionRepository transactionRepository;
    private final MerchantService merchantService;
    private final RuleMatcher ruleMatcher;
    private final ImportMerchantResolver importMerchantResolver;

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
        // with a target, one lookup resolves links to merchants for both the verdicts and the overlaps
        Map<String, Merchant> links = target != null ? linkedMerchants(userId, matched) : null;
        Map<String, Long> linked = links != null ? unassignedLinkIds(matched, links) : linkedDescriptions(userId, matched);

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
                rows.stream().limit(MAX_ROWS).toList(),
                willAssignIds(rows),
                overlaps(matched, ranked, draft, target, links));
    }

    /**
     * Resolves every matched description's exact link to its merchant in one lookup (PF-981). All
     * matched transactions are looked up, not only merchant-less ones, because overlaps report
     * where import would put each description whatever merchant it has today.
     *
     * @param userId  the calling user's id
     * @param matched the transactions the draft matches
     * @return each linked raw description mapped to its merchant
     */
    private Map<String, Merchant> linkedMerchants(Long userId, List<Transaction> matched) {
        List<String> descriptions = matched.stream().map(Transaction::getDescription).distinct().toList();
        return merchantService.findMatchingMerchants(userId, descriptions);
    }

    /**
     * Narrows resolved links to the merchant-less transactions' descriptions, as merchant ids: the
     * same map {@link #linkedDescriptions} builds, without a second lookup.
     *
     * @param matched the transactions the draft matches
     * @param links   every matched description's link, from {@link #linkedMerchants}
     * @return each merchant-less linked description mapped to its merchant id
     */
    private static Map<String, Long> unassignedLinkIds(List<Transaction> matched, Map<String, Merchant> links) {
        Map<String, Long> ids = new HashMap<>();
        for (Transaction transaction : matched) {
            Merchant merchant = links.get(transaction.getDescription());
            if (transaction.getMerchant() == null && merchant != null) {
                ids.put(transaction.getDescription(), merchant.getId());
            }
        }
        return ids;
    }

    /**
     * Groups the matched transactions where the draft meets another merchant (PF-981). Needs a
     * target merchant: without one there is no "other" merchant to compare with, so it is empty.
     * Each transaction is placed the way import would place it: an exact link first, then the
     * first matching rule in {@code ranked}. Groups are per merchant and deciding rule, because
     * the reason belongs to a rule; a merchant reached through two rules appears twice.
     *
     * @param matched the transactions the draft matches, whatever merchant each has today
     * @param ranked  the saved rules with the draft ranked among them
     * @param draft   the draft rule
     * @param target  the merchant the draft points at, or null
     * @param links   every matched description's link, or null when there is no target
     * @return the overlap groups, other-merchant wins first, then by transaction count
     */
    private List<MerchantRuleOverlapDto> overlaps(List<Transaction> matched, List<MerchantRule> ranked,
                                                  MerchantRule draft, Merchant target, Map<String, Merchant> links) {
        if (target == null || links == null || matched.isEmpty()) {
            return List.of();
        }

        Map<OverlapKey, OverlapGroup> groups = new LinkedHashMap<>();
        for (Transaction transaction : matched) {
            String description = transaction.getDescription();
            Merchant linked = links.get(description);
            if (linked != null) {
                // an exact link beats every rule, the draft included
                if (!target.getId().equals(linked.getId())) {
                    group(groups, MerchantRuleOverlapOutcome.OTHER_WINS, MerchantRuleOverlapReason.LINK, linked, null, description);
                }
                continue;
            }
            MerchantRule winner = importMerchantResolver.firstMatchingRule(ranked,
                    new ImportMerchantResolver.ImportRow(description, transaction.getAmount()));
            if (winner == null) {
                continue;
            }
            if (draft.equals(winner)) {
                MerchantRule rival = strongestRival(ranked, draft, target, transaction);
                if (rival != null) {
                    group(groups, MerchantRuleOverlapOutcome.THIS_RULE_WINS, reason(draft, rival), rival.getMerchant(), rival, description);
                }
            } else if (!target.getId().equals(winner.getMerchant().getId())) {
                group(groups, MerchantRuleOverlapOutcome.OTHER_WINS, reason(winner, draft), winner.getMerchant(), winner, description);
            }
        }
        return groups.values().stream()
                .map(OverlapGroup::toDto)
                .sorted(OVERLAP_ORDER)
                .toList();
    }

    /**
     * The highest-ranked rule, other than the draft, that matches the transaction and points at a
     * merchant other than the target: the rule the draft beats there. Only the strongest rival is
     * reported, even when rules for several other merchants match.
     *
     * @param ranked      the saved rules with the draft ranked among them
     * @param draft       the draft rule, which won the transaction
     * @param target      the merchant the draft points at
     * @param transaction the transaction the draft won
     * @return the strongest other-merchant rule matching the transaction, or null if none does
     */
    private MerchantRule strongestRival(List<MerchantRule> ranked, MerchantRule draft, Merchant target, Transaction transaction) {
        for (MerchantRule rule : ranked) {
            Merchant merchant = rule.getMerchant();
            if (!draft.equals(rule) && merchant != null && merchant.getId() != null
                    && !target.getId().equals(merchant.getId()) && matches(transaction, rule)) {
                return rule;
            }
        }
        return null;
    }

    /**
     * Translates the ordering step that puts {@code winner} ahead of {@code loser} into a reason.
     *
     * @param winner the rule ranked ahead
     * @param loser  the rule ranked behind it
     * @return the first rule-order key that separates them, as an overlap reason
     */
    static MerchantRuleOverlapReason reason(MerchantRule winner, MerchantRule loser) {
        return switch (RuleOrdering.decidingCriterion(winner, loser)) {
            case PRIORITY -> MerchantRuleOverlapReason.HIGHER_PRIORITY;
            case KEYWORD_LENGTH -> MerchantRuleOverlapReason.LONGER_KEYWORDS;
            case ID -> MerchantRuleOverlapReason.LOWER_ID;
        };
    }

    /**
     * Adds one transaction's description to its overlap group, creating the group on first use.
     *
     * @param groups      the groups built so far, in first-seen order
     * @param outcome     whether the draft or the other merchant takes the transaction
     * @param reason      why the winner wins
     * @param merchant    the other merchant
     * @param rule        the other merchant's deciding rule, or null for a link
     * @param description the transaction's raw description
     */
    private static void group(Map<OverlapKey, OverlapGroup> groups, MerchantRuleOverlapOutcome outcome,
                              MerchantRuleOverlapReason reason, Merchant merchant, MerchantRule rule, String description) {
        OverlapKey key = new OverlapKey(outcome, reason, merchant.getId(), rule != null ? rule.getId() : null);
        groups.computeIfAbsent(key, k -> new OverlapGroup(outcome, reason, merchant, rule)).add(description);
    }

    /**
     * What an overlap group is keyed on: one group per outcome, reason, merchant and rule.
     *
     * @param outcome    whether the draft or the other merchant wins
     * @param reason     why the winner wins
     * @param merchantId the other merchant's id
     * @param ruleId     the other merchant's deciding rule id, or null for a link
     */
    private record OverlapKey(MerchantRuleOverlapOutcome outcome, MerchantRuleOverlapReason reason, Long merchantId, Long ruleId) {
    }

    /** Collects one overlap group's descriptions, counted by folded key, keeping the first spelling seen. */
    private static final class OverlapGroup {
        private final MerchantRuleOverlapOutcome outcome;
        private final MerchantRuleOverlapReason reason;
        private final Merchant merchant;
        private final MerchantRule rule;
        private final Map<String, Integer> countByKey = new LinkedHashMap<>();
        private final Map<String, String> spellingByKey = new LinkedHashMap<>();
        private int transactions;

        private OverlapGroup(MerchantRuleOverlapOutcome outcome, MerchantRuleOverlapReason reason, Merchant merchant, MerchantRule rule) {
            this.outcome = outcome;
            this.reason = reason;
            this.merchant = merchant;
            this.rule = rule;
        }

        private void add(String description) {
            String key = DescriptionKeys.fold(description);
            countByKey.merge(key, 1, Integer::sum);
            spellingByKey.putIfAbsent(key, description);
            transactions++;
        }

        private MerchantRuleOverlapDto toDto() {
            List<String> samples = countByKey.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                    .limit(MerchantRuleOverlapDto.MAX_DESCRIPTIONS)
                    .map(entry -> spellingByKey.get(entry.getKey()))
                    .toList();
            return new MerchantRuleOverlapDto(outcome, reason, merchant.getId(), merchant.getName(),
                    rule != null ? toRef(rule) : null, countByKey.size(), transactions, samples);
        }
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

    /**
     * The ids of every match the draft would give a merchant, in the rows' order (newest first),
     * capped at {@link MerchantRuleMatchPreviewDto#MAX_WILL_ASSIGN_IDS}. The ledger uses it to apply
     * the rule to all of them when more match than the capped rows list.
     */
    private static List<Long> willAssignIds(List<MerchantRuleMatchRowDto> rows) {
        return rows.stream()
                .filter(row -> row.verdict() == MerchantRuleVerdict.WILL_ASSIGN)
                .map(MerchantRuleMatchRowDto::transactionId)
                .limit(MerchantRuleMatchPreviewDto.MAX_WILL_ASSIGN_IDS)
                .toList();
    }

    /** Counts the rows with the given verdict. */
    private static int count(List<MerchantRuleMatchRowDto> rows, MerchantRuleVerdict verdict) {
        return (int) rows.stream().filter(row -> row.verdict() == verdict).count();
    }
}
