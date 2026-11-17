import { ChangeDetectorRef, Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { MsalService } from '@azure/msal-angular';
import { environment } from '../../environments/environment';

interface LogEntry {
  timestamp: string;
  type: 'INFO' | 'ERROR' | 'SUCCESS' | 'WARN';
  message: string;
}

interface KafkaEnvio {
  topic: string;
  particion: number;
  offset: number;
  key: string;
  mensaje: string;
}

interface AlertaPedido {
  pedidoId: string;
  cliente: string;
  montoClp: number;
  montoUsd: number;
  motivo: string;
  severidad: 'ALTA' | 'MEDIA';
}

interface ColaStats {
  mensajes: number;
  consumidores: number;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.css']
})
export class DashboardComponent implements OnInit, OnDestroy {
  private http = inject(HttpClient);
  private msal = inject(MsalService);
  private cdr = inject(ChangeDetectorRef);
  private renderTimer?: ReturnType<typeof setInterval>;

  userName: string = '';

  // En la nube pasan por Nginx: /order/... y /fraud/...
  private orderApi = environment.orderServiceUrl;
  private fraudApi = environment.fraudServiceUrl;
  private orderApiUrl = `${environment.orderServiceUrl}/api/admin/queues`;

  // ===== RabbitMQ: emisor =====
  selectedType: string = 'INFO';
  customerName: string = 'Cliente Corporativo';
  orderTotal: number = 29990;
  isSending: boolean = false;
  colas: Record<string, ColaStats> = {};

  stats = { total: 0, success: 0, dlq: 0, kafka: 0, alertas: 0 };

  // ===== RabbitMQ: administrador de colas =====
  newQueueName: string = '';
  isDurable: boolean = true;
  isLoadingAdmin: boolean = false;
  managedQueues: string[] = ['cola_auditoria_pedidos', 'pedidos_dlq'];

  // ===== Kafka: topic pedidos-eventos =====
  kafkaMensaje: string = 'Paso1-Creada';
  kafkaKey: string = 'orden-555';
  isSendingKafka: boolean = false;
  kafkaEnvios: KafkaEnvio[] = [];

  // ===== Kafka Streams: pipeline anti-fraude =====
  pipeCliente: string = 'juan';
  pipeMonto: number = 1500000;
  pipeCanal: string = 'web';
  isSendingPipe: boolean = false;
  alertas: AlertaPedido[] = [];
  totales: { cliente: string; total: number }[] = [];

  systemLogs: LogEntry[] = [];
  colasRabbit: any[] = [];

  // ===== Guía 3.3.2: prueba de carga y consumer lag =====
  cargaCantidad: number = 100000;
  isCargando: boolean = false;
  cargaResultado: any = null;
  lagInfo: any = null;
  lagMaximo: number = 0;

  private refreshTimer?: ReturnType<typeof setInterval>;

  ngOnInit() {
    const account = this.msal.instance.getActiveAccount() ?? this.msal.instance.getAllAccounts()[0];
    this.userName = account?.name ?? account?.username ?? '';
    if (this.userName) {
      this.addLog('SUCCESS', `Sesión iniciada con Microsoft Entra ID: ${this.userName}.`);
    }
    this.addLog('INFO', 'Dashboard conectado a RabbitMQ (order-service), Kafka (pedidos-eventos) y Kafka Streams (fraud-service).');

    this.refrescar();
    this.refreshTimer = setInterval(() => this.refrescar(), 3000);
    // App zoneless: avisar a Angular que redibuje cuando llegan datos HTTP
    this.renderTimer = setInterval(() => this.cdr.markForCheck(), 500);
  }

  ngOnDestroy() {
    clearInterval(this.renderTimer);
    if (this.refreshTimer) {
      clearInterval(this.refreshTimer);
    }
  }

  addLog(type: 'INFO' | 'ERROR' | 'SUCCESS' | 'WARN', message: string) {
    const now = new Date();
    const timestamp = now.toTimeString().split(' ')[0] + '.' + String(now.getMilliseconds()).padStart(3, '0');
    this.systemLogs.unshift({ timestamp, type, message });
    if (this.systemLogs.length > 30) {
      this.systemLogs.pop();
    }
  }

