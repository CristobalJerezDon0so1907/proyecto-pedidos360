import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MsalService, MsalBroadcastService } from '@azure/msal-angular';
import { EventMessage, EventType } from '@azure/msal-browser';
import { filter, takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './login.html',
  styleUrls: ['./login.css']
})
export class LoginComponent implements OnInit, OnDestroy {
  username = '';
  password = '';
  errorMessage = '';

  isMsalLoggedIn = false;
  msalUserName = '';
  debugToken = '';

  private readonly _destroying$ = new Subject<void>();

  constructor(
    private router: Router,
    private msal: MsalService,
    private msalBroadcastService: MsalBroadcastService
  ) {}

  ngOnInit(): void {
    const savedToken = sessionStorage.getItem('debugToken');
    if (savedToken) {
      this.debugToken = savedToken;
    }

    // --- TEMPORAL: ver todos los eventos de MSAL ---
    this.msalBroadcastService.msalSubject$
      .pipe(takeUntil(this._destroying$))
      .subscribe((msg: EventMessage) => {
        console.log('EVENTO MSAL:', msg.eventType, msg.payload);
      });

    this.msalBroadcastService.msalSubject$
      .pipe(
        filter((msg: EventMessage) => msg.eventType === EventType.INITIALIZE_END),
        takeUntil(this._destroying$)
      )
      .subscribe(() => {
        this.updateMsalStatus();
      });

    this.msalBroadcastService.msalSubject$
      .pipe(
        filter((msg: EventMessage) =>
          msg.eventType === EventType.LOGIN_SUCCESS ||
          msg.eventType === EventType.ACQUIRE_TOKEN_SUCCESS
        ),
        takeUntil(this._destroying$)
      )
      .subscribe((result: EventMessage) => {
        const payload = result.payload as any;
        if (payload?.account) {
          this.msal.instance.setActiveAccount(payload.account);
        }
        if (payload?.accessToken) {
          this.debugToken = payload.accessToken;
          sessionStorage.setItem('debugToken', payload.accessToken);
        }
        this.updateMsalStatus();
      });

    this.updateMsalStatus();
  }

  private updateMsalStatus(): void {
    const accounts = this.msal.instance.getAllAccounts();
    this.isMsalLoggedIn = accounts.length > 0;

    if (this.isMsalLoggedIn) {
      const activeAccount = this.msal.instance.getActiveAccount() || accounts[0];
      this.msal.instance.setActiveAccount(activeAccount);
      this.msalUserName = activeAccount.name ?? activeAccount.username;
    } else {
      this.msalUserName = '';
    }
  }

  onSubmit() {
    if (this.username === 'admin' && this.password === '1234') {
      alert('¡Inicio de sesión exitoso!');
      this.errorMessage = '';
    } else {
      this.errorMessage = 'Usuario o contraseña incorrectos';
    }
  }

  loginWithMsal(): void {
    this.msal.loginRedirect();
  }

  logoutWithMsal(): void {
    this.msal.logoutRedirect();
  }

  obtenerToken(): void {
    this.msal.instance.acquireTokenRedirect({
      scopes: ['api://d483f75f-ee12-4d87-b9a3-6214dd4ceb87/OT.Create']
    });
  }

  ngOnDestroy(): void {
    this._destroying$.next();
    this._destroying$.complete();
  }
}
