package com.ids.ms_products.dto;

public class ImportErrorDTO {
    private long fila;
    private String mensaje;

    public ImportErrorDTO(long fila, String mensaje) {
        this.fila = fila;
        this.mensaje = mensaje;
    }

    public long getFila() {
        return fila;
    }

    public String getMensaje() {
        return mensaje;
    }
}
