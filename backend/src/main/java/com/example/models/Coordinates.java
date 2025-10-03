package com.example.models;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Data
@NoArgsConstructor
public class Coordinates {
    private long x;
    private long y;
}
