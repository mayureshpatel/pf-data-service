package com.mayureshpatel.pfdataservice.dto.transaction;

/**
 * The per-file result of a bulk import, returned in request order so a client can show each
 * file's outcome separately instead of one all-or-nothing message.
 *
 * @param fileName          the source filename the request carried (may be null)
 * @param status            whether the file was imported, skipped as already imported, or failed
 * @param savedCount        how many transactions were actually saved (0 unless imported)
 * @param duplicateRowCount how many rows were skipped as duplicates of existing or same-batch rows
 * @param error             the failure message when {@code status} is FAILED, otherwise null
 */
public record FileImportResultDto(String fileName,
                                  FileImportStatus status,
                                  int savedCount,
                                  int duplicateRowCount,
                                  String error) {
}
