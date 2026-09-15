import { Routes } from '@angular/router';
import { authGuard, approverGuard } from './core/guards';

export const routes: Routes = [
  {
    path: 'connexion',
    title: 'Connexion — Trésorerie Générale du Royaume',
    loadComponent: () => import('./features/auth/login.component').then((m) => m.LoginComponent),
  },
  {
    path: '',
    loadComponent: () => import('./features/layout/shell.component').then((m) => m.ShellComponent),
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'tableau-de-bord' },
      {
        path: 'tableau-de-bord',
        title: 'Tableau de bord — Trésorerie Générale du Royaume',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent),
      },
      {
        path: 'demandes',
        title: 'Demandes — Trésorerie Générale du Royaume',
        canActivate: [approverGuard],
        loadComponent: () =>
          import('./features/requests/requests-list.component').then((m) => m.RequestsListComponent),
      },
      {
        path: 'employes',
        title: 'Employés — Trésorerie Générale du Royaume',
        canActivate: [approverGuard],
        loadComponent: () =>
          import('./features/employees/employees.component').then((m) => m.EmployeesComponent),
      },
      {
        path: 'demandes/:id',
        title: 'Demande — Trésorerie Générale du Royaume',
        loadComponent: () =>
          import('./features/requests/request-detail.component').then((m) => m.RequestDetailComponent),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
