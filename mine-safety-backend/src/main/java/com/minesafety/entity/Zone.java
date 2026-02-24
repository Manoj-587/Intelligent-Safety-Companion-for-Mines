package com.minesafety.entity;

import com.minesafety.enums.Status;
import com.minesafety.enums.ZoneType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "zones")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Zone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "zone_code", nullable = false, unique = true, length = 50)
    private String zoneCode;

    @Column(name = "zone_name", nullable = false, length = 150)
    private String zoneName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mine_id", nullable = false)
    private Mine mine;

    @Enumerated(EnumType.STRING)
    @Column(name = "zone_type", nullable = false)
    private ZoneType zoneType = ZoneType.GENERAL;

    @Column(name = "max_safe_temperature")
    private Double maxSafeTemperature;

    @Column(name = "min_safe_oxygen")
    private Double minSafeOxygen;

    @Column(name = "max_safe_methane")
    private Double maxSafeMethane;

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
