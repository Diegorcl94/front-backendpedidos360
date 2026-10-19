import { CommonModule } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NgModule, provideZonelessChangeDetection } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { beforeEach, afterEach, describe, expect, it } from 'vitest';
import { environment } from '../../../environments/environment';
import { Pedido } from '../../services/pedidos.service';
import { PedidosComponent } from './pedidos.component';

@NgModule({
  declarations: [PedidosComponent],
  imports: [CommonModule, FormsModule]
})
class PedidosTestModule {}

describe('PedidosComponent: actualización después de HTTP en Angular zoneless', () => {
  const apiUrl = `${environment.apiBaseUrl}/api/pedidos`;
  const existente: Pedido = {
    id: 1, cliente: 'Cliente Norte', producto: 'Monitor',
    cantidad: 1, total: 100000, estado: 'PENDIENTE'
  };
  const nuevo: Omit<Pedido, 'id'> = {
    cliente: 'Cliente Santiago', producto: 'Teclado',
    cantidad: 2, total: 40000, estado: 'PENDIENTE'
  };
  let fixture: ComponentFixture<PedidosComponent>;
  let http: HttpTestingController;
  let root: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PedidosTestModule],
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(PedidosComponent);
    root = fixture.nativeElement;
    // Render inicial: inicia el GET. No se fuerza detectChanges().
    await fixture.whenStable();
  });

  afterEach(() => {
    http.verify();
  });

  async function cargarInicial(): Promise<void> {
    http.expectOne({ method: 'GET', url: apiUrl }).flush([existente]);
    await fixture.whenStable();
  }

  async function enviarFormulario(): Promise<void> {
    root.querySelector<HTMLButtonElement>('.btn-nuevo-pedido')!.click();
    await fixture.whenStable();
    for (const [name, value] of Object.entries(nuevo)) {
      const input = root.querySelector<HTMLInputElement>(`input[name="${name}"]`);
      if (input) {
        input.value = String(value);
        input.dispatchEvent(new Event('input', { bubbles: true }));
      }
    }
    await fixture.whenStable();
    root.querySelector('form')!.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    await fixture.whenStable();
  }

  it('muestra la consulta inicial sin necesitar otro clic', async () => {
    await cargarInicial();
    expect(root.querySelector('.message')).toBeNull();
    expect(root.querySelector('tbody')!.textContent).toContain('Cliente Norte');
    expect(root.querySelector('.summary-card strong')!.textContent).toBe('1');
  });

  it('muestra confirmación, fila y contador al responder 201; evita POST duplicados y GET adicionales', async () => {
    await cargarInicial();
    await enviarFormulario();
    const post = http.expectOne({ method: 'POST', url: apiUrl });
    expect(post.request.body).toEqual(nuevo);
    expect(root.querySelector<HTMLButtonElement>('.btn-guardar')!.disabled).toBe(true);
    expect(root.querySelector('.btn-guardar')!.textContent).toContain('Guardando...');
    expect(root.querySelector('[role="status"]')).toBeNull();

    // Incluso una segunda emisión de submit mientras espera no debe enviar otro POST.
    root.querySelector('form')!.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    await fixture.whenStable();
    http.expectNone({ method: 'POST', url: apiUrl });

    post.flush({ ...nuevo, id: 4 }, { status: 201, statusText: 'Created' });
    // La respuesta debe programar el render por sí sola, sin clic ni detectChanges().
    await fixture.whenStable();

    expect(fixture.componentInstance.guardando).toBe(false);
    expect(root.querySelector('form')).toBeNull();
    expect(root.querySelector('[role="status"]')!.textContent).toContain('¡Pedido creado correctamente!');
    expect(root.querySelector('[role="status"]')!.textContent).toContain('Pedido #4 creado correctamente.');
    expect(root.querySelectorAll('tbody tr')).toHaveLength(2);
    expect(root.querySelector('tbody tr')!.textContent).toContain('#4');
    expect(root.querySelector('.summary-card strong')!.textContent).toBe('2');
    expect(fixture.componentInstance.nuevoPedido.cliente).toBe('');
    http.expectNone(apiUrl);

    // El botón Actualizar sigue pudiendo recargar manualmente.
    root.querySelector<HTMLButtonElement>('.reload')!.click();
    http.expectOne({ method: 'GET', url: apiUrl }).flush([existente, { ...nuevo, id: 4 }]);
    await fixture.whenStable();
    expect(root.querySelectorAll('tbody tr')).toHaveLength(2);
  });

  it.each([403, 500, 0])('recupera el formulario y muestra HTTP %s sin éxito ni reintentos', async (status) => {
    await cargarInicial();
    await enviarFormulario();
    const post = http.expectOne({ method: 'POST', url: apiUrl });
    if (status === 0) {
      post.error(new ProgressEvent('error'));
    } else {
      post.flush({}, { status, statusText: 'Error' });
    }
    await fixture.whenStable();

    expect(root.querySelector('.error')!.textContent).toContain(`HTTP ${status}`);
    expect(root.querySelector('form')).not.toBeNull();
    expect(root.querySelector<HTMLButtonElement>('.btn-guardar')!.disabled).toBe(false);
    expect(root.querySelector('.btn-guardar')!.textContent).toContain('Crear pedido');
    expect(root.querySelector<HTMLInputElement>('#cliente')!.value).toBe(nuevo.cliente);
    expect(root.querySelector('[role="status"]')).toBeNull();
    expect(fixture.componentInstance.pedidos).toEqual([existente]);
    http.expectNone(apiUrl);
  });

  it('muestra un error del GET sin quedar cargando indefinidamente', async () => {
    http.expectOne({ method: 'GET', url: apiUrl }).flush({}, { status: 500, statusText: 'Error' });
    await fixture.whenStable();
    expect(root.querySelector('.message')).toBeNull();
    expect(root.querySelector('.error')!.textContent).toContain('HTTP 500');
  });
});
