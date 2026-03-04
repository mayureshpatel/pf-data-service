package com.mayureshpatel.pfdataservice.dto.transaction.fileimport;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder(toBuilder = true)
@ToString
public class FileImportCreateRequest {

    private final String accountId;
    private final String fileName;
    private final String fileHash;
    private final String fileContent;
}
