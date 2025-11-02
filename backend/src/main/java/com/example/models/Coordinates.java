package com.example.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Data
@NoArgsConstructor
public class Coordinates {
    @JacksonXmlProperty
    private long x;
    @JacksonXmlProperty
    private long y;
}
