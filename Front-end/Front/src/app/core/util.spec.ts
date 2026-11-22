import { HttpErrorResponse } from '@angular/common/http';
import { FormControl, FormGroup } from '@angular/forms';
import { cpfValido, mensagemDeErro, parametros, senhasIguais, somenteDigitos, validarCpf } from './util';

describe('util', () => {
  describe('cpfValido', () => {
    it('aceita CPF válido com ou sem máscara', () => {
      expect(cpfValido('529.982.247-25')).toBe(true);
      expect(cpfValido('11144477735')).toBe(true);
    });

    it('recusa CPF inválido', () => {
      expect(cpfValido('529.982.247-24')).toBe(false);
      expect(cpfValido('111.111.111-11')).toBe(false);
      expect(cpfValido('123')).toBe(false);
      expect(cpfValido(null)).toBe(false);
    });

    it('validarCpf ignora campo vazio (quem exige é o required)', () => {
      expect(validarCpf(new FormControl(''))).toBeNull();
      expect(validarCpf(new FormControl('123'))).toEqual({ cpf: true });
    });
  });

  it('somenteDigitos remove a máscara', () => {
    expect(somenteDigitos('529.982.247-25')).toBe('52998224725');
    expect(somenteDigitos(undefined)).toBe('');
  });

  it('senhasIguais sinaliza senhas diferentes no grupo', () => {
    const grupo = new FormGroup(
      { senha: new FormControl('12345678'), confirmacao: new FormControl('12345679') },
      { validators: senhasIguais('senha', 'confirmacao') },
    );
    expect(grupo.hasError('senhasDiferentes')).toBe(true);
    grupo.controls.confirmacao.setValue('12345678');
    expect(grupo.hasError('senhasDiferentes')).toBe(false);
  });

  describe('mensagemDeErro', () => {
    it('usa a mensagem da API', () => {
      const erro = new HttpErrorResponse({ status: 422, error: { mensagem: 'Saldo insuficiente.' } });
      expect(mensagemDeErro(erro)).toBe('Saldo insuficiente.');
    });

    it('prioriza o erro do primeiro campo inválido', () => {
      const erro = new HttpErrorResponse({
        status: 400,
        error: { mensagem: 'Dados inválidos.', campos: { cpf: 'CPF inválido.' } },
      });
      expect(mensagemDeErro(erro)).toBe('CPF inválido.');
    });

    it('explica a falta de conexão e usa o texto padrão para o resto', () => {
      expect(mensagemDeErro(new HttpErrorResponse({ status: 0 }))).toContain('Sem conexão');
      expect(mensagemDeErro(new Error('x'), 'Falhou.')).toBe('Falhou.');
    });
  });

  it('parametros descarta valores vazios', () => {
    const params = parametros({ a: 1, b: '', c: null, d: undefined, e: 'x' });
    expect(params.keys().sort()).toEqual(['a', 'e']);
  });
});
