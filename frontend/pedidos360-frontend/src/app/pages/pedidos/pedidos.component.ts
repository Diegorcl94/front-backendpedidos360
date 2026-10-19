import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import {
  Pedido,
  PedidosService
} from '../../services/pedidos.service';

@Component({
  standalone: false,
  selector: 'app-pedidos',
  templateUrl: './pedidos.component.html',
  styleUrls: ['./pedidos.component.css']
})
export class PedidosComponent implements OnInit {

  pedidos: Pedido[] = [];
  cargando = true;
  guardando = false;
  error = '';
  mensaje = '';
  mostrarFormulario = false;

  nuevoPedido = {
    cliente: '',
    producto: '',
    cantidad: 1,
    total: 0,
    estado: 'PENDIENTE'
  };

  constructor(
    private readonly pedidosService: PedidosService,
    private readonly changeDetectorRef: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.cargarPedidos();
  }

  cargarPedidos(): void {
    this.cargando = true;
    this.error = '';

    this.pedidosService.listar().subscribe({
      next: (data) => {
        this.pedidos = data;
        this.cargando = false;
        // Angular zoneless necesita una notificación tras actualizar el estado por HTTP.
        this.changeDetectorRef.markForCheck();
      },
      error: (err) => {
        this.error =
          `No fue posible cargar los pedidos. HTTP ${err.status ?? 'desconocido'}.`;
        this.cargando = false;
        this.changeDetectorRef.markForCheck();
      }
    });
  }

  abrirFormulario(): void {
    this.mostrarFormulario = true;
    this.mensaje = '';
    this.error = '';
  }

  cerrarFormulario(): void {
    this.mostrarFormulario = false;
  }

  crearPedido(): void {

    if (this.guardando) return;

    if (
      !this.nuevoPedido.cliente.trim() ||
      !this.nuevoPedido.producto.trim() ||
      this.nuevoPedido.cantidad < 1 ||
      this.nuevoPedido.total <= 0
    ) {
      this.error = 'Completa todos los campos correctamente.';
      return;
    }

    this.guardando = true;
    this.error = '';
    this.mensaje = '';

    this.pedidosService.crear(this.nuevoPedido).subscribe({
      next: (pedido) => {
        this.guardando = false;
        this.mostrarFormulario = false;

        this.mensaje =
          `Pedido #${pedido.id} creado correctamente.`;

        // Mostrar inmediatamente el pedido en la tabla
        this.pedidos = [pedido, ...this.pedidos];

        this.nuevoPedido = {
          cliente: '',
          producto: '',
          cantidad: 1,
          total: 0,
          estado: 'PENDIENTE'
        };
        this.changeDetectorRef.markForCheck();
      },
      error: (err) => {
        this.guardando = false;
        this.error =
          `No fue posible confirmar la creación del pedido. HTTP ${err.status ?? 'desconocido'}.`;
        this.changeDetectorRef.markForCheck();

        // No reintentar automáticamente: el servidor podría
        // haber guardado el pedido antes del error.
      }
    });
  }
}
