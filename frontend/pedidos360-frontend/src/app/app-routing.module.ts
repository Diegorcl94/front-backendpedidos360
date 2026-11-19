import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { MsalGuard } from '@azure/msal-angular';
import { InicioComponent } from './pages/inicio/inicio.component';
import { PedidosComponent } from './pages/pedidos/pedidos.component';
import { EstadisticasComponent } from './pages/estadisticas/estadisticas.component';
import { KafkaAdminComponent } from './pages/kafka-admin/kafka-admin.component';

const routes: Routes = [
  { path: '', component: InicioComponent },
  {
    path: 'pedidos',
    component: PedidosComponent,
    canActivate: [MsalGuard]
  },
  {
    path: 'estadisticas',
    component: EstadisticasComponent,
    canActivate: [MsalGuard]
  },
  {
    path: 'kafka-admin',
    component: KafkaAdminComponent,
    canActivate: [MsalGuard]
  },
  { path: '**', redirectTo: '' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}
