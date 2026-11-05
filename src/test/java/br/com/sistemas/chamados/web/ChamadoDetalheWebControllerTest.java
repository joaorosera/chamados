package br.com.sistemas.chamados.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.sistemas.chamados.config.SecurityConfig;
import br.com.sistemas.chamados.dto.ChamadoDetalheResponse;
import br.com.sistemas.chamados.dto.ComentarioRequest;
import br.com.sistemas.chamados.dto.ComentarioResponse;
import br.com.sistemas.chamados.dto.HistoricoStatusResponse;
import br.com.sistemas.chamados.dto.TecnicoResponse;
import br.com.sistemas.chamados.entity.Perfil;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.exception.ChamadoFechadoException;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.service.ChamadoService;
import br.com.sistemas.chamados.service.ComentarioService;
import br.com.sistemas.chamados.service.TecnicoService;

@WebMvcTest(ChamadoDetalheWebController.class)
@Import(SecurityConfig.class)
@WithMockUser(username = "usuario", roles = "USUARIO")
class ChamadoDetalheWebControllerTest {

    private static final LocalDateTime ABERTURA = LocalDateTime.of(2026, 11, 2, 9, 30);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ChamadoService chamadoService;
    @MockitoBean
    private ComentarioService comentarioService;
    @MockitoBean
    private TecnicoService tecnicoService;

    private ChamadoDetalheResponse chamado(StatusChamado status) {
        return new ChamadoDetalheResponse(1L, "Impressora não liga", "Setor financeiro", Prioridade.ALTA,
            status, ABERTURA, status == StatusChamado.FECHADO ? ABERTURA.plusDays(1) : null,
            1L, "Ana Souza", 1L, "Hardware", null, null,
            List.of(new ComentarioResponse(10L, "Verificando o cabo", "tecnico", ABERTURA.plusHours(1))));
    }

    @BeforeEach
    void preparar() {
        when(chamadoService.buscar(1L)).thenReturn(chamado(StatusChamado.EM_ATENDIMENTO));
        when(chamadoService.historico(1L)).thenReturn(List.of(
            new HistoricoStatusResponse(1L, null, StatusChamado.ABERTO, ABERTURA),
            new HistoricoStatusResponse(2L, StatusChamado.ABERTO, StatusChamado.EM_ATENDIMENTO, ABERTURA)));
        when(tecnicoService.listar()).thenReturn(List.of(
            new TecnicoResponse(3L, "Carlos Lima", "carlos@empresa.com", Perfil.TECNICO)));
    }

    @Test
    void usuarioVeDadosComentariosEHistoricoMasNaoAsAcoes() throws Exception {
        mvc.perform(get("/app/chamados/1"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Impressora não liga")))
            .andExpect(content().string(containsString("Verificando o cabo")))
            .andExpect(content().string(containsString("Aberto → Em atendimento")))
            .andExpect(content().string(containsString("Novo comentário")))
            .andExpect(content().string(not(containsString("Alterar status"))))
            .andExpect(content().string(not(containsString("Atribuir técnico"))));
    }

    @Test
    @WithMockUser(username = "tecnico", roles = "TECNICO")
    void tecnicoVeAlterarStatusMasNaoAtribuir() throws Exception {
        mvc.perform(get("/app/chamados/1"))
            .andExpect(content().string(containsString("Alterar status")))
            .andExpect(content().string(not(containsString("Atribuir técnico"))));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminVeAsDuasAcoes() throws Exception {
        mvc.perform(get("/app/chamados/1"))
            .andExpect(content().string(containsString("Alterar status")))
            .andExpect(content().string(containsString("Atribuir técnico")))
            .andExpect(content().string(containsString("Carlos Lima")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void chamadoFechadoNaoMostraAcoesNemFormularioDeComentario() throws Exception {
        when(chamadoService.buscar(1L)).thenReturn(chamado(StatusChamado.FECHADO));

        mvc.perform(get("/app/chamados/1"))
            .andExpect(content().string(containsString("Chamado fechado em <span>03/11/2026 09:30</span>")))
            .andExpect(content().string(not(containsString("Novo comentário"))))
            .andExpect(content().string(not(containsString("Alterar status"))));
    }

    @Test
    void comentarioUsaOUsuarioLogadoComoAutor() throws Exception {
        mvc.perform(post("/app/chamados/1/comentarios").with(csrf()).param("texto", "  Ainda sem papel  "))
            .andExpect(redirectedUrl("/app/chamados/1"))
            .andExpect(flash().attribute("sucesso", "Comentário registrado."));

        verify(comentarioService).criar(1L, new ComentarioRequest("Ainda sem papel", "usuario"));
    }

    @Test
    void comentarioEmBrancoNaoESalvo() throws Exception {
        mvc.perform(post("/app/chamados/1/comentarios").with(csrf()).param("texto", "   "))
            .andExpect(redirectedUrl("/app/chamados/1"))
            .andExpect(flash().attribute("erro", "O comentário deve ter entre 1 e 1000 caracteres."));

        verify(comentarioService, never()).criar(anyLong(), any());
    }

    @Test
    void comentarioEmChamadoFechadoMostraOErro() throws Exception {
        when(comentarioService.criar(anyLong(), any())).thenThrow(new ChamadoFechadoException(1L));

        mvc.perform(post("/app/chamados/1/comentarios").with(csrf()).param("texto", "Oi"))
            .andExpect(flash().attribute("erro", "Chamado 1 está fechado e não pode ser alterado"));
    }

    @Test
    void usuarioNaoAlteraStatus() throws Exception {
        mvc.perform(post("/app/chamados/1/status").with(csrf()).param("status", "FECHADO"))
            .andExpect(status().isForbidden());

        verify(chamadoService, never()).alterarStatus(anyLong(), any());
    }

    @Test
    @WithMockUser(username = "tecnico", roles = "TECNICO")
    void tecnicoAlteraStatus() throws Exception {
        mvc.perform(post("/app/chamados/1/status").with(csrf()).param("status", "FECHADO"))
            .andExpect(redirectedUrl("/app/chamados/1"))
            .andExpect(flash().attribute("sucesso", "Status alterado."));

        verify(chamadoService).alterarStatus(1L, StatusChamado.FECHADO);
    }

    @Test
    @WithMockUser(username = "tecnico", roles = "TECNICO")
    void mesmoStatusMostraAMensagemDoService() throws Exception {
        when(chamadoService.alterarStatus(1L, StatusChamado.EM_ATENDIMENTO))
            .thenThrow(new RegraNegocioException("Chamado 1 já está com status EM_ATENDIMENTO"));

        mvc.perform(post("/app/chamados/1/status").with(csrf()).param("status", "EM_ATENDIMENTO"))
            .andExpect(flash().attribute("erro", "Chamado 1 já está com status EM_ATENDIMENTO"));
    }

    @Test
    @WithMockUser(username = "tecnico", roles = "TECNICO")
    void tecnicoNaoAtribui() throws Exception {
        mvc.perform(post("/app/chamados/1/atribuir").with(csrf()).param("tecnicoId", "3"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminAtribuiTecnico() throws Exception {
        mvc.perform(post("/app/chamados/1/atribuir").with(csrf()).param("tecnicoId", "3"))
            .andExpect(redirectedUrl("/app/chamados/1"))
            .andExpect(flash().attribute("sucesso", "Técnico atribuído."));

        verify(chamadoService).atribuir(1L, 3L);
    }
}
