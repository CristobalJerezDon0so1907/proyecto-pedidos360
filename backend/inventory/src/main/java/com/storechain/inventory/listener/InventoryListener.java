package com.storechain.inventory.listener;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class InventoryListener {

    @RabbitListener(queues = "all_orders_queue")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            System.out.println("[INVENTORY] Recibido evento: " + message);

            // Simulación de fallo para probar la DLQ
            if (message.contains("FAIL")) {
                throw new RuntimeException("Error simulado al procesar evento en Inventario");
            }

            System.out.println("[INVENTORY] Procesado con éxito.");
            // Confirmación manual (Ack)
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            System.err.println("[INVENTORY ! ERROR] " + e.getMessage());
            try {
                // Nack con requeue=false: envía el mensaje al DLX (Dead Letter Exchange)
                channel.basicNack(deliveryTag, false, false);
                System.out.println("[INVENTORY -> DLX] Mensaje derivado a la Dead Letter Queue");
            } catch (Exception nackException) {
                nackException.printStackTrace();
            }
        }
    }

    @RabbitListener(queues = "orders.dlq")
    public void receiveDLQ(String message) {
        System.out.println("==================================================");
        System.out.println("[DLQ CUARENTENA] Mensaje fallido capturado: " + message);
        System.out.println("==================================================");
    }
}
