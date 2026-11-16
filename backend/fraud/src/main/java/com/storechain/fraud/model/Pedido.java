package com.storechain.fraud.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Evento de ENTRADA del pipeline (topic: pedidos-transacciones).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Pedido {
    private String pedidoId;
    private String cliente;
    private Double monto;      // en CLP
    private String canal;      // "web", "app", "tienda"
    private Long timestamp;
}