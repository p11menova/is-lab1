package com.example.models;

import jakarta.persistence.Embeddable;
import lombok.*;

@Data
@NoArgsConstructor
@Embeddable
public class Location {
    private Integer x;
    private Double y;
    private float z;
}
