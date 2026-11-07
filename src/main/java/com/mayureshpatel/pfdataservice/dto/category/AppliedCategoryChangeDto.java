package com.mayureshpatel.pfdataservice.dto.category;

/**
 * One transaction whose category a create-and-apply call changed, with the category it had
 * before, so the change can be undone (PF-922).
 *
 * @param transactionId       the changed transaction
 * @param previousCategoryId  the category it had before, null if it had none
 */
public record AppliedCategoryChangeDto(Long transactionId, Long previousCategoryId) {
}
