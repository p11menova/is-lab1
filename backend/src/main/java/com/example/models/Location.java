package com.example.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.persistence.Embeddable;
import lombok.*;

@Data
@NoArgsConstructor
@Embeddable
public class Location {
    @JacksonXmlProperty
    private Integer x;
    @JacksonXmlProperty
    private Double y;
    @JacksonXmlProperty
    private float z;
}
