package com.hacknosis.backend.models;

import java.util.Arrays;

public enum ResusStatus {
    FullResus,
    CriticalCare,
    MedicalCare,
    ComfortCare;

    public static boolean validResusStatus(ResusStatus v) {
        return Arrays.stream(ResusStatus.values()).anyMatch((e) -> e.name().equals(v.name()));
    }
}
