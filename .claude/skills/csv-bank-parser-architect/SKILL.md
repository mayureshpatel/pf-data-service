---
name: csv-bank-parser-architect
description: Generates a new bank-specific CSV transaction parser (a TransactionParser implementation). Trigger this skill when adding support for importing a new bank's CSV export format.
---

# CSV Bank Parser Architect

This skill dictates the exact procedural steps and constraints for adding a new bank's CSV import
format to this backend.

## 🚨 Architectural Constraints
- **One `BankName` enum constant per bank.** `BankName` is a deliberate enum-per-bank design at
  the current scale (5 banks), not a placeholder for a lookup table (PF-310) — add a new constant,
  don't build a registration mechanism.
- **One `@Component implements TransactionParser` per bank**, indexed automatically by
  `TransactionParserFactory` via `getBankName()` — no manual registration beyond the `@Component`
  annotation and the interface method.
- **Every date must resolve to UTC**, never a real timezone. Use the interface's own
  `parseDate(String, DateTimeFormatter)` default method with a formatter that resolves to UTC
  (`parseDefaulting(ChronoField.OFFSET_SECONDS, 0)`, never `.withZone(ZoneId.of(...))` with a real
  zone) — see the Gotchas below; this exact mistake has shipped twice.
- **The frontend needs a matching entry too.** `pf-ui`'s `csv-import-dialog.component.ts`'s
  `bankOptions` array is a hardcoded list — a new `BankName`/parser is invisible to real users
  until it's added there as well. Easy to miss since it's a different repo.

## 🛠 Procedural Workflow
1. **Get a real sample export.** Every existing parser's format was built against an actual
   downloaded file from that bank (see `src/test/resources/sample-imports/`) — don't guess a
   plausible-looking header/date shape.
2. **Add the `BankName` enum constant** (`domain/bank/BankName.java`) with its display name.
3. **Implement `TransactionParser`** as a new `@Component` class in `service/parser/`, named
   `<Bank>CsvParser`. Copy `CapitalOneCsvParser` (`references/parser-gold-source.java`) as the
   starting shape — confirmed the cleanest of the 5 existing parsers (PF-EPIC-014's bug hunt found
   real bugs in 2 of the other 4, none in this one). Reuse the interface's default `parseDate`/
   `parseAmount`/`configureTransactionTypeAndAmount`/`configureCreditCardTransactionTypeAndAmount`
   helpers rather than reimplementing them.
4. **Register the new option in `pf-ui`**: add an entry to `csv-import-dialog.component.ts`'s
   `bankOptions` array (label, `BankName` value, a short description of the expected format).
5. **Write a parser test** mirroring `CapitalOneCsvParserTest`'s shape, covering at minimum: a
   valid row, the amount-sign/zero-value convention this specific bank uses, and the date format.
6. **Verify against the real sample file**, not just synthetic test strings.

## ⚠️ Gotchas
This parser family has a real, repeated bug history — see
`docs/technical/02-backend-spring/csv-bank-parsers.md` for the full writeups. The 3 patterns to
actively check a new parser against before shipping:
- **Timezone.** A `DateTimeFormatter` with `.withZone(ZoneId.of("America/..."))` or any real zone
  will silently shift every date it parses. `DiscoverCsvParser` shipped hardcoded to
  `America/New_York` for a long time before this was caught (PF-197). Resolve to UTC via
  `parseDefaulting(ChronoField.OFFSET_SECONDS, 0)` instead.
- **Amount handling across format variations.** A header that's *entirely missing* from the
  file's header row is a structural problem (the bank changed its export) and should throw, not
  silently resolve to zero — `parseAmount()`'s default implementation already does this correctly;
  don't bypass it with hand-rolled column access. `DiscoverCsvParser`'s pre-2022 format silently
  zeroed every transaction before this was fixed (PF-198).
- **Date-pattern alternatives must trace to a real export.** A `DateTimeFormatterBuilder`
  bracketed-alternative pattern that doesn't match anything just sits inert rather than failing
  loudly — `SynovusCsvParser` shipped with one that had a typo (missing `/`) for a while (PF-214).
  Verify every pattern alternative against an actual sample file, not a guess.
- **`StandardCsvParser`'s "generic" format has no real bank behind it** — it's this app's own
  format, advertised to users simply as "Generic format (Date, Description, Amount)", not a real
  bank export. Its date parsing accepts a plain ISO date as well as a full offset-date-time
  (PF-311) precisely because a real user, not a bank, produces this file — a useful reminder that
  "who actually produces this CSV" changes what a reasonable input looks like.

## 📚 References
- [Parser Gold-Source](references/parser-gold-source.java) — `CapitalOneCsvParser`, copied
  verbatim. Confirmed the cleanest of the 5 existing parsers; start here, not from a blank file.
