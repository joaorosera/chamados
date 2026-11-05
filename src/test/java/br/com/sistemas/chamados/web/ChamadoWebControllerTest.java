package br.com.sistemas.chamados.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.sistemas.chamados.config.SecurityConfig;
import br.com.sistemas.chamados.dto.CategoriaResponse;
import br.com.sistemas.chamados.dto.ChamadoRequest;
import br.com.sistemas.chamados.dto.ChamadoResponse;
import br.com.sistemas.chamados.dto.ClienteResponse;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.service.CategoriaService;
import br.com.sistemas.chamados.service.ChamadoService;
import br.com.sistemas.chamados.service.ClienteService;

@WebMvcTest(ChamadoWebController.class)
@Import(SecurityConfig.class)
@WithMockUser(username = "usuario", roles = "USUARIO")
class ChamadoWebControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ChamadoService chamadoService;
    @MockitoBean
    private ClienteService clienteService;
    @MockitoBean
    private CategoriaService categoriaService;

    private final ChamadoResponse impressora = new ChamadoResponse(7L, "Impressora não liga", "Setor financeiro",
        Prioridade.ALTA, StatusChamado.EM_ATENDIMENTO, LocalDateTime.of(2026, 11, 2, 9, 30), null,
        1L, "Ana Souza", 1L, "Hardware", 3L, "Carlos");

    @BeforeEach
    void preparar() {
        when(clienteService.listar()).thenReturn(List.of(new ClienteResponse(1L, "Ana Souza", "ana@exemplo.com", null)));
        when(categoriaService.listar()).thenReturn(List.of(new CategoriaResponse(1L, "Hardware", null)));
    }

    @Test
    void listaMostraOsChamadosComNomesLegiveis() throws Exception {
        when(chamadoService.listar(isNull(), isNull(), isNull(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(impressora)));

        mvc.perform(get("/app/chamados"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Impressora não liga")))
            .andExpect(content().string(containsString("Em atendimento")))
            .andExpect(content().string(containsString("02/11/2026 09:30")));
    }

    @Test
    void filtrosSaoRepassadosAoService() throws Exception {
        when(chamadoService.listar(any(), any(), any(), any(Pageable.class))).thenReturn(Page.empty());

        mvc.perform(get("/app/chamados").param("status", "ABERTO").param("prioridade", "ALTA")
                .param("clienteId", "1").param("pagina", "2"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Nenhum chamado encontrado")))
            // o select continua marcado depois de filtrar
            .andExpect(content().string(containsString("value=\"ABERTO\" selected=\"selected\"")));

        verify(chamadoService).listar(eq(StatusChamado.ABERTO), eq(Prioridade.ALTA), eq(1L),
            eq(PageRequest.of(2, 10, Sort.by(Sort.Direction.DESC, "dataAbertura"))));
    }

    @Test
    void filtroVazioContaComoSemFiltro() throws Exception {
        when(chamadoService.listar(any(), any(), any(), any(Pageable.class))).thenReturn(Page.empty());

        mvc.perform(get("/app/chamados").param("status", "").param("prioridade", "").param("clienteId", ""))
            .andExpect(status().isOk());

        verify(chamadoService).listar(isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void novoComClienteJaEscolhido() throws Exception {
        mvc.perform(get("/app/chamados/novo").param("clienteId", "1"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("value=\"1\" selected=\"selected\">Ana Souza")));
    }

    @Test
    void abrirChamadoValidoVoltaParaALista() throws Exception {
        when(chamadoService.criar(any(ChamadoRequest.class))).thenReturn(impressora);

        mvc.perform(post("/app/chamados").with(csrf())
                .param("titulo", "Impressora não liga").param("descricao", "Setor financeiro")
                .param("prioridade", "ALTA").param("clienteId", "1").param("categoriaId", "1"))
            .andExpect(redirectedUrl("/app/chamados"))
            .andExpect(flash().attribute("sucesso", "Chamado #7 aberto."));

        verify(chamadoService).criar(new ChamadoRequest("Impressora não liga", "Setor financeiro",
            Prioridade.ALTA, 1L, 1L));
    }

    @Test
    void abrirChamadoSemCamposMostraOsErros() throws Exception {
        mvc.perform(post("/app/chamados").with(csrf()).param("titulo", ""))
            .andExpect(view().name("chamados/form"))
            .andExpect(content().string(containsString("Título é obrigatório")))
            .andExpect(content().string(containsString("Cliente é obrigatório")))
            // os selects continuam com as opções depois do erro
            .andExpect(content().string(containsString("Hardware")));

        verify(chamadoService, never()).criar(any());
    }
}