  // Cada 3 s: estado de colas, alertas y totales
  refrescar() {
    this.cargarColas();
    this.refrescarLag();
    this.http.get<Record<string, ColaStats>>(`${this.orderApi}/api/orders/messages/stats`).subscribe({
      next: (res) => this.colas = res,
      error: () => {}
    });

    this.http.get<AlertaPedido[]>(`${this.fraudApi}/api/pedidos/alertas`).subscribe({
      next: (res) => {
        if (res.length > this.alertas.length && this.alertas.length > 0) {
          const nueva = res[0];
          this.addLog('ERROR', `Kafka Streams: ALERTA ${nueva.severidad} - ${nueva.motivo} (${nueva.cliente}, US$${nueva.montoUsd}).`);
        }
        this.alertas = res;
        this.stats.alertas = res.length;
      },
      error: () => {}
    });

    this.http.get<Record<string, number>>(`${this.fraudApi}/api/pedidos/totales`).subscribe({
      next: (res) => this.totales = Object.entries(res).map(([cliente, total]) => ({ cliente, total })),
      error: () => {}
    });
  }

  // ===== RabbitMQ: envío real =====
  executeSend() {
    this.isSending = true;
    const payload = {
      level: this.selectedType,
      message: `Orden de ${this.customerName} por $${this.orderTotal}`
    };

    this.http.post(`${this.orderApi}/api/orders/messages/send`, payload, { responseType: 'text' }).subscribe({
      next: (res) => {
        this.stats.total++;
        if (this.selectedType === 'FAIL') {
          this.stats.dlq++;
          this.addLog('WARN', `RabbitMQ: orden de ${this.customerName} ($${this.orderTotal}) rechazada intencionalmente.`);
          this.addLog('ERROR', res);
        } else {
          this.stats.success++;
          this.addLog('SUCCESS', `RabbitMQ: ${res} (orders.exchange -> inventory.queue).`);
        }
        this.isSending = false;
        this.refrescar();
      },
      error: (err) => {
        this.addLog('ERROR', `RabbitMQ: no se pudo publicar el evento (${err.status || 'sin conexión'}).`);
        this.isSending = false;
      }
    });
  }

  // ===== Kafka: productor =====
  enviarKafka(conKey: boolean) {
    if (!this.kafkaMensaje.trim()) {
      this.addLog('WARN', 'Kafka: el mensaje no puede estar vacío.');
      return;
    }
    this.isSendingKafka = true;
    const params: Record<string, string> = { message: this.kafkaMensaje.trim() };
    if (conKey && this.kafkaKey.trim()) {
      params['key'] = this.kafkaKey.trim();
    }

    this.http.get<KafkaEnvio>(`${this.orderApi}/api/kafka/send`, { params }).subscribe({
      next: (res) => {
        this.kafkaEnvios.unshift(res);
        if (this.kafkaEnvios.length > 12) {
          this.kafkaEnvios.pop();
        }
        this.stats.kafka++;
        this.addLog('SUCCESS', `Kafka: '${res.mensaje}' -> ${res.topic} | partición ${res.particion} | offset ${res.offset} | key ${res.key}.`);
        this.isSendingKafka = false;
      },
      error: (err) => {
        this.addLog('ERROR', `Kafka: no se pudo enviar el mensaje (${err.status || 'sin conexión'}).`);
        this.isSendingKafka = false;
      }
    });
  }

  // ===== Kafka Streams: pedido al pipeline =====
  enviarPipeline() {
    if (!this.pipeCliente.trim() || !this.pipeMonto) {
      this.addLog('WARN', 'Pipeline: completa cliente y monto.');
      return;
    }
    this.isSendingPipe = true;
    const pedido = { cliente: this.pipeCliente.trim(), monto: this.pipeMonto, canal: this.pipeCanal };

    this.http.post(`${this.fraudApi}/api/pedidos/transaccion`, pedido, { responseType: 'text' }).subscribe({
      next: (res) => {
        this.addLog('INFO', `Kafka Streams: ${res}`);
        if (this.pipeMonto < 1000) {
          this.addLog('WARN', 'Pipeline: monto menor a $1.000, el filtro lo va a ignorar.');
        }
        this.isSendingPipe = false;
        setTimeout(() => this.refrescar(), 1500);
      },
      error: (err) => {
        this.addLog('ERROR', `Pipeline: no se pudo enviar el pedido (${err.status || 'sin conexión'}).`);
        this.isSendingPipe = false;
      }
    });
  }

  // ===== RabbitMQ: administrador de colas =====
  createQueue() {
    if (!this.newQueueName || this.newQueueName.trim() === '') {
      this.addLog('WARN', 'Validación rechazada: el nombre de la cola no puede ser vacío.');
      return;
    }
    const queueName = this.newQueueName.trim();
    this.isLoadingAdmin = true;
    const payload = { name: queueName, durable: this.isDurable };

    this.http.post<any>(this.orderApiUrl, payload).subscribe({
      next: () => {
        this.addLog('SUCCESS', `AMQP Admin: Cola '${queueName}' aprovisionada con persistencia durable=${this.isDurable}.`);
        if (!this.managedQueues.includes(queueName)) {
          this.managedQueues.push(queueName);
        }
        this.newQueueName = '';
        this.cargarColas();
        this.isLoadingAdmin = false;
      },
      error: (err) => {
        this.addLog('ERROR', `Error al aprovisionar cola: ${err.error?.message || err.message || 'Fallo de conexión'}`);
        this.isLoadingAdmin = false;
      }
    });
  }

  deleteQueue(queueName: string) {
    if (!confirm(`¿Eliminar la cola '${queueName}' de RabbitMQ?`)) {
      return;
    }
    this.http.delete<any>(`${this.orderApiUrl}/${queueName}`).subscribe({
      next: () => {
        this.addLog('WARN', `AMQP Admin: Cola '${queueName}' purgada y eliminada del broker.`);
        this.cargarColas();
      },
      error: (err) => {
        this.addLog('ERROR', `Fallo al eliminar la cola '${queueName}': ${err.error?.message || err.message}`);
      }
    });
  }

  lanzarCarga() {
    this.isCargando = true;
    this.lagMaximo = 0;
    this.addLog('WARN', `Prueba de carga: enviando ${this.cargaCantidad} eventos de tracking a Kafka...`);
    this.http.get<any>(`${this.orderApi}/api/test/start`, { params: { count: String(this.cargaCantidad) } }).subscribe({
      next: (res) => {
        this.cargaResultado = res;
        this.addLog('SUCCESS', `Carga completada: ${res.mensajes} mensajes en ${res.msConfirmado} ms (${res.throughputMsgSeg} msg/s).`);
        this.isCargando = false;
        this.refrescarLag();
      },
      error: (err) => {
        this.addLog('ERROR', `Prueba de carga fallida (${err.status || 'sin conexión'}).`);
        this.isCargando = false;
      }
    });
  }

  refrescarLag() {
    this.http.get<any>(`${this.orderApi}/api/test/lag`).subscribe({
      next: (res) => {
        this.lagInfo = res;
        this.lagMaximo = Math.max(this.lagMaximo, res.lagTotal);
      },
      error: () => {}
    });
  }

  porcentajeLag(): number {
    if (!this.lagInfo || this.lagMaximo === 0) {
      return 0;
    }
    return Math.round(this.lagInfo.lagTotal * 100 / this.lagMaximo);
  }

  cargarColas() {
    this.http.get<any[]>(this.orderApiUrl).subscribe({
      next: (res) => this.colasRabbit = res,
      error: () => {}
    });
  }

  logout() {
    this.msal.logoutRedirect();
  }

  clearLogs() {
    this.systemLogs = [];
  }
}
