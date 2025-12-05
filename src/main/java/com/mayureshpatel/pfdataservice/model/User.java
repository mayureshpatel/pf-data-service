package com.mayureshpatel.pfdataservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "last_updated_by", nullable = false)
    private String lastUpdatedBy;

    @UpdateTimestamp
    @Column(name = "last_updated_timestamp")
    private LocalDateTime lastUpdatedTimestamp;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdTimestamp;
}
