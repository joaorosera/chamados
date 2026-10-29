package br.com.sistemas.chamados.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.sistemas.chamados.dto.ChamadoRequest;
import br.com.sistemas.chamados.dto.ChamadoResponse;
import br.com.sistemas.chamados.dto.HistoricoStatusResponse;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.exception.ChamadoFechadoException;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.service.ChamadoService;

@WebMvcTest(ChamadoController.class)
class ChamadoControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ChamadoService service;

    private ChamadoResponse chamadoAberto() {
        return new ChamadoResponse(1L, "Impressora", "Nao liga", Prioridade.ALTA,
            StatusChamado.ABERTO, LocalDateTime.now(), null,
            1L, "Ana Souza", 1L, "Hardware", null, null);
    }

    @Test
    void postCriaChamadoERetorna201() throws Exception {
        when(service.criar(any(ChamadoRequest.class))).thenReturn(chamadoAberto());

        mvc.perform(post("/chamados").contentType(MediaType.APPLICATION_JSON)
                .content("{\"titulo\":\"Impressora\",\"descricao\":\"Nao liga\","
                    + "\"prioridade\":\"ALTA\",\"clienteId\":1,\"categoriaId\":1}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("ABERTO"));
    }

    @Test
    void postSemCamposObrigatoriosRetorna400() throws Exception {
        mvc.perform(post("/chamados").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detalhes.length()").value(5));
    }

    @Test
    void postComPrioridadeInexistenteRetorna400() throws Exception {
        mvc.perform(post("/chamados").contentType(MediaType.APPLICATION_JSON)
                .content("{\"titulo\":\"x\",\"descricao\":\"y\","
                    + "\"prioridade\":\"URGENTE\",\"clienteId\":1,\"categoriaId\":1}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro").value("Corpo da requisição inválido"));
    }

    @Test
    void postComClienteInexistenteRetorna404() throws Exception {
        when(service.criar(any(ChamadoRequest.class)))
            .thenThrow(new RecursoNaoEncontradoException("Cliente 999 não encontrado"));

        mvc.perform(post("/chamados").contentType(MediaType.APPLICATION_JSON)
                .content("{\"titulo\":\"x\",\"descricao\":\"y\","
                    + "\"prioridade\":\"BAIXA\",\"clienteId\":999,\"categoriaId\":1}"))
            .andExpect(status().isNotFound());
    }

    @Test
    void patchStatusDeChamadoFechadoRetorna409() throws Exception {
        when(service.alterarStatus(eq(1L), any())).thenThrow(new ChamadoFechadoException(1L));

        mvc.perform(patch("/chamados/1/status").contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"ABERTO\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.erro").value("Chamado 1 está fechado e não pode ser alterado"));
    }

    @Test
    void getHistoricoRetornaMudancasDeStatus() throws Exception {
        when(service.historico(1L)).thenReturn(List.of(
            new HistoricoStatusResponse(1L, null, StatusChamado.ABERTO, LocalDateTime.now()),
            new HistoricoStatusResponse(2L, StatusChamado.ABERTO, StatusChamado.FECHADO, LocalDateTime.now())));

        mvc.perform(get("/chamados/1/historico"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[1].statusAnterior").value("ABERTO"))
            .andExpect(jsonPath("$[1].statusNovo").value("FECHADO"));
    }

    @Test
    void getHistoricoDeChamadoInexistenteRetorna404() throws Exception {
        when(service.historico(999L))
            .thenThrow(new RecursoNaoEncontradoException("Chamado 999 não encontrado"));

        mvc.perform(get("/chamados/999/historico"))
            .andExpect(status().isNotFound());
    }

    @Test
    void filtroComStatusInvalidoRetorna400() throws Exception {
        mvc.perform(get("/chamados").param("status", "XYZ"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro").value("Parâmetro inválido"));
    }
}
