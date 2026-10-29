package br.com.sistemas.chamados.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.sistemas.chamados.dto.DashboardResponse;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.service.DashboardService;

@WebMvcTest(DashboardController.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private DashboardService service;

    @Test
    void getDashboardRetornaResumo() throws Exception {
        when(service.resumo()).thenReturn(new DashboardResponse(3, 2, 1,
            Map.of(StatusChamado.ABERTO, 1L, StatusChamado.FECHADO, 1L),
            Map.of(Prioridade.ALTA, 2L)));

        mvc.perform(get("/dashboard"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(3))
            .andExpect(jsonPath("$.pendentesSemTecnico").value(1))
            .andExpect(jsonPath("$.porStatus.ABERTO").value(1))
            .andExpect(jsonPath("$.pendentesPorPrioridade.ALTA").value(2));
    }
}
