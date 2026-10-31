package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.account.AccountType;
import com.mayureshpatel.pfdataservice.dto.report.explore.DateWindow;
import com.mayureshpatel.pfdataservice.dto.report.networth.NetWorthBreakdownDto;
import com.mayureshpatel.pfdataservice.dto.report.networth.NetWorthCompare;
import com.mayureshpatel.pfdataservice.dto.report.networth.NetWorthGroupBy;
import com.mayureshpatel.pfdataservice.dto.report.networth.NetWorthGroupDto;
import com.mayureshpatel.pfdataservice.dto.report.networth.NetWorthPointDto;
import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.NetWorthRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.NetWorthRepository.IncomeExpense;
import com.mayureshpatel.pfdataservice.repository.transaction.NetWorthRepository.StoryFlows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies {@code NetWorthService}. The fixture is small enough to check by hand: checking (current 1000), savings
 * (current 5000) and a credit card (current -400), with monthly net flows chosen so the month-end balances are
 * checking 920/1020/970/1000, savings 4800/4800/5000/5000 and card -300/-300/-400/-400 at Jul/Aug/Sep/Oct month ends
 * (today is Oct 25, so October takes the current balance).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NetWorthService Unit Tests")
class NetWorthServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 25);
    private static final LocalDate AUG_1 = LocalDate.of(2026, 8, 1);
    private static final String CHECKING = "CHECKING";
    private static final String SAVINGS = "SAVINGS";
    private static final String CREDIT_CARD = "CREDIT_CARD";
    private static final String CASH = "CASH";
    private static final String ASSET = "ASSET";
    private static final String DEBT = "DEBT";
    private static final String ZERO = "0";
    private static final String OWED_400 = "-400";
    private static final String SIX_THOUSAND = "6000";
    private static final String HUNDRED = "100";
    private static final String FOUR_HUNDRED = "400";
    private static final String FIVE_THOUSAND = "5000";
    private static final long CHECKING_ID = 1L;
    private static final long SAVINGS_ID = 2L;
    private static final long CARD_ID = 3L;
    private static final YearMonth AUG = YearMonth.of(2026, 8);
    private static final YearMonth SEP = YearMonth.of(2026, 9);
    private static final YearMonth OCT = YearMonth.of(2026, 10);

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private NetWorthRepository netWorthRepository;
    @InjectMocks
    private NetWorthService service;

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }

    private static Account account(long id, String name, String typeCode, boolean asset, String balance, OffsetDateTime created) {
        return Account.builder()
                .id(id).userId(USER_ID).name(name)
                .type(AccountType.builder().code(typeCode).label(typeCode.toLowerCase(Locale.ROOT)).asset(asset).build())
                .currentBalance(money(balance))
                .audit(TableAudit.builder().createdAt(created).build())
                .build();
    }

    private static final OffsetDateTime LONG_AGO = OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    private final Account checking = account(CHECKING_ID, "Everyday", CHECKING, true, "1000", LONG_AGO);
    private final Account savings = account(SAVINGS_ID, "Rainy day", SAVINGS, true, FIVE_THOUSAND, LONG_AGO);
    private final Account card = account(CARD_ID, "Visa", CREDIT_CARD, false, OWED_400, LONG_AGO);

    @BeforeEach
    void setUp() {
        lenient().when(accountRepository.findAllByUserId(USER_ID)).thenReturn(List.of(checking, savings, card));
        lenient().when(netWorthRepository.findMonthlyNetFlow(eq(USER_ID), any())).thenReturn(Map.of(
                CHECKING_ID, Map.of(AUG, money(HUNDRED), SEP, money("-50"), OCT, money("30")),
                SAVINGS_ID, Map.of(SEP, money("200")),
                CARD_ID, Map.of(SEP, money("-100"))));
        lenient().when(netWorthRepository.findIncomeExpense(eq(USER_ID), anyList(), any(DateWindow.class)))
                .thenReturn(new IncomeExpense(money("300"), money("150")));
        lenient().when(netWorthRepository.findExpense(eq(USER_ID), anyList(), any(DateWindow.class))).thenReturn(money("300"));
        lenient().when(netWorthRepository.findStoryFlows(eq(USER_ID), any(DateWindow.class))).thenReturn(Map.of(
                CHECKING_ID, new StoryFlows(money("130"), money("50"), money(ZERO))));
    }

    private NetWorthBreakdownDto run(NetWorthGroupBy groupBy, List<Long> accountIds, String typeCode, NetWorthCompare compare, YearMonth asOf) {
        return service.breakdown(USER_ID, AUG_1, TODAY, groupBy, accountIds, typeCode, compare, asOf, TODAY);
    }

    private NetWorthBreakdownDto run() {
        return run(NetWorthGroupBy.TYPE, List.of(), null, NetWorthCompare.NONE, null);
    }

    private void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, money(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }

    private NetWorthGroupDto group(NetWorthBreakdownDto dto, String key) {
        return dto.groups().stream().filter(g -> g.key().equals(key)).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("should equal the accounts page balance at the latest point")
    void shouldEqualTheAccountsPageBalanceAtTheLatestPoint() {
        // act
        NetWorthBreakdownDto dto = run(NetWorthGroupBy.ACCOUNT, List.of(), null, NetWorthCompare.NONE, null);

        // assert & verify -- october is in progress, so every account shows exactly its current balance
        assertMoney("1000", group(dto, "1").balance());
        assertMoney(FIVE_THOUSAND, group(dto, "2").balance());
        assertMoney(OWED_400, group(dto, "3").balance());
        assertTrue(dto.points().get(2).partial());
    }

    @Test
    @DisplayName("should list a point per month end with the hand-computed totals")
    void shouldListAPointPerMonthEndWithTheHandComputedTotals() {
        // act
        NetWorthBreakdownDto dto = run();

        // assert & verify
        List<NetWorthPointDto> points = dto.points();
        assertEquals(List.of(LocalDate.of(2026, 8, 31), LocalDate.of(2026, 9, 30), LocalDate.of(2026, 10, 31)),
                points.stream().map(NetWorthPointDto::date).toList());
        assertMoney("5820", points.get(0).assets());
        assertMoney("300", points.get(0).debts());
        assertMoney("5520", points.get(0).netWorth());
        assertMoney("5970", points.get(1).assets());
        assertMoney("5570", points.get(1).netWorth());
        assertMoney(SIX_THOUSAND, points.get(2).assets());
        assertMoney(FOUR_HUNDRED, points.get(2).debts());
        assertMoney("5720", dto.opening().assets());
        assertMoney("5420", dto.opening().netWorth());
    }

    @Test
    @DisplayName("should sum group balances to the totals")
    void shouldSumGroupBalancesToTheTotals() {
        // act
        NetWorthBreakdownDto dto = run();

        // assert & verify
        BigDecimal sum = dto.groups().stream().map(NetWorthGroupDto::balance).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertMoney("5600", sum);
        assertMoney("5600", dto.summary().netWorth());
    }

    @Test
    @DisplayName("should make assets minus debts equal net worth")
    void shouldMakeAssetsMinusDebtsEqualNetWorth() {
        // act
        NetWorthBreakdownDto dto = run();

        // assert & verify
        for (NetWorthPointDto point : dto.points()) {
            assertEquals(0, point.assets().subtract(point.debts()).compareTo(point.netWorth()));
        }
    }

    @Test
    @DisplayName("should count a negative credit card as a positive debt and an overpaid one as a negative debt")
    void shouldCountANegativeCreditCardAsAPositiveDebtAndAnOverpaidOneAsANegativeDebt() {
        // arrange -- the card is now overpaid: +50
        Account overpaid = account(CARD_ID, "Visa", CREDIT_CARD, false, "50", LONG_AGO);
        when(accountRepository.findAllByUserId(USER_ID)).thenReturn(List.of(overpaid));
        when(netWorthRepository.findMonthlyNetFlow(eq(USER_ID), any())).thenReturn(Map.of());

        // act
        NetWorthBreakdownDto dto = run();

        // assert & verify
        assertMoney("-50", dto.summary().debts());
        assertMoney("50", dto.summary().netWorth());
    }

    @Test
    @DisplayName("should leave an account out of months before it was created and give it null in its own series")
    void shouldLeaveAnAccountOutOfMonthsBeforeItWasCreatedAndGiveItNullInItsOwnSeries() {
        // arrange -- the card was opened on Sep 10
        Account youngCard = account(CARD_ID, "Visa", CREDIT_CARD, false, OWED_400, OffsetDateTime.of(2026, 9, 10, 0, 0, 0, 0, ZoneOffset.UTC));
        when(accountRepository.findAllByUserId(USER_ID)).thenReturn(List.of(checking, savings, youngCard));

        // act
        NetWorthBreakdownDto dto = run(NetWorthGroupBy.ACCOUNT, List.of(), null, NetWorthCompare.NONE, null);

        // assert & verify
        assertMoney(ZERO, dto.points().get(0).debts());
        assertMoney(FOUR_HUNDRED, dto.points().get(1).debts());
        List<BigDecimal> cardSeries = group(dto, "3").series();
        assertNull(cardSeries.get(0));
        assertMoney(OWED_400, cardSeries.get(1));
        assertMoney(ZERO, dto.opening().debts());
    }

    @Test
    @DisplayName("should split saved and other so they add up to the change")
    void shouldSplitSavedAndOtherSoTheyAddUpToTheChange() {
        // act
        NetWorthBreakdownDto dto = run();

        // assert & verify -- change 5600 - 5420 = 180; saved 300 - 150 = 150; other is the remainder 30
        assertMoney("180", dto.moved().change());
        assertMoney("150", dto.moved().saved());
        assertMoney("30", dto.moved().other());
        assertEquals(0, dto.moved().saved().add(dto.moved().other()).compareTo(dto.moved().change()));
    }

    @Test
    @DisplayName("should keep saved plus other equal to the change with an account filter")
    void shouldKeepSavedPlusOtherEqualToTheChangeWithAnAccountFilter() {
        // act
        NetWorthBreakdownDto dto = run(NetWorthGroupBy.TYPE, List.of(CHECKING_ID), null, NetWorthCompare.NONE, null);

        // assert & verify -- checking alone: 1000 - 920
        assertMoney("80", dto.moved().change());
        assertEquals(0, dto.moved().saved().add(dto.moved().other()).compareTo(dto.moved().change()));
        verify(netWorthRepository).findIncomeExpense(eq(USER_ID), eq(List.of(CHECKING_ID)), any(DateWindow.class));
    }

    @Test
    @DisplayName("should ask for the whole months of the range so the story and the change cover the same days")
    void shouldAskForTheWholeMonthsOfTheRangeSoTheStoryAndTheChangeCoverTheSameDays() {
        // act
        service.breakdown(USER_ID, LocalDate.of(2026, 8, 15), LocalDate.of(2026, 10, 3), NetWorthGroupBy.TYPE, List.of(), null, NetWorthCompare.NONE, null, TODAY);

        // assert & verify -- august 1 to the end of october, even though the dates were mid-month
        verify(netWorthRepository).findIncomeExpense(eq(USER_ID), anyList(), eq(new DateWindow(
                LocalDate.of(2026, 8, 1).atStartOfDay().atOffset(ZoneOffset.UTC),
                LocalDate.of(2026, 11, 1).atStartOfDay().atOffset(ZoneOffset.UTC))));
    }

    @Test
    @DisplayName("should make the story close the gap between opening and closing balance")
    void shouldMakeTheStoryCloseTheGapBetweenOpeningAndClosingBalance() {
        // act
        NetWorthBreakdownDto dto = run(NetWorthGroupBy.ACCOUNT, List.of(), null, NetWorthCompare.NONE, null);

        // assert & verify -- checking: opening 920, closing 1000; deposits 130 - withdrawals 50 + other 0 = 80
        var story = group(dto, "1").story();
        assertMoney("920", story.openingBalance());
        assertMoney("1000", story.closingBalance());
        assertEquals(0, story.closingBalance().subtract(story.openingBalance())
                .compareTo(story.deposits().subtract(story.withdrawals()).add(story.interestAndAdjustments())));
        assertNull(group(run(), CHECKING).story());
    }

    @Test
    @DisplayName("should total liquid cash from checking, savings and cash only")
    void shouldTotalLiquidCashFromCheckingSavingsAndCashOnly() {
        // arrange
        Account cash = account(4L, "Wallet", CASH, true, "20", LONG_AGO);
        Account brokerage = account(5L, "Brokerage", "INVESTMENT", true, "9000", LONG_AGO);
        when(accountRepository.findAllByUserId(USER_ID)).thenReturn(List.of(checking, savings, card, cash, brokerage));

        // act
        NetWorthBreakdownDto dto = run();

        // assert & verify -- 1000 + 5000 + 20; the card and the brokerage are not liquid
        assertMoney("6020", dto.summary().liquidCash());
    }

    @Test
    @DisplayName("should divide liquid cash by the average of the last three months' spending")
    void shouldDivideLiquidCashByTheAverageOfTheLastThreeMonthsSpending() {
        // act
        NetWorthBreakdownDto dto = run();

        // assert & verify -- 300 spent over three months is 100 a month; 6000 liquid covers 60 months
        assertMoney(HUNDRED, dto.summary().averageMonthlySpending());
        assertMoney("60", dto.summary().monthsCovered());
        verify(netWorthRepository).findExpense(eq(USER_ID), anyList(), eq(new DateWindow(
                LocalDate.of(2026, 8, 1).atStartOfDay().atOffset(ZoneOffset.UTC),
                LocalDate.of(2026, 11, 1).atStartOfDay().atOffset(ZoneOffset.UTC))));
    }

    @Test
    @DisplayName("should return null months covered when spending is zero")
    void shouldReturnNullMonthsCoveredWhenSpendingIsZero() {
        // arrange
        when(netWorthRepository.findExpense(eq(USER_ID), anyList(), any(DateWindow.class))).thenReturn(BigDecimal.ZERO);

        // act
        NetWorthBreakdownDto dto = run();

        // assert & verify
        assertNull(dto.summary().monthsCovered());
    }

    @Test
    @DisplayName("should compare a year ago with Feb 29 mapped to Feb 28")
    void shouldCompareAYearAgoWithFeb29MappedToFeb28() {
        // act
        NetWorthBreakdownDto dto = service.breakdown(USER_ID, LocalDate.of(2028, 3, 1), LocalDate.of(2028, 3, 31), NetWorthGroupBy.TYPE,
                List.of(), null, NetWorthCompare.YEAR_AGO, null, LocalDate.of(2028, 4, 5));

        // assert & verify
        assertEquals("a year ago", dto.comparison().label());
        assertEquals(LocalDate.of(2027, 3, 1), dto.comparison().start());
        assertEquals(LocalDate.of(2027, 3, 31), dto.comparison().end());
        LocalDate leapEnd = service.breakdown(USER_ID, LocalDate.of(2028, 2, 1), LocalDate.of(2028, 2, 29), NetWorthGroupBy.TYPE,
                List.of(), null, NetWorthCompare.YEAR_AGO, null, LocalDate.of(2028, 4, 5)).comparison().end();
        assertEquals(LocalDate.of(2027, 2, 28), leapEnd);
    }

    @Test
    @DisplayName("should compare the previous period of the same length ending the day before the range")
    void shouldCompareThePreviousPeriodOfTheSameLengthEndingTheDayBeforeTheRange() {
        // act
        NetWorthBreakdownDto dto = service.breakdown(USER_ID, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), NetWorthGroupBy.TYPE,
                List.of(), null, NetWorthCompare.PREVIOUS_PERIOD, null, TODAY);

        // assert & verify -- 30 days: Aug 2 to Aug 31
        assertEquals("the previous period", dto.comparison().label());
        assertEquals(LocalDate.of(2026, 8, 2), dto.comparison().start());
        assertEquals(LocalDate.of(2026, 8, 31), dto.comparison().end());
        assertMoney("5520", dto.comparison().netWorth());
        assertMoney("5520", group(dto, CHECKING).comparisonBalance().add(group(dto, SAVINGS).comparisonBalance()).add(group(dto, CREDIT_CARD).comparisonBalance()));
    }

    @Test
    @DisplayName("should return no comparison when not comparing")
    void shouldReturnNoComparisonWhenNotComparing() {
        // act
        NetWorthBreakdownDto dto = run();

        // assert & verify
        assertNull(dto.comparison());
        assertNull(group(dto, CHECKING).comparisonBalance());
    }

    @Test
    @DisplayName("should drill into one type's accounts")
    void shouldDrillIntoOneTypesAccounts() {
        // act
        NetWorthBreakdownDto dto = run(NetWorthGroupBy.ACCOUNT, List.of(), SAVINGS, NetWorthCompare.NONE, null);

        // assert & verify
        assertEquals(List.of("2"), dto.groups().stream().map(NetWorthGroupDto::key).toList());
        assertMoney(FIVE_THOUSAND, dto.summary().netWorth());
    }

    @Test
    @DisplayName("should take the summary and group balances at the asOf month")
    void shouldTakeTheSummaryAndGroupBalancesAtTheAsOfMonth() {
        // act
        NetWorthBreakdownDto dto = run(NetWorthGroupBy.TYPE, List.of(), null, NetWorthCompare.NONE, SEP);

        // assert & verify -- september: 970 + 5000 - 400
        assertEquals(LocalDate.of(2026, 9, 30), dto.asOf());
        assertMoney("5570", dto.summary().netWorth());
        assertMoney("970", group(dto, CHECKING).balance());
        assertMoney("920", group(dto, CHECKING).previousBalance());
    }

    @Test
    @DisplayName("should order assets first, largest first, then debts")
    void shouldOrderAssetsFirstLargestFirstThenDebts() {
        // act
        NetWorthBreakdownDto dto = run();

        // assert & verify
        assertEquals(List.of(SAVINGS, CHECKING, CREDIT_CARD), dto.groups().stream().map(NetWorthGroupDto::key).toList());
        assertEquals(List.of(ASSET, ASSET, DEBT), dto.groups().stream().map(NetWorthGroupDto::kind).toList());
    }

    @Test
    @DisplayName("should handle no accounts without asking the repository for anyone's totals")
    void shouldHandleNoAccountsWithoutAskingTheRepositoryForAnyonesTotals() {
        // arrange
        when(accountRepository.findAllByUserId(USER_ID)).thenReturn(List.of());

        // act
        NetWorthBreakdownDto dto = run();

        // assert & verify -- an empty account list must not mean "all accounts" to the repository
        assertTrue(dto.groups().isEmpty());
        assertMoney(ZERO, dto.summary().netWorth());
        assertMoney(ZERO, dto.moved().change());
        verify(netWorthRepository, never()).findIncomeExpense(any(), anyList(), any());
        verify(netWorthRepository, never()).findExpense(any(), anyList(), any());
    }

    @Test
    @DisplayName("should handle only debts")
    void shouldHandleOnlyDebts() {
        // arrange
        when(accountRepository.findAllByUserId(USER_ID)).thenReturn(List.of(card));

        // act
        NetWorthBreakdownDto dto = run();

        // assert & verify
        assertMoney(OWED_400, dto.summary().netWorth());
        assertMoney(FOUR_HUNDRED, dto.summary().debts());
        assertMoney(ZERO, dto.summary().assets());
        assertEquals(List.of(DEBT), dto.groups().stream().map(NetWorthGroupDto::kind).toList());
    }

    @Test
    @DisplayName("should reject a reversed range, an unknown type and an asOf outside the range")
    void shouldRejectAReversedRangeAnUnknownTypeAndAnAsOfOutsideTheRange() {
        // act & assert & verify
        assertThrows(IllegalArgumentException.class, () -> service.breakdown(USER_ID, TODAY, AUG_1, NetWorthGroupBy.TYPE,
                List.of(), null, NetWorthCompare.NONE, null, TODAY));
        assertThrows(IllegalArgumentException.class, () -> run(NetWorthGroupBy.ACCOUNT, List.of(), "NOPE", NetWorthCompare.NONE, null));
        assertThrows(IllegalArgumentException.class, () -> run(NetWorthGroupBy.TYPE, List.of(), null, NetWorthCompare.NONE, YearMonth.of(2025, 1)));
    }
}
