package com.mayureshpatel.pfdataservice.dto.transaction;

/**
 * The outcome of committing one file within a bulk import: it was saved, it was skipped because
 * the exact same file was already imported, or it failed for any other reason. Each file's
 * outcome is independent of the others in the same request.
 */
public enum FileImportStatus {
    IMPORTED,
    SKIPPED_ALREADY_IMPORTED,
    FAILED
}
