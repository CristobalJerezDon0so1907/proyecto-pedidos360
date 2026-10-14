import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';

interface LogEntry {
  timestamp: string;
  type: 'INFO' | 'ERROR' | 'SUCCESS' | 'WARN';
  message: string;
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.html',
  styleUrls: ['./app.css']
})
export class AppComponent implements OnInit {
  private http = inject(HttpClient);

  // Estado del mensaje
  selectedType: string = 'INFO';
  customerName: string = 'Cliente Corporativo';
  orderTotal: number = 29990;
  isSending: boolean = false;

  // Métricas
  stats = {
    total: 0,
    success: 0,
    dlq: 0
  };

  // Administrador de Colas
  newQueueName: string = '';
  isDurable: boolean = true;
  isLoadingAdmin: boolean = false;
  managedQueues: string[] = ['cola_auditoria_pedidos', 'pedidos_dlq'];

  // Logs en tiempo real
  systemLogs: LogEntry[] = [];

  private orderApiUrl = 'http://localhost:8081/api/admin/queues';

  ngOnInit() {
    this.addLog('INFO', 'Cluster RabbitMQ inicializado en nodos rabbit@rabbitmq-1 y rabbit@rabbitmq-2.');
    this.addLog('SUCCESS', 'Conexión con BFF (puerto 8080) y Order-Service (puerto 8081) activa.');
  }

  addLog(type: 'INFO' | 'ERROR' | 'SUCCESS' | 'WARN', message: string) {
    const now = new Date();
    const timestamp = now.toTimeString().split(' ')[0] + '.' + String(now.getMilliseconds()).padStart(3, '0');
    this.systemLogs.unshift({ timestamp, type, message });
    if (this.systemLogs.length > 20) {
      this.systemLogs.pop();
    }
  }

  executeSend() {
    this.isSending = true;
    this.stats.total++;

    setTimeout(() => {
      if (this.selectedType === 'FAIL') {
        this.stats.dlq++;
        this.addLog('WARN', `Orden para ${this.customerName} ($${this.orderTotal}) rechazada intencionalmente.`);
        this.addLog('ERROR', `Mensaje desviado por regla de enrutamiento hacia 'pedidos_dlq' (Dead Letter Exchange).`);
      } else {
        this.stats.success++;
        this.addLog('INFO', `Orden generada para ${this.customerName} por monto $${this.orderTotal}.`);
        this.addLog('SUCCESS', `Evento publicado en exchange 'orders_exchange' y sincronizado en cluster.`);
      }
      this.isSending = false;
    }, 300);
  }

  createQueue() {
    if (!this.newQueueName || this.newQueueName.trim() === '') {
      this.addLog('WARN', 'Validación rechazada: el nombre de la cola no puede ser vacío.');
      return;
    }

    const queueName = this.newQueueName.trim();
    this.isLoadingAdmin = true;

    const payload = {
      name: queueName,
      durable: this.isDurable
    };

    this.http.post<any>(this.orderApiUrl, payload).subscribe({
      next: (res) => {
        this.addLog('SUCCESS', `AMQP Admin: Cola '${queueName}' aprovisionada con persistencia durable=${this.isDurable}.`);
        if (!this.managedQueues.includes(queueName)) {
          this.managedQueues.push(queueName);
        }
        this.newQueueName = '';
        this.isLoadingAdmin = false;
      },
      error: (err) => {
        this.addLog('ERROR', `Error al aprovisionar cola: ${err.error?.message || err.message || 'Fallo de conexión'}`);
        this.isLoadingAdmin = false;
      }
    });
  }

  deleteQueue(queueName: string) {
    this.http.delete<any>(`${this.orderApiUrl}/${queueName}`).subscribe({
      next: () => {
        this.addLog('WARN', `AMQP Admin: Cola '${queueName}' purgada y eliminada del broker.`);
        this.managedQueues = this.managedQueues.filter(q => q !== queueName);
      },
      error: (err) => {
        this.addLog('ERROR', `Fallo al eliminar la cola '${queueName}': ${err.message}`);
      }
    });
  }

  clearLogs() {
    this.systemLogs = [];
  }
}