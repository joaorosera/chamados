package br.com.sistemas.chamados.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import br.com.sistemas.chamados.config.SecurityConfig;
import br.com.sistemas.chamados.dto.ChamadoRequest;
import br.com.sistemas.chamados.dto.ChamadoResponse;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.service.CategoriaService;
import br.com.sistemas.chamados.service.ChamadoService;
import br.com.sistemas.chamados.service.ClienteService;
import br.com.sistemas.chamados.service.DashboardService;
import br.com.sistemas.chamados.service.TecnicoService;

/**
 * Permissões por papel, com os usuários de verdade do SecurityConfig (HTTP Basic
 * e senha conferida pelo PasswordEncoder). Os Services são mocks: aqui só importa
 * se a requisição chega ao controller (2xx) ou é barrada (401/403).
 */
@WebMvcTest(controllers = {ChamadoController.class, ClienteController.class,
    CategoriaController.class, TecnicoController.class, DashboardController.class})
@Import(SecurityConfig.class)
class SegurancaTest {

    private static final RequestPostProcessor USUARIO = httpBasic("usuario", "usuario123");
    private static final RequestPostProcessor TECNICO = httpBasic("tecnico", "tecnico123");
    private static final RequestPostProcessor ADMIN = httpBasic("admin", "admin123");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ChamadoService chamadoService;
    @MockitoBean
    private ClienteService clienteService;
    @MockitoBean
    private CategoriaService categoriaService;
    @MockitoBean
    private TecnicoService tecnicoService;
    @MockitoBean
    private DashboardService dashboardService;

    @Test
    void semCredenciaisRetorna401NoFormatoDaApi() throws Exception {
        mvc.perform(get("/clientes"))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string("WWW-Authenticate", "Basic realm=\"chamados\""))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.erro").value("Autenticação necessária"));
    }

    @Test
    void senhaErradaRetorna401() throws Exception {
        mvc.perform(get("/clientes").with(httpBasic("admin", "errada")))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioConsultaEAbreChamado() throws Exception {
        when(chamadoService.criar(any(ChamadoRequest.class))).thenReturn(new ChamadoResponse(
            1L, "Impressora", "Nao liga", Prioridade.ALTA, StatusChamado.ABERTO,
            LocalDateTime.now(), null, 1L, "Ana", 1L, "Hardware", null, null));

        mvc.perform(get("/clientes").with(USUARIO)).andExpect(status().isOk());
        mvc.perform(get("/categorias").with(USUARIO)).andExpect(status().isOk());
        mvc.perform(post("/chamados").with(USUARIO).contentType(MediaType.APPLICATION_JSON)
                .content("{\"titulo\":\"Impressora\",\"descricao\":\"Nao liga\","
                    + "\"prioridade\":\"ALTA\",\"clienteId\":1,\"categoriaId\":1}"))
            .andExpect(status().isCreated());
    }

    @Test
    void usuarioNaoAlteraStatusMasTecnicoAltera() throws Exception {
        mvc.perform(patch("/chamados/1/status").with(USUARIO)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"EM_ATENDIMENTO\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.erro").value("Acesso negado"));

        mvc.perform(patch("/chamados/1/status").with(TECNICO)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"EM_ATENDIMENTO\"}"))
            .andExpect(status().isOk());
    }

    @Test
    void soAdminAtribuiTecnico() throws Exception {
        mvc.perform(patch("/chamados/1/atribuir").with(TECNICO)
                .contentType(MediaType.APPLICATION_JSON).content("{\"tecnicoId\":1}"))
            .andExpect(status().isForbidden());

        mvc.perform(patch("/chamados/1/atribuir").with(ADMIN)
                .contentType(MediaType.APPLICATION_JSON).content("{\"tecnicoId\":1}"))
            .andExpect(status().isOk());
    }

    @Test
    void soAdminVeODashboard() throws Exception {
        mvc.perform(get("/dashboard").with(TECNICO)).andExpect(status().isForbidden());
        mvc.perform(get("/dashboard").with(ADMIN)).andExpect(status().isOk());
    }

    @Test
    void soAdminMantemCategoriasETecnicos() throws Exception {
        mvc.perform(post("/categorias").with(TECNICO).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Rede\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/tecnicos").with(USUARIO).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"X\",\"email\":\"x@empresa.com\",\"perfil\":\"TECNICO\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void soAdminExclui() throws Exception {
        mvc.perform(delete("/clientes/1").with(TECNICO)).andExpect(status().isForbidden());
        mvc.perform(delete("/clientes/1").with(ADMIN)).andExpect(status().isNoContent());
    }
}
