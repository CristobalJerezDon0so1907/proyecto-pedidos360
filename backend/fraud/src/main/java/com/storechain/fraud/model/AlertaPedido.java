package com.storechain.fraud.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Evento de SALIDA del pipeline (topic: pedidos-alertas).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AlertaPedido {
    private String pedidoId;
    private String cliente;
    private Double montoClp;
    private Double montoUsd;   // enriquecimiento (conversion simulada)
    private String motivo;
    private String severidad;  // "ALTA" o "MEDIA"
}