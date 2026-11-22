package br.com.safralink.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DocumentosTest {

    @Test
    void aceitaCpfValidoComOuSemMascara() {
        assertThat(Documentos.cpfValido("529.982.247-25")).isTrue();
        assertThat(Documentos.cpfValido("11144477735")).isTrue();
    }

    @Test
    void recusaCpfInvalido() {
        assertThat(Documentos.cpfValido("529.982.247-24")).isFalse();
        assertThat(Documentos.cpfValido("111.111.111-11")).isFalse();
        assertThat(Documentos.cpfValido("123")).isFalse();
        assertThat(Documentos.cpfValido(null)).isFalse();
    }

    @Test
    void validaCnpj() {
        assertThat(Documentos.cnpjValido("11.222.333/0001-81")).isTrue();
        assertThat(Documentos.cnpjValido("11.222.333/0001-82")).isFalse();
        assertThat(Documentos.cnpjValido("00000000000000")).isFalse();
    }
}
