package com.hacknosis.backend.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "patients")
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;

    @NotNull(message = "Name cannot be null")
    String name;

    @NotNull(message = "Patient summary cannot be null")
    String summary;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull(message = "Age cannot be null")
    private Integer age;

    @NotNull(message = "Birth Sex must be specified")
    private Character sex;

    @ElementCollection
    private List<String> allergies = new ArrayList<>();

    @NotNull(message = "Room number cannot be null")
    private long room;

    @NotNull(message = "Resus Status cannot be null")
    @Enumerated(EnumType.STRING)
    private ResusStatus resusStatus;

    @NotNull(message = "Special Indicators cannot be null")
    @ElementCollection
    private Set<Indicator> specialIndicators;

    @NotNull(message = "Appointments cannot be null")
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL)
    private List<Appointment> appointments = new ArrayList<>();
}
