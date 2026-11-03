package br.com.sistemas.chamados.dto;

import java.util.Map;

import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;

/**
 * Resumo para o dashboard. "Pendente" = todo chamado que ainda não está FECHADO.
 * Os mapas trazem todos os valores do enum, com 0 quando não há chamados.
 */
public record DashboardResponse(
    long total,
    long pendentes,
    long pendentesSemTecnico,
    Map<StatusChamado, Long> porStatus,
    Map<Prioridade, Long> pendentesPorPrioridade
) { }
