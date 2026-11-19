import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { forkJoin } from 'rxjs';
import { environment } from '../../../environments/environment';

@Component({
  standalone: false,
  selector: 'app-kafka-admin',
  template: `
    <section class="dashboard">

      <div class="header">
        <div>
          <span class="badge">KAFKA · ADMINISTRACIÓN</span>
          <h1>Kafka Admin</h1>
          <p>Administración de topics y grupos de consumidores.</p>
        </div>

        <button (click)="cargar()">Actualizar</button>
      </div>

      <p class="success" *ngIf="mensaje">{{ mensaje }}</p>
      <p class="error" *ngIf="error">{{ error }}</p>
      <p *ngIf="cargando">Cargando información...</p>

      <div class="panel">
        <h2>{{ editando ? 'Modificar topic' : 'Crear topic' }}</h2>

        <form (ngSubmit)="guardar()">

          <div class="form-grid">

            <label>Nombre del topic
              <input name="nombre" [(ngModel)]="form.nombre"
                     [disabled]="editando" required>
            </label>

            <label>Particiones
              <input type="number" name="particiones"
                     [(ngModel)]="form.particiones"
                     [disabled]="editando" min="1" max="12" required>
            </label>

            <label>Réplicas
              <input type="number" name="replicas"
                     [(ngModel)]="form.replicas"
                     [disabled]="editando" min="1" max="3" required>
            </label>

            <label>Min ISR
              <input type="number" name="minIsr"
                     [(ngModel)]="form.minIsr" min="1" required>
            </label>

            <label>Retención (ms)
              <input type="number" name="retencionMs"
                     [(ngModel)]="form.retencionMs"
                     min="60000" required>
            </label>

            <label>Política de limpieza
              <select name="politica" [(ngModel)]="form.politica">
                <option value="delete">Delete</option>
                <option value="compact">Compact</option>
                <option value="compact,delete">Compact + Delete</option>
              </select>
            </label>

          </div>

          <button type="submit" [disabled]="ocupado || cargando">
            {{ editando ? 'Guardar cambios' : 'Crear topic' }}
          </button>

          <button type="button" class="secondary"
                  *ngIf="editando" (click)="limpiar()">
            Cancelar
          </button>

        </form>
      </div>

      <div class="panel">
        <h2>Topics del clúster</h2>

        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Topic</th>
                <th>Particiones</th>
                <th>Réplicas</th>
                <th>Min ISR</th>
                <th>Retención (ms)</th>
                <th>Política</th>
                <th>Acciones</th>
              </tr>
            </thead>

            <tbody>
              <tr *ngFor="let t of topics">
                <td><strong>{{ t.nombre }}</strong></td>
                <td>{{ t.particiones }}</td>
                <td>{{ t.replicas }}</td>
                <td>{{ t.minIsr }}</td>
                <td>{{ t.retencionMs }}</td>
                <td>{{ t.politica }}</td>
                <td>
                  <ng-container *ngIf="t.nombre.startsWith('demo.'); else protegido">
                    <button class="small" (click)="editar(t)"
                            [disabled]="ocupado">Editar</button>

                    <button class="danger small" (click)="eliminar(t)"
                            [disabled]="ocupado">Eliminar</button>
                  </ng-container>

                  <ng-template #protegido>
                    <span class="protected">Protegido</span>
                  </ng-template>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="panel">
        <h2>Consumer Groups</h2>

        <table>
          <thead>
            <tr>
              <th>Grupo</th>
              <th>Estado</th>
            </tr>
          </thead>

          <tbody>
            <tr *ngFor="let g of grupos">
              <td>{{ g.grupo }}</td>
              <td>
                <span class="protected">{{ g.estado }}</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

    </section>
  `,
  styles: [`
    .dashboard {
      background: #f6f8fc;
      min-height: 80vh;
      padding: 45px 5%;
      color: #0f172a;
    }

    .header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 20px;
      margin-bottom: 25px;
    }

    .badge {
      color: #2563eb;
      font-size: 12px;
      font-weight: bold;
      letter-spacing: 1px;
    }

    h1 { font-size: 34px; margin: 10px 0; }
    p { color: #64748b; }

    .panel {
      padding: 25px;
      background: white;
      border: 1px solid #e8edf4;
      border-radius: 16px;
      margin-bottom: 24px;
      box-shadow: 0 8px 25px rgba(15,23,42,.04);
    }

    .form-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 18px;
      margin: 20px 0;
    }

    label {
      display: flex;
      flex-direction: column;
      gap: 8px;
      font-size: 14px;
    }

    input, select {
      padding: 12px;
      border: 1px solid #cbd5e1;
      border-radius: 8px;
      min-width: 0;
    }

    button {
      background: #2563eb;
      color: white;
      border: 0;
      border-radius: 8px;
      padding: 12px 18px;
      cursor: pointer;
      font-weight: bold;
    }

    button:disabled { opacity: .5; cursor: not-allowed; }
    .secondary { background: #64748b; margin-left: 5px; }
    .danger { background: #dc2626; }
    .small { padding: 7px 10px; margin: 3px; }

    .table-wrap { overflow-x: auto; }

    table {
      width: 100%;
      border-collapse: collapse;
    }

    th, td {
      text-align: left;
      padding: 14px;
      border-bottom: 1px solid #eef2f7;
      font-size: 13px;
    }

    th { background: #f8fafc; color: #64748b; }

    .protected {
      color: #166534;
      background: #dcfce7;
      padding: 6px 10px;
      border-radius: 20px;
      font-weight: bold;
    }

    .success {
      padding: 15px;
      background: #dcfce7;
      color: #166534;
      border-radius: 10px;
    }

    .error {
      padding: 15px;
      background: #fff1f2;
      color: #be123c;
      border-radius: 10px;
    }

    @media (max-width: 800px) {
      .form-grid { grid-template-columns: 1fr; }
      .header { flex-wrap: wrap; }
    }
  `]
})
export class KafkaAdminComponent implements OnInit {

