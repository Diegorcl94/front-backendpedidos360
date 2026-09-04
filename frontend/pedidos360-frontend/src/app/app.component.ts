import { Component, OnDestroy, OnInit } from '@angular/core';
import { InteractionStatus } from '@azure/msal-browser';
import { MsalBroadcastService, MsalService } from '@azure/msal-angular';
import { Subject, filter, takeUntil } from 'rxjs';
import { pedidosScope } from './auth-config';

@Component({
  standalone: false,
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit, OnDestroy {

  private readonly destroy$ = new Subject<void>();

  loginDisplay = false;
  nombreUsuario = '';

  constructor(
    private readonly authService: MsalService,
    private readonly msalBroadcastService: MsalBroadcastService
  ) {}

  ngOnInit(): void {

    this.msalBroadcastService.inProgress$
      .pipe(
        filter(
          (status: InteractionStatus) =>
            status === InteractionStatus.None
        ),
        takeUntil(this.destroy$)
      )
      .subscribe(() => {
        this.actualizarSesion();
      });
  }

  login(): void {

    this.authService.loginRedirect({
      scopes: [
        'openid',
        'profile',
        pedidosScope
      ]
    });
  }

  logout(): void {

    this.authService.logoutRedirect({
      postLogoutRedirectUri: 'http://localhost:4200'
    });
  }

  private actualizarSesion(): void {

    const cuentas =
      this.authService.instance.getAllAccounts();

    if (
      !this.authService.instance.getActiveAccount() &&
      cuentas.length > 0
    ) {
      this.authService.instance.setActiveAccount(cuentas[0]);
    }

    const cuenta =
      this.authService.instance.getActiveAccount();

    this.loginDisplay = !!cuenta;

    this.nombreUsuario =
      cuenta?.name ??
      cuenta?.username ??
      '';
  }

  ngOnDestroy(): void {

    this.destroy$.next();
    this.destroy$.complete();
  }
}
