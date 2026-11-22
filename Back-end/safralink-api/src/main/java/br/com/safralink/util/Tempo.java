package br.com.safralink.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public final class Tempo {
    public static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");

    private Tempo() {}

    public static Instant inicioDoDia(LocalDate data) {
        return data.atStartOfDay(ZONA).toInstant();
    }

    public static Instant fimDoDiaExclusivo(LocalDate data) {
        return data.plusDays(1).atStartOfDay(ZONA).toInstant();
    }

    public static LocalDate hoje() {
        return LocalDate.now(ZONA);
    }
}
