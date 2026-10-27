package com.gym.routine;

/** Tipo de entrenamiento al que pertenece una rutina. */
public enum Discipline {
    GYM("Gym"), CROSSFIT("CrossFit");

    private final String label;

    Discipline(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
