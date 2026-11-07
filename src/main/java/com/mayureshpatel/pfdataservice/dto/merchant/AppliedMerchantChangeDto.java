package com.mayureshpatel.pfdataservice.dto.merchant;

/**
 * One transaction whose merchant a create-and-apply call changed, with the merchant it had
 * before, so the change can be undone (PF-922).
 *
 * @param transactionId       the changed transaction
 * @param previousMerchantId  the merchant it had before, null if it had none
 */
public record AppliedMerchantChangeDto(Long transactionId, Long previousMerchantId) {
}
