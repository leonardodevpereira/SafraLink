import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatBadgeModule } from '@angular/material/badge';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialogModule } from '@angular/material/dialog';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatListModule } from '@angular/material/list';
import { MatMenuModule } from '@angular/material/menu';
import { MatPaginatorIntl, MatPaginatorModule } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterModule } from '@angular/router';
import { AppShell } from './app-shell/app-shell';
import { ConfirmDialog, PromptDialog } from './dialogs/dialogs';
import { DocumentoPipe } from './documento.pipe';
import { GraoIcon } from './grao-icon/grao-icon';
import { PaginatorPtBr } from './paginator-ptbr';
import { Perfil } from './perfil/perfil';
import { PriceChart } from './price-chart/price-chart';
import { ProdutorSelect } from './produtor-select/produtor-select';
import { RequisicaoDetalheDialog } from './requisicao-detalhe/requisicao-detalhe';
import { StatusChip } from './status-chip/status-chip';
import { TransacaoDetalheDialog } from './transacao-detalhe/transacao-detalhe-dialog';

const MATERIAL = [
  MatAutocompleteModule,
  MatBadgeModule,
  MatButtonModule,
  MatButtonToggleModule,
  MatCardModule,
  MatCheckboxModule,
  MatChipsModule,
  MatDialogModule,
  MatDividerModule,
  MatFormFieldModule,
  MatIconModule,
  MatInputModule,
  MatListModule,
  MatMenuModule,
  MatPaginatorModule,
  MatProgressBarModule,
  MatProgressSpinnerModule,
  MatSelectModule,
  MatSidenavModule,
  MatSlideToggleModule,
  MatSnackBarModule,
  MatTableModule,
  MatToolbarModule,
  MatTooltipModule,
];

const COMPONENTES = [
  AppShell,
  StatusChip,
  PriceChart,
  ProdutorSelect,
  GraoIcon,
  ConfirmDialog,
  PromptDialog,
  RequisicaoDetalheDialog,
  TransacaoDetalheDialog,
  Perfil,
  DocumentoPipe,
];

@NgModule({
  declarations: COMPONENTES,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, ...MATERIAL],
  exports: [CommonModule, ReactiveFormsModule, RouterModule, ...MATERIAL, ...COMPONENTES],
  providers: [{ provide: MatPaginatorIntl, useClass: PaginatorPtBr }],
})
export class SharedModule {}
