package com.minesafety.entity;

import com.minesafety.enums.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "mines")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Mine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mine_code", nullable = false, unique = true, length = 50)
    private String mineCode;

    @Column(name = "mine_name", nullable = false, length = 150)
    private String mineName;

    @Column(nullable = false, length = 150)
    private String location;

    @Column(length = 100)
    private String district;

    @Column(length = 100)
    private String state;

    @Column(name = "total_area")
    private Double totalArea;

    @Column
    private Double depth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
