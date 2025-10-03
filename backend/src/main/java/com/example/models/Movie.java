package com.example.models;

import com.example.models.enums.MovieGenre;
import com.example.models.enums.MpaaRating;
import jakarta.persistence.*;
import java.time.ZonedDateTime;
import lombok.Data;

@Data
@Entity
@Table(name = "movies")
public class Movie {

    // ID: not null, > 0, unique, auto-generated
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Name: not null, not empty
    // @NotBlank // <-- Bean Validation для проверки на пустоту
    @Column(nullable = false)
    private String name;

    // CreationDate: not null, auto-generated
    @Column(name = "creation_date", nullable = false)
    private ZonedDateTime creationDate;

    // OscarsCount: > 0, not null
    // @NotNull @Min(1) // <-- Bean Validation для not null и > 0
    @Column(name = "oscars_count", nullable = false)
    private Integer oscarsCount;

    // Budget: > 0, not null
    // @NotNull @Positive // <-- Bean Validation для not null и > 0
    @Column(nullable = false)
    private Float budget;

    // TotalBoxOffice: может быть null, > 0
    // @Positive // <-- Bean Validation для > 0
    @Column(name = "total_box_office")
    private Long totalBoxOffice;

    // Length: может быть null, > 0
    // @Positive // <-- Bean Validation для > 0
    private Long length;

    // GoldenPalmCount: > 0 (long в Java не может быть null, поэтому только проверка > 0)
    // @Min(1) // <-- Bean Validation для > 0
    @Column(name = "golden_palm_count", nullable = false)
    private long goldenPalmCount;

    // --- Встроенный Объект ---

    // Coordinates: not null. @Embedded для встраивания полей в эту таблицу.
    @Embedded
    // Используем @AttributeOverride, чтобы явно указать, что колонки не могут быть null
    @AttributeOverrides({
        @AttributeOverride(name = "x", column = @Column(name = "coord_x", nullable = false)),
        @AttributeOverride(name = "y", column = @Column(name = "coord_y", nullable = false))
    })
    private Coordinates coordinates; // Подразумевается, что Coordinates помечен как @Embeddable

    // --- Перечисления ---

    // MpaaRating: not null
    @Enumerated(EnumType.STRING)
    @Column(name = "mpaa_rating", nullable = false)
    private MpaaRating mpaaRating;

    // Genre: not null
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovieGenre genre;

    // Director: может быть null. Используем ManyToOne для ссылки на сущность Person.
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "director_id") // Создаст внешний ключ 'director_id' в таблице 'movies'
    private Person director; // nullable = true по умолчанию

    // Screenwriter: может быть null.
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "screenwriter_id") // Создаст внешний ключ 'screenwriter_id'
    private Person screenwriter;

    // Operator: not null. Явно указываем, что колонка внешнего ключа не может быть null.
    @ManyToOne(cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "operator_id", nullable = false) // Создаст внешний ключ 'operator_id'
    private Person operator;

    // --- Методы Жизненного Цикла JPA ---

    // Установка creationDate перед сохранением
    @PrePersist
    protected void onCreate() {
        if (creationDate == null) {
            creationDate = ZonedDateTime.now();
        }
    }
}
