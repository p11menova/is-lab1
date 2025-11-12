package com.example.models;

import com.example.models.enums.Color;
import com.example.models.enums.Country;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "persons")
@Data
@NoArgsConstructor
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "eye_color")
    private Color eyeColor;

    @Enumerated(EnumType.STRING)
    @Column(name = "hair_color", nullable = false)
    private Color hairColor;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "x", column = @Column(name = "loc_x", nullable = false)),
        @AttributeOverride(name = "y", column = @Column(name = "loc_y", nullable = false)),
        @AttributeOverride(name = "z", column = @Column(name = "loc_z", nullable = false)),
    })
    private Location location;

    @Column(name = "birthday")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime birthday;

    @Enumerated(EnumType.STRING)
    @Column(name = "nationality")
    private Country nationality;
}
