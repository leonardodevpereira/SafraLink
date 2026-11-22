import { Component, DestroyRef, OnInit, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormResetEvent, TouchedChangeEvent } from '@angular/forms';
import { ErrorStateMatcher } from '@angular/material/core';
import { filter } from 'rxjs';
import { ProdutorResumo } from '../../core/models';

const MAXIMO_NA_LISTA = 50;

function normalizar(valor: string): string {
  return valor
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
    .trim();
}

@Component({
  selector: 'app-produtor-select',
  standalone: false,
  templateUrl: './produtor-select.html',
  styleUrl: './produtor-select.scss',
})
export class ProdutorSelect implements OnInit {
  readonly produtores = input.required<ProdutorResumo[]>();
  readonly controle = input.required<FormControl<number | null>>();
  readonly rotulo = input('Produtor');
  readonly obrigatorio = input(false);
  readonly compacto = input(false);

  private readonly destroyRef = inject(DestroyRef);

  protected readonly texto = new FormControl<string | ProdutorResumo>('', { nonNullable: true });
  private readonly termo = signal('');

  protected readonly filtrados = computed(() => {
    const busca = normalizar(this.termo());
    const lista = this.produtores();
    if (!busca) {
      return lista.slice(0, MAXIMO_NA_LISTA);
    }
    const digitos = busca.replace(/\D/g, '');
    return lista
      .filter((p) => normalizar(p.nome).includes(busca) || (digitos.length >= 3 && p.cpf.includes(digitos)))
      .slice(0, MAXIMO_NA_LISTA);
  });

  protected readonly exibir = (valor: ProdutorResumo | string | null): string =>
    typeof valor === 'string' ? valor : (valor?.nome ?? '');

  protected readonly matcher: ErrorStateMatcher = {
    isErrorState: () =>
      this.obrigatorio() && this.controle().value === null && (this.texto.touched || this.controle().touched),
  };

  ngOnInit(): void {
    const controle = this.controle();
    this.sincronizar(controle.value);

    this.texto.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((valor) => {
      if (typeof valor === 'string') {
        this.termo.set(valor);
        if (controle.value !== null) {
          controle.setValue(null);
        }
      } else {
        this.termo.set('');
        controle.setValue(valor.id);
      }
    });

    controle.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((valor) => this.sincronizar(valor));

    controle.events
      .pipe(
        filter((e) => e instanceof FormResetEvent),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => this.zerarTexto());

    controle.events
      .pipe(
        filter((e) => e instanceof TouchedChangeEvent && e.touched),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => this.texto.markAsTouched());
  }

  protected limpar(): void {
    this.zerarTexto();
    this.controle().setValue(null);
  }

  private zerarTexto(): void {
    this.texto.setValue('', { emitEvent: false });
    this.termo.set('');
  }

  private sincronizar(id: number | null): void {
    if (id === null) {
      if (typeof this.texto.value !== 'string') {
        this.zerarTexto();
      }
      return;
    }
    const produtor = this.produtores().find((p) => p.id === id);
    if (produtor && this.texto.value !== produtor) {
      this.texto.setValue(produtor, { emitEvent: false });
    }
  }
}
