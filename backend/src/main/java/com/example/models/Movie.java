package com.example.models;

import com.example.models.enums.MovieGenre;
import com.example.models.enums.MpaaRating;
import jakarta.json.bind.annotation.JsonbDateFormat;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@Entity
@Table(name = "movies")
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "creation_date", nullable = false)
    @JsonbDateFormat("yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime creationDate;

    @Column(name = "oscars_count", nullable = false)
    private Integer oscarsCount;

    @Column(nullable = false)
    private Float budget;

    @Column(name = "total_box_office")
    private Long totalBoxOffice;

    private Long length;

    @Column(name = "golden_palm_count", nullable = false)
    private long goldenPalmCount;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "x", column = @Column(name = "coord_x", nullable = false)),
        @AttributeOverride(name = "y", column = @Column(name = "coord_y", nullable = false))
    })
    private Coordinates coordinates;

    @Enumerated(EnumType.STRING)
    @Column(name = "mpaa_rating", nullable = false)
    private MpaaRating mpaaRating;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovieGenre genre;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "director_id")
    private Person director;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "screenwriter_id")
    private Person screenwriter;

    @ManyToOne(cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "operator_id", nullable = false)
    private Person operator;

    @PrePersist
    protected void onCreate() {
        if (creationDate == null) {
            creationDate = LocalDateTime.now();
        }
    }
}
