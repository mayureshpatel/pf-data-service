package com.mayureshpatel.pfdataservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "category_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String keyword;

    @Column(name = "category_name", nullable = false, length = 50)
    private String categoryName;

    @Column(nullable = false)
    private Integer priority;
}
