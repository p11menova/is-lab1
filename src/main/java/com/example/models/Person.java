package com.example.models;

import com.example.models.enums.Color;
import com.example.models.enums.Country;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "persons")
@Data
@NoArgsConstructor
public class Person {
    // Первичный ключ (ID) - обязателен для любой JPA-сущности
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 1. Name: not null, not empty (JPA гарантирует только NOT NULL)
    // @NotBlank // <-- Используй это, если включен Bean Validation
    @Column(nullable = false)
    private String name;

    // 2. EyeColor: может быть null
    @Enumerated(EnumType.STRING)
    @Column(name = "eye_color") // nullable = true по умолчанию
    private Color eyeColor;

    // 3. HairColor: not null
    // @NotNull // <-- Используй это, если включен Bean Validation
    @Enumerated(EnumType.STRING)
    @Column(name = "hair_color", nullable = false)
    private Color hairColor;

    // 4. Location: not null. Используем @Embedded
    @Embedded
    // Используем @AttributeOverrides для настройки колонок, которые будут созданы для встроенного
    // класса
    @AttributeOverrides({
        @AttributeOverride(name = "x", column = @Column(name = "loc_x", nullable = false)),
        @AttributeOverride(name = "y", column = @Column(name = "loc_y", nullable = false)),
        @AttributeOverride(name = "z", column = @Column(name = "loc_z", nullable = false)),
    })
    private Location
            location; // nullable = false по умолчанию, но нужно явно указать для вложенных полей

    // 5. Birthday: может быть null
    @Column(name = "birthday")
    private LocalDateTime birthday; // nullable = true по умолчанию

    // 6. Nationality: может быть null
    @Enumerated(EnumType.STRING)
    @Column(name = "nationality")
    private Country nationality; // nullable = true по умолчанию
}
