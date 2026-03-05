//package com.mayureshpatel.pfdataservice.repository.currency;
//
//import com.mayureshpatel.pfdataservice.BaseIntegrationTest;
//import com.mayureshpatel.pfdataservice.domain.currency.Currency;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@Transactional
//@DisplayName("CurrencyRepository Integration Tests")
//class CurrencyRepositoryTest extends BaseIntegrationTest {
//
//    @Autowired
//    private CurrencyRepository currencyRepository;
//
//    @Test
//    @DisplayName("save() should upsert currency")
//    void save_shouldUpsertCurrency() {
//        Currency currency = new Currency("TST", "Test Dollar", "T$", true, new CreatedAtAudit());
//
//        Currency saved = currencyRepository.save(currency);
//
//        assertThat(saved.getCode()).isEqualTo("TST");
//    }
//
//    @Test
//    @DisplayName("findById() should return currency by code")
//    void findById_shouldReturnCurrency() {
//        currencyRepository.save(new Currency("XYZ", "XYZ Currency", "X", true, new CreatedAtAudit()));
//
//        Optional<Currency> found = currencyRepository.findById("XYZ");
//
//        assertThat(found).isPresent();
//        assertThat(found.get().getName()).isEqualTo("XYZ Currency");
//        assertThat(found.get().getSymbol()).isEqualTo("X");
//    }
//
//    @Test
//    @DisplayName("findById() should return empty for nonexistent code")
//    void findById_shouldReturnEmpty() {
//        assertThat(currencyRepository.findById("NOPE")).isEmpty();
//    }
//
//    @Test
//    @DisplayName("findAll() should return all currencies")
//    void findAll_shouldReturnAll() {
//        currencyRepository.save(new Currency("AA1", "Alpha", "A", true, new CreatedAtAudit()));
//        currencyRepository.save(new Currency("BB2", "Beta", "B", true, new CreatedAtAudit()));
//
//        List<Currency> all = currencyRepository.findAll();
//
//        assertThat(all).hasSizeGreaterThanOrEqualTo(2);
//    }
//
//    @Test
//    @DisplayName("findByIsActive() should return only active currencies")
//    void findByIsActive_shouldReturnActive() {
//        currencyRepository.save(new Currency("ACT", "Active", "$", true, new CreatedAtAudit()));
//        currencyRepository.save(new Currency("INA", "Inactive", "X", false, new CreatedAtAudit()));
//
//        List<Currency> active = currencyRepository.findByIsActive();
//
//        assertThat(active).extracting(Currency::getCode).contains("ACT");
//        assertThat(active).extracting(Currency::getCode).doesNotContain("INA");
//    }
//
//    @Test
//    @DisplayName("existsById() should return true when currency exists")
//    void existsById_shouldReturnTrue() {
//        currencyRepository.save(new Currency("EXS", "Exists", "$", true, new CreatedAtAudit()));
//
//        assertThat(currencyRepository.existsById("EXS")).isTrue();
//    }
//
//    @Test
//    @DisplayName("existsById() should return false when currency does not exist")
//    void existsById_shouldReturnFalse() {
//        assertThat(currencyRepository.existsById("NOPE")).isFalse();
//    }
//
//    @Test
//    @DisplayName("deleteById() should remove currency")
//    void deleteById_shouldRemoveCurrency() {
//        currencyRepository.save(new Currency("DEL", "Delete", "$", true, new CreatedAtAudit()));
//
//        currencyRepository.deleteById("DEL");
//
//        assertThat(currencyRepository.findById("DEL")).isEmpty();
//    }
//}
