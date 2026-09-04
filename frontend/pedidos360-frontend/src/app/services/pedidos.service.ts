import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Pedido {
  id: number;
  cliente: string;
  producto: string;
  cantidad: number;
  total: number;
  estado: string;
}

@Injectable({ providedIn: 'root' })
export class PedidosService {
  private readonly apiUrl = `${environment.apiBaseUrl}/api/pedidos`;

  constructor(private readonly http: HttpClient) {}

  listar(): Observable<Pedido[]> {
    return this.http.get<Pedido[]>(this.apiUrl);
  }
}