  private api = `${environment.apiBaseUrl}/api/admin/kafka`;

  topics: any[] = [];
  grupos: any[] = [];

  mensaje = '';
  error = '';
  cargando = false;
  ocupado = false;
  editando = false;

  form = this.formularioInicial();

  constructor(
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) {}

  formularioInicial() {
    return {
      nombre: 'demo.prueba',
      particiones: 3,
      replicas: 3,
      minIsr: 2,
      retencionMs: 604800000,
      politica: 'delete'
    };
  }

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    if (this.cargando) return;

    this.cargando = true;
    this.error = '';

    forkJoin({
      topics: this.http.get<any[]>(`${this.api}/topics`),
      grupos: this.http.get<any[]>(`${this.api}/groups`)
    }).subscribe({
      next: data => {
        this.topics = data.topics;
        this.grupos = data.grupos;
        this.cargando = false;
        this.cdr.markForCheck();
      },
      error: err => {
        this.error = 'Error al consultar Kafka. HTTP ' + err.status;
        this.cargando = false;
        this.cdr.markForCheck();
      }
    });
  }

  guardar(): void {
    if (this.ocupado) return;

    this.ocupado = true;
    this.error = '';
    this.mensaje = '';

    const solicitud = this.editando
      ? this.http.put<any>(
          `${this.api}/topics/${encodeURIComponent(this.form.nombre)}`,
          {
            minIsr: this.form.minIsr,
            retencionMs: this.form.retencionMs,
            politica: this.form.politica
          }
        )
      : this.http.post<any>(`${this.api}/topics`, this.form);

    solicitud.subscribe({
      next: respuesta => {
        this.mensaje = respuesta.mensaje;
        this.ocupado = false;
        this.limpiar();
        this.cargar();
        this.cdr.markForCheck();
      },
      error: err => {
        this.error = 'No se pudo guardar. HTTP ' + err.status;
        this.ocupado = false;
        this.cdr.markForCheck();
      }
    });
  }

  editar(topic: any): void {
    this.editando = true;

    this.form = {
      nombre: topic.nombre,
      particiones: topic.particiones,
      replicas: topic.replicas,
      minIsr: Number(topic.minIsr),
      retencionMs: Number(topic.retencionMs),
      politica: topic.politica === 'delete,compact'
        ? 'compact,delete'
        : topic.politica
    };
  }

  eliminar(topic: any): void {
    if (this.ocupado) return;

    if (!confirm('¿Eliminar el topic ' + topic.nombre + '?')) return;

    this.ocupado = true;
    this.error = '';
    this.mensaje = '';

    this.http.delete<any>(
      `${this.api}/topics/${encodeURIComponent(topic.nombre)}`
    ).subscribe({
      next: respuesta => {
        this.mensaje = respuesta.mensaje;
        this.ocupado = false;
        this.limpiar();
        this.cargar();
        this.cdr.markForCheck();
      },
      error: err => {
        this.error = 'No se pudo eliminar. HTTP ' + err.status;
        this.ocupado = false;
        this.cdr.markForCheck();
      }
    });
  }

  limpiar(): void {
    this.editando = false;
    this.form = this.formularioInicial();
  }
}
