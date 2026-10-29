package br.com.sistemas.chamados.service;

import java.util.EnumMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sistemas.chamados.dto.DashboardResponse;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.repository.ChamadoRepository;

@Service
public class DashboardService {

    private final ChamadoRepository repository;

    public DashboardService(ChamadoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse resumo() {
        // O group by só devolve os grupos que existem; começa tudo em 0 para o
        // front-end receber sempre as mesmas chaves.
        Map<StatusChamado, Long> porStatus = zerado(StatusChamado.class);
        repository.contarPorStatus().forEach(t -> porStatus.put(t.getStatus(), t.getTotal()));

        Map<Prioridade, Long> pendentesPorPrioridade = zerado(Prioridade.class);
        repository.contarPorPrioridadeExceto(StatusChamado.FECHADO)
            .forEach(t -> pendentesPorPrioridade.put(t.getPrioridade(), t.getTotal()));

        long total = porStatus.values().stream().mapToLong(Long::longValue).sum();
        long pendentes = total - porStatus.get(StatusChamado.FECHADO);
        long semTecnico = repository.countByStatusNotAndTecnicoIsNull(StatusChamado.FECHADO);

        return new DashboardResponse(total, pendentes, semTecnico, porStatus, pendentesPorPrioridade);
    }

    private static <E extends Enum<E>> Map<E, Long> zerado(Class<E> tipo) {
        Map<E, Long> mapa = new EnumMap<>(tipo);
        for (E valor : tipo.getEnumConstants()) {
            mapa.put(valor, 0L);
        }
        return mapa;
    }
}
