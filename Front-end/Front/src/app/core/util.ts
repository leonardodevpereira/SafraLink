import { HttpErrorResponse, HttpParams } from '@angular/common/http';
import { AbstractControl, ValidationErrors } from '@angular/forms';

export function mensagemDeErro(erro: unknown, padrao = 'Não foi possível concluir a operação.'): string {
  if (erro instanceof HttpErrorResponse) {
    if (erro.status === 0) {
      return 'Sem conexão com o servidor. Verifique sua internet e tente novamente.';
    }
    const corpo = erro.error as { mensagem?: string; campos?: Record<string, string> } | null;
    if (corpo?.campos) {
      const primeiro = Object.values(corpo.campos)[0];
      if (primeiro) {
        return primeiro;
      }
    }
    if (corpo?.mensagem) {
      return corpo.mensagem;
    }
  }
  return padrao;
}

export function parametros(valores: Record<string, string | number | boolean | null | undefined>): HttpParams {
  let params = new HttpParams();
  for (const [chave, valor] of Object.entries(valores)) {
    if (valor !== null && valor !== undefined && valor !== '') {
      params = params.set(chave, String(valor));
    }
  }
  return params;
}

export function somenteDigitos(valor: string | null | undefined): string {
  return (valor ?? '').replace(/\D/g, '');
}

export function cpfValido(valor: string | null | undefined): boolean {
  const cpf = somenteDigitos(valor);
  if (cpf.length !== 11 || /^(\d)\1{10}$/.test(cpf)) {
    return false;
  }
  const digito = (tamanho: number): number => {
    let soma = 0;
    for (let i = 0; i < tamanho; i++) {
      soma += Number(cpf[i]) * (tamanho + 1 - i);
    }
    const resto = (soma * 10) % 11;
    return resto === 10 ? 0 : resto;
  };
  return digito(9) === Number(cpf[9]) && digito(10) === Number(cpf[10]);
}

export function validarCpf(controle: AbstractControl): ValidationErrors | null {
  const valor = controle.value as string | null;
  return !valor || cpfValido(valor) ? null : { cpf: true };
}

export function formatarCpf(valor: string | null | undefined): string {
  const d = somenteDigitos(valor);
  return d.length === 11 ? `${d.slice(0, 3)}.${d.slice(3, 6)}.${d.slice(6, 9)}-${d.slice(9)}` : (valor ?? '');
}

export function senhasIguais(campo: string, confirmacao: string) {
  return (grupo: AbstractControl): ValidationErrors | null => {
    const a = grupo.get(campo)?.value as string | null;
    const b = grupo.get(confirmacao)?.value as string | null;
    return a && b && a !== b ? { senhasDiferentes: true } : null;
  };
}

export function hojeISO(): string {
  const d = new Date();
  const mes = String(d.getMonth() + 1).padStart(2, '0');
  const dia = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${mes}-${dia}`;
}
