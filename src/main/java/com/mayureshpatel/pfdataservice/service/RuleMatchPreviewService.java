package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
import com.mayureshpatel.pfdataservice.domain.category.MatchType;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.dto.category.RuleMatchPreviewDto;
import com.mayureshpatel.pfdataservice.dto.category.RuleMatchPreviewRequest;
import com.mayureshpatel.pfdataservice.dto.category.RuleMatchRowDto;
import com.mayureshpatel.pfdataservice.dto.category.RuleRefDto;
import com.mayureshpatel.pfdataservice.dto.category.RuleVerdict;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.mapper.CategoryDtoMapper;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRuleRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import com.mayureshpatel.pfdataservice.service.categorization.RuleMatcher;
import com.mayureshpatel.pfdataservice.service.categorization.RuleOrdering;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * The PF-900 live match preview for a draft category rule: which of the user's transactions the
 * draft would match, and for each whether the draft would categorize it, lose it to a saved rule
 * ranked ahead of it, or leave it alone because it already has a category. Read-only; nothing is
 * saved or changed.
 *
 * <p>Verdicts compare rule against rule, because rules are the only source of a category
 * in {@code TransactionCategorizer}. The
 * draft is ranked among the saved rules by {@link RuleOrdering} (the same order the SQL gives
 * import and "Apply rules") and matched by the shared {@link RuleMatcher}, so the preview and
 * reality cannot drift.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RuleMatchPreviewService {

    /** The most rows a preview returns; the counts always cover every match. */
    public static final int MAX_ROWS = 50;

    /** The id a new draft ranks with, so it sorts after saved rules it fully ties with. */
    private static final long NEW_DRAFT_ID = Long.MAX_VALUE;

    /** Verdict order (will-categorize first), then newest first, then highest id for stability. */
    private static final Comparator<RuleMatchRowDto> ROW_ORDER = Comparator
            .comparing(RuleMatchRowDto::verdict)
            .thenComparing(RuleMatchRowDto::date, Comparator.nullsLast(Comparator.<OffsetDateTime>reverseOrder()))
            .thenComparing(RuleMatchRowDto::transactionId, Comparator.nullsLast(Comparator.<Long>reverseOrder()));

    private final CategoryRuleRepository categoryRuleRepository;
    private final TransactionRepository transactionRepository;
    private final RuleMatcher ruleMatcher;

    /**
     * Previews a draft rule against the calling user's whole ledger. A draft with no non-blank
     * keyword matches nothing, so it returns an empty preview without querying anything. Otherwise
     * candidates come from the description prefilter and only those the draft itself matches are
     * kept; each gets a verdict from the first matching rule in the effective order.
     *
     * @param userId  the calling user's id; only their rules and transactions are read
     * @param request the draft rule
     * @return the counts over all matches and up to {@link #MAX_ROWS} rows
     * @throws ResourceNotFoundException if {@code ruleId} is set but is not one of the caller's own
     *                                   rules; a rule owned by someone else looks exactly like a missing one
     */
    public RuleMatchPreviewDto preview(Long userId, RuleMatchPreviewRequest request) {
        // ownership is checked before anything else so an empty draft cannot probe rule ids either
        List<CategoryRule> savedRules = request.ruleId() != null ? ownedRules(userId, request.ruleId()) : null;

        List<String> keywords = nonBlank(request.keywords());
        if (keywords.isEmpty()) {
            return RuleMatchPreviewDto.empty();
        }

        CategoryRule draft = toDraft(request, keywords);
        List<CategoryRule> ranked = RuleOrdering.rank(
                savedRules != null ? savedRules : categoryRuleRepository.findByUserId(userId),
                draft, request.ruleId());

        List<RuleMatchRowDto> matches = transactionRepository.findCandidatesByDescriptionKeywords(userId, keywords)
                .stream()
                .filter(transaction -> matches(transaction, draft))
                .map(transaction -> toRow(transaction, ranked, draft))
                .sorted(ROW_ORDER)
                .toList();

        return new RuleMatchPreviewDto(
                matches.size(),
                count(matches, RuleVerdict.WILL_CATEGORIZE),
                count(matches, RuleVerdict.LOSES_TO_OTHER_RULE),
                count(matches, RuleVerdict.ALREADY_CATEGORIZED),
                matches.size() > MAX_ROWS,
                matches.stream().limit(MAX_ROWS).toList(),
                willCategorizeIds(matches));
    }

    /** Loads the caller's rules and requires {@code ruleId} to be one of them, else not-found. */
    private List<CategoryRule> ownedRules(Long userId, Long ruleId) {
        List<CategoryRule> rules = categoryRuleRepository.findByUserId(userId);
        if (rules.stream().noneMatch(rule -> ruleId.equals(rule.getId()))) {
            throw new ResourceNotFoundException("Rule not found");
        }
        return rules;
    }

    /**
     * Builds the draft as a rule: no category is needed to match or rank it, and defaults follow
     * {@code CategoryRuleService.createRule} (match type {@code OR}, priority 0). An edited draft
     * keeps the saved rule's id so it ranks exactly where the saved rule would after the edit.
     */
    private static CategoryRule toDraft(RuleMatchPreviewRequest request, List<String> keywords) {
        return CategoryRule.builder()
                .id(request.ruleId() != null ? request.ruleId() : NEW_DRAFT_ID)
                .keywords(keywords)
                .matchType(request.matchType() != null ? request.matchType() : MatchType.OR)
                .priority(request.priority() != null ? request.priority() : 0)
                .minAmount(request.minAmount())
                .maxAmount(request.maxAmount())
                .build();
    }

    /**
     * Decides one matched transaction's verdict and lists the saved rules that also match it. A
     * categorized transaction is never changed by applying rules, so it is informational only;
     * for an uncategorized one the first matching rule in the effective order wins.
     */
    private RuleMatchRowDto toRow(Transaction transaction, List<CategoryRule> ranked, CategoryRule draft) {
        List<CategoryRule> matching = ranked.stream()
                .filter(rule -> matches(transaction, rule))
                .toList();

        RuleVerdict verdict;
        if (transaction.getCategory() != null) {
            verdict = RuleVerdict.ALREADY_CATEGORIZED;
        } else if (draft.equals(matching.get(0))) {
            verdict = RuleVerdict.WILL_CATEGORIZE;
        } else {
            verdict = RuleVerdict.LOSES_TO_OTHER_RULE;
        }

        // rules compare by id; the draft id is unique in the ranking because rank() drops the saved
        // version of an edited rule, and a new draft uses an id no saved rule can have
        List<RuleRefDto> otherRules = matching.stream()
                .filter(rule -> !draft.equals(rule))
                .map(RuleMatchPreviewService::toRef)
                .toList();

        return new RuleMatchRowDto(
                transaction.getId(),
                transaction.getTransactionDate(),
                transaction.getDescription(),
                transaction.getAmount(),
                transaction.getAccount() != null ? transaction.getAccount().getName() : null,
                CategoryDtoMapper.toDto(transaction.getCategory()),
                verdict,
                otherRules);
    }

    /** Runs the shared import matcher for one rule against one transaction. */
    private boolean matches(Transaction transaction, CategoryRule rule) {
        return ruleMatcher.matches(transaction.getDescription(), transaction.getAmount(), rule);
    }

    /** Maps a saved rule to the compact reference shown on a preview row. */
    private static RuleRefDto toRef(CategoryRule rule) {
        return new RuleRefDto(
                rule.getId(),
                rule.getKeywords(),
                rule.getCategory() != null ? rule.getCategory().getName() : null,
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
    /**
     * The ids of every match the draft would categorize, in the rows' order (newest first), capped
     * at {@link RuleMatchPreviewDto#MAX_WILL_CATEGORIZE_IDS}. The ledger uses it to apply the rule
     * to all of them when more match than the capped rows list.
     */
    private static List<Long> willCategorizeIds(List<RuleMatchRowDto> matches) {
        return matches.stream()
                .filter(row -> row.verdict() == RuleVerdict.WILL_CATEGORIZE)
                .map(RuleMatchRowDto::transactionId)
                .limit(RuleMatchPreviewDto.MAX_WILL_CATEGORIZE_IDS)
                .toList();
    }

    private static int count(List<RuleMatchRowDto> rows, RuleVerdict verdict) {
        return (int) rows.stream().filter(row -> row.verdict() == verdict).count();
    }
}
