package com.storechain.group4.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Bảng chấm công của nhân viên: mỗi nhân viên chỉ có 1 bản ghi chấm công mỗi ngày.
 */
@Entity
@Table(name = "employee_workdays",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "work_date"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeWorkday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "workday_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "check_in_at", nullable = false)
    private LocalDateTime checkInAt;

    @Column(name = "note", length = 255)
    private String note;

    @PrePersist
    protected void onCreate() {
        if (this.checkInAt == null) {
            this.checkInAt = LocalDateTime.now();
        }
    }
}
