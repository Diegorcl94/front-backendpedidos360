import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { forkJoin } from 'rxjs';
import { environment } from '../../../environments/environment';

@Component({
  standalone: false,
  selector: 'app-estadisticas',
  template: `
    <section class="dashboard">

      <div class="header">
        <div>
          <span class="badge">KAFKA · EVENT STREAMING</span>
          <h1>Reportes y auditoría</h1>
          <p>Monitoreo de los eventos procesados por Pedidos360.</p>
        </div>

        <button (click)="cargar()" [disabled]="cargando">
          Actualizar
        </button>
      </div>

      <p *ngIf="cargando">Cargando información...</p>
      <p class="error" *ngIf="error">{{ error }}</p>

      <div class="cards" *ngIf="resumen && !error">

        <article>
          <span>📦 Pedidos procesados</span>
          <strong>{{ resumen.pedidosProcesados }}</strong>
        </article>

        <article>
          <span>📊 Unidades solicitadas</span>
          <strong>{{ resumen.unidadesSolicitadas }}</strong>
        </article>

        <article>
          <span>💰 Monto total</span>
          <strong>{{ formatearMonto(resumen.montoTotal) }}</strong>
        </article>

      </div>

      <div class="panel" *ngIf="!error && !cargando">
        <h2>Historial de auditoría</h2>
        <p>Eventos recibidos desde audit.timeline</p>

        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Pedido</th>
                <th>Evento</th>
                <th>Producto</th>
                <th>Fecha</th>
                <th>Actor</th>
                <th>Partición</th>
                <th>Offset</th>
              </tr>
            </thead>

            <tbody>
              <tr *ngFor="let evento of eventos">
                <td>#{{ evento.pedidoId }}</td>
                <td>
                  <span class="event-badge">
                    {{ evento.tipoEvento }}
                  </span>
                </td>
                <td>{{ evento.producto }}</td>
                <td>{{ evento.fecha | date:'dd/MM/yyyy HH:mm:ss' }}</td>
                <td class="actor">{{ evento.actor }}</td>
                <td>{{ evento.particion }}</td>
                <td>{{ evento.offset }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <p *ngIf="eventos.length === 0">
          No existen eventos registrados.
        </p>
      </div>

    </section>
  `,
  styles: [`
    .dashboard {
      background: #f6f8fc;
      min-height: 80vh;
      padding: 50px 6%;
      color: #0f172a;
    }

    .header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 20px;
      margin-bottom: 30px;
    }

    h1 {
      font-size: 34px;
      margin: 10px 0;
    }

    p { color: #64748b; }

    .badge {
      color: #2563eb;
      font-size: 12px;
      font-weight: 800;
      letter-spacing: 1px;
    }

    button {
      background: #2563eb;
      color: white;
      border: 0;
      padding: 13px 22px;
      border-radius: 10px;
      cursor: pointer;
      font-weight: bold;
    }

    button:disabled {
      opacity: .5;
    }

    .cards {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 20px;
      margin-bottom: 30px;
    }

    article, .panel {
      background: white;
      border: 1px solid #e8edf4;
      border-radius: 16px;
      padding: 25px;
      box-shadow: 0 8px 25px rgba(15,23,42,.04);
    }

    article span {
      color: #64748b;
      font-size: 14px;
    }

    article strong {
      display: block;
      font-size: 32px;
      margin-top: 12px;
    }

    .table-wrap {
      overflow-x: auto;
    }

    table {
      width: 100%;
      border-collapse: collapse;
    }

    th, td {
      text-align: left;
      padding: 15px;
      border-bottom: 1px solid #eef2f7;
      font-size: 13px;
    }

    th {
      background: #f8fafc;
      color: #64748b;
    }

    .event-badge {
      background: #dcfce7;
      color: #166534;
      padding: 6px 10px;
      border-radius: 20px;
      font-weight: bold;
    }

    .actor {
      max-width: 180px;
      overflow-wrap: anywhere;
    }

    .error {
      background: #fff1f2;
      color: #be123c;
      padding: 15px;
      border-radius: 10px;
    }

    @media (max-width: 800px) {
      .cards {
        grid-template-columns: 1fr;
      }

      .header {
        flex-direction: column;
        align-items: flex-start;
      }
    }
  `]
})
export class EstadisticasComponent implements OnInit {

  resumen: any = null;
  eventos: any[] = [];
  cargando = false;
  error = '';

  constructor(
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando = true;
    this.error = '';

    forkJoin({
      resumen: this.http.get<any>(
        `${environment.apiBaseUrl}/api/reportes/resumen`
      ),
      eventos: this.http.get<any[]>(
        `${environment.apiBaseUrl}/api/auditoria/eventos`
      )
    }).subscribe({
      next: (datos) => {
        this.resumen = datos.resumen;
        this.eventos = datos.eventos;
        this.cargando = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = 'No fue posible cargar los datos. HTTP ' + err.status;
        this.cargando = false;
        this.cdr.markForCheck();
      }
    });
  }

  formatearMonto(monto: number): string {
    return new Intl.NumberFormat('es-CL', {
      style: 'currency',
      currency: 'CLP'
    }).format(monto);
  }
}
