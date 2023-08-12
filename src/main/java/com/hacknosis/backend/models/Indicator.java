package com.hacknosis.backend.models;

import java.util.Arrays;

public enum Indicator {
    AggressiveViolent,
    AntibioticResistantDisease,
    C_Difficile,
    CandidaAU,
    ChildWelfareAlert,
    ContactMDRO,
    ContainsSealedInformation,
    CPE,
    CPEExposure,
    DifficultToIntubate,
    ESBL,
    FallPrecaution,
    H_AlertForBehavioralCare,
    H_PseudocholinesteraseDeficie,
    HearingAndVisionAssistance,
    HospicePatient,
    HosoitalOutOfCountry,
    InfectionPrecaution,
    InterpreterNeeded,
    IPAC_MSH,
    Isolation,
    M_AlertForBehavioraCare,
    M_COVID_19Exposure,
    M_COVID_19Positive,
    M_COVID_19Suspected,
    MalignantHyperthermia,
    MDRO,
    MRSA,
    MRSAExposure,
    PalliativeCarePatient,
    S_AlertForBehavioralCare,
    S_PseudocholinesteraseDeficie,
    SubstanceMisuseRisk,
    VRE,
    VREExposure;

    public static boolean validIndicator(Indicator v) {
        return Arrays.stream(Indicator.values()).anyMatch((e) -> e.name().equals(v.name()));
    }
}
