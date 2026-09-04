import { Component, OnInit } from '@angular/core';
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

  error = '';

  constructor(
    private readonly pedidosService: PedidosService
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
      },

      error: (err) => {

        this.error =
          `No fue posible cargar los pedidos. HTTP ${err.status ?? 'desconocido'}.`;

        this.cargando = false;
      }
    });
  }
}
