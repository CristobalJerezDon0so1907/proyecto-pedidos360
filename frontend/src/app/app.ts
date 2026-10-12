import { Component } from '@angular/core';

@Component({
  selector: 'app-root',
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class AppComponent {
  title = 'frontend';
  respuesta: string = '';

  async enviarMensaje(levelSelect: string, messageInput: string) {
    const routingKey = (levelSelect === 'FAIL') ? 'INFO' : levelSelect;
    const finalMessage = (levelSelect === 'FAIL' && !messageInput.includes('FAIL')) 
      ? `${messageInput} FAIL` 
      : messageInput;

    try {
      const res = await fetch('http://localhost:8081/api/orders/messages/send', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ level: routingKey, message: finalMessage })
      });

      if (res.ok) {
        this.respuesta = await res.text();
      } else {
        this.respuesta = 'Error al comunicar con la API';
      }
    } catch (err: any) {
      this.respuesta = 'Error de conexión: ' + err.message;
    }
  }
}
