package br.com.sistemas.chamados.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.sistemas.chamados.config.SecurityConfig;
import br.com.sistemas.chamados.dto.ClienteRequest;
import br.com.sistemas.chamados.dto.ClienteResponse;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.service.ChamadoService;
import br.com.sistemas.chamados.service.ClienteService;

@WebMvcTest(ClienteWebController.class)
@Import(SecurityConfig.class)
@WithMockUser(username = "usuario", roles = "USUARIO")
class ClienteWebControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ClienteService clienteService;
    @MockitoBean
    private ChamadoService chamadoService;

    private final ClienteResponse ana = new ClienteResponse(1L, "Ana Souza", "ana@exemplo.com", null);

    @Test
    void listaMostraOsClientes() throws Exception {
        when(clienteService.listar()).thenReturn(List.of(ana));

        mvc.perform(get("/app/clientes"))
            .andExpect(status().isOk())
            .andExpect(view().name("clientes/lista"))
            .andExpect(content().string(containsString("ana@exemplo.com")));
    }

    @Test
    void cadastroValidoRedirecionaParaODetalhe() throws Exception {
        when(clienteService.criar(any(ClienteRequest.class))).thenReturn(ana);

        mvc.perform(post("/app/clientes").with(csrf())
                .param("nome", "Ana Souza").param("email", "ana@exemplo.com"))
            .andExpect(redirectedUrl("/app/clientes/1"))
            .andExpect(flash().attribute("sucesso", "Cliente cadastrado."));
    }

    @Test
    void cadastroInvalidoVoltaParaOFormularioComOsErros() throws Exception {
        mvc.perform(post("/app/clientes").with(csrf())
                .param("nome", "").param("email", "isso-nao-e-email"))
            .andExpect(status().isOk())
            .andExpect(view().name("clientes/form"))
            .andExpect(content().string(containsString("Nome é obrigatório")))
            .andExpect(content().string(containsString("E-mail inválido")));

        verify(clienteService, never()).criar(any());
    }

    @Test
    void emailRepetidoApareceNoCampo() throws Exception {
        when(clienteService.criar(any(ClienteRequest.class)))
            .thenThrow(new RegraNegocioException("Já existe cliente com o e-mail ana@exemplo.com"));

        mvc.perform(post("/app/clientes").with(csrf())
                .param("nome", "Ana Souza").param("email", "ana@exemplo.com"))
            .andExpect(view().name("clientes/form"))
            .andExpect(content().string(containsString("Já existe cliente com o e-mail ana@exemplo.com")));
    }

    @Test
    void detalheMostraClienteEChamados() throws Exception {
        when(clienteService.buscar(1L)).thenReturn(ana);
        when(chamadoService.listarPorCliente(eq(1L), any())).thenReturn(Page.empty());

        mvc.perform(get("/app/clientes/1"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Ana Souza")))
            .andExpect(content().string(containsString("ainda não abriu chamados")))
            // usuário comum não vê o botão de excluir
            .andExpect(content().string(org.hamcrest.Matchers.not(containsString("/excluir"))));
    }

    @Test
    void clienteInexistenteMostraPagina404() throws Exception {
        when(clienteService.buscar(99L)).thenThrow(new RecursoNaoEncontradoException("Cliente 99 não encontrado"));

        mvc.perform(get("/app/clientes/99"))
            .andExpect(status().isNotFound())
            .andExpect(view().name("error/404"))
            .andExpect(content().string(containsString("Cliente 99 não encontrado")));
    }

    @Test
    void usuarioNaoPodeExcluir() throws Exception {
        mvc.perform(post("/app/clientes/1/excluir").with(csrf()))
            .andExpect(status().isForbidden());

        verify(clienteService, never()).excluir(any());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminExcluiEVoltaParaALista() throws Exception {
        mvc.perform(post("/app/clientes/1/excluir").with(csrf()))
            .andExpect(redirectedUrl("/app/clientes"))
            .andExpect(flash().attribute("sucesso", "Cliente excluído."));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void clienteComChamadosNaoEExcluido() throws Exception {
        doThrow(new RegraNegocioException("Cliente 1 possui chamados e não pode ser excluído"))
            .when(clienteService).excluir(1L);

        mvc.perform(post("/app/clientes/1/excluir").with(csrf()))
            .andExpect(redirectedUrl("/app/clientes/1"))
            .andExpect(flash().attribute("erro", "Cliente 1 possui chamados e não pode ser excluído"));
    }
}
