package br.com.safralink.util;

public final class Documentos {
    private Documentos() {}

    public static String somenteDigitos(String valor) {
        return valor == null ? null : valor.replaceAll("\\D", "");
    }

    public static boolean cpfValido(String valor) {
        String cpf = somenteDigitos(valor);
        if (cpf == null || cpf.length() != 11 || todosIguais(cpf)) {
            return false;
        }
        return digito(cpf, 9, 10) == cpf.charAt(9) - '0' && digito(cpf, 10, 11) == cpf.charAt(10) - '0';
    }

    public static boolean cnpjValido(String valor) {
        String cnpj = somenteDigitos(valor);
        if (cnpj == null || cnpj.length() != 14 || todosIguais(cnpj)) {
            return false;
        }
        int[] p1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] p2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        return digitoCnpj(cnpj, p1) == cnpj.charAt(12) - '0' && digitoCnpj(cnpj, p2) == cnpj.charAt(13) - '0';
    }

    private static boolean todosIguais(String s) {
        return s.chars().allMatch(c -> c == s.charAt(0));
    }

    private static int digito(String cpf, int tamanho, int pesoInicial) {
        int soma = 0;
        for (int i = 0; i < tamanho; i++) {
            soma += (cpf.charAt(i) - '0') * (pesoInicial - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }

    private static int digitoCnpj(String cnpj, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += (cnpj.charAt(i) - '0') * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
