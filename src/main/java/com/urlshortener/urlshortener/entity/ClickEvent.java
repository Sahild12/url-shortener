package com.urlshortener.urlshortener.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "click_events",
       indexes = @Index(name = "idx_click_short_code", columnList = "short_code"))
@Getter
@Setter
public class ClickEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "short_code", nullable = false)
    private String shortCode;

    private LocalDateTime clickedAt;

    private String browser;

    private String device;
}