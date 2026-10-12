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

  constructor(private http: HttpClient) {}

  executeSend() {
    const payload = {
      message: this.messageText,
      type: this.selectedType
    };

    this.stats.total++;

    // Se corrige la URL de http://localhost:8081/orders a http://localhost:8081/api/orders
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
}