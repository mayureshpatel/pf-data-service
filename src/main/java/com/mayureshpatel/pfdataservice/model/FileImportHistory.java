package com.mayureshpatel.pfdataservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "file_import_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileImportHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long accountId;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String fileHash;

    @Column(nullable = false)
    private int transactionCount;

    @CreationTimestamp
    private LocalDateTime importedAt;
}
