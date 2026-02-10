package com.cirm.platform.complaint.entity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import com.cirm.platform.complaint.entity.enums.ComplaintStatus;


@Entity
@Table(name = "complaints")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private String department;
    private String priority;
    
    @Enumerated(EnumType.STRING)
    private ComplaintStatus status;


    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }   
}
