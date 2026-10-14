import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpClientModule } from '@angular/common/http';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule, HttpClientModule],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class AppComponent {
  messageText: string = 'Pedido registrado desde Angular';
  selectedType: string = 'INFO';
  statusMessage: string = '';

  stats = {
    total: 0,
    success: 0,
    failed: 0
  };

  // --- Propiedades para Administrador de Colas ---
  newQueueName: string = '';
  isDurable: boolean = true;
  adminStatusMessage: string = '';
  isAdminError: boolean = false;
  managedQueues: string[] = ['cola_auditoria_pedidos'];

  private readonly adminApiUrl = 'http://localhost:8081/api/admin/queues';

  constructor(private http: HttpClient) {}

  executeSend() {
    const payload = {
      message: this.messageText,
      type: this.selectedType
    };

    this.stats.total++;

    this.http.post<any>('http://localhost:8081/api/orders', payload).subscribe({
      next: (res) => {
        if (this.selectedType === 'FAIL') {
          this.stats.failed++;
          this.statusMessage = `[DLQ] Orden enviada a simulación de error (Routing: ${this.selectedType})`;
        } else {
          this.stats.success++;
          this.statusMessage = `Mensaje enviado a RabbitMQ con Routing Key: ${this.selectedType}`;
        }
      },
      error: (err) => {
        this.stats.failed++;
        this.statusMessage = 'Error de conexión con order-service';
      }
    });
  }

  // --- Acciones de Administración de RabbitMQ ---
  createQueue() {
    if (!this.newQueueName.trim()) {
      this.adminStatusMessage = 'Error 400: El nombre de la cola no puede estar vacío';
      this.isAdminError = true;
      return;
    }

    const payload = {
      name: this.newQueueName.trim(),
      durable: this.isDurable,
      autoDelete: false
    };

    this.http.post<any>(this.adminApiUrl, payload).subscribe({
      next: (res) => {
        this.isAdminError = false;
        this.adminStatusMessage = `Éxito: ${res.message || 'Cola creada'} (${res.queueName})`;
        if (!this.managedQueues.includes(res.queueName)) {
          this.managedQueues.push(res.queueName);
        }
        this.newQueueName = '';
      },
      error: (err) => {
        this.isAdminError = true;
        this.adminStatusMessage = `Error ${err.status}: ${err.error?.message || 'Fallo al crear la cola'}`;
      }
    });
  }

  deleteQueue(queueName: string) {
    this.http.delete<any>(`${this.adminApiUrl}/${queueName}`).subscribe({
      next: (res) => {
        this.isAdminError = false;
        this.adminStatusMessage = `Éxito: ${res.message || 'Cola eliminada'}`;
        this.managedQueues = this.managedQueues.filter((q) => q !== queueName);
      },
      error: (err) => {
        this.isAdminError = true;
        this.adminStatusMessage = `Error ${err.status}: ${err.error?.message || 'Fallo al eliminar la cola'}`;
      }
    });
  }
}