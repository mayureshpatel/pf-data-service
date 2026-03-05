package com.mayureshpatel.pfdataservice.dto.transaction.fileimport;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder(toBuilder = true)
@ToString
public class FileImportCreateRequest {

    @NotBlank(message = "Account ID cannot be blank")
    private final String accountId;

    @NotBlank(message = "File name cannot be blank")
    @Size(max = 255, message = "File name must be less than 255 characters")
    private final String fileName;

    @NotBlank(message = "File hash cannot be blank")
    @Size(max = 64, message = "File hash must be less than 64 characters")
    private final String fileHash;

    @NotBlank(message = "File content cannot be blank")
    private final String fileContent;
}
