package com.shelter.model;

public enum TipoRecurso {
    AGUA(true),
    ALIMENTO(true),
    MEDICINA(true),
    ABRIGO(false),
    HIGIENE(false);

    private final boolean critico;

    TipoRecurso(boolean critico) {
        this.critico = critico;
    }

    // AGUA, ALIMENTO y MEDICINA son recursos críticos
    public boolean esCritico() {
        return critico;
    }
}
