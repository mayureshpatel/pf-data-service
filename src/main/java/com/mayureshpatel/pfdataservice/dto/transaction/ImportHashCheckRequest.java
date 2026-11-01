package com.mayureshpatel.pfdataservice.dto.transaction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * One (account, file hash) pair to check against the import history. Used both as the request
 * item and as the response item of the import-check endpoint, where the response contains only
 * the pairs whose file was already imported.
 *
 * @param accountId the account the file would be imported into
 * @param fileHash  the file's SHA-256 hash, computed client-side
 */
public record ImportHashCheckRequest(
        @NotNull(message = "Account id cannot be null")
        Long accountId,

        @NotBlank(message = "File hash cannot be blank")
        String fileHash
) {
}
