package com.example.backend.client;

public enum Activity {
    OWN_ADMINISTRATION("Egen administration"),
    VACATION("Semester"),
    SICK_LEAVE("Sjuk"),
    SICK_CHILD_CARE("VAB"),
    PARENTAL_LEAVE("Föräldraledig"),
    LEAVE_OF_ABSENCE("Tjänstledig"),
    CONSULTANCY_TIME("Konsult-tid"),
    ON_CALL("Jourtid");
    public final String activity;
    private Activity(String activity) {
        this.activity = activity;
    }
}
