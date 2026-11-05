package br.com.sistemas.chamados.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.sistemas.chamados.config.SecurityConfig;
import br.com.sistemas.chamados.dto.CategoriaRequest;
import br.com.sistemas.chamados.dto.CategoriaResponse;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.service.CategoriaService;

@WebMvcTest(CategoriaWebController.class)
@Import(SecurityConfig.class)
@WithMockUser(username = "admin", roles = "ADMIN")
class CategoriaWebControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CategoriaService service;

    @Test
    @WithMockUser(username = "usuario", roles = "USUARIO")
    void usuarioVeAListaSemOsBotoesDeAdmin() throws Exception {
        when(service.listar()).thenReturn(List.of(new CategoriaResponse(1L, "Hardware", null)));

        mvc.perform(get("/app/categorias"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Hardware")))
            .andExpect(content().string(not(containsString("Nova categoria"))))
            .andExpect(content().string(not(containsString("/excluir"))));
    }

    @Test
    @WithMockUser(username = "tecnico", roles = "TECNICO")
    void tecnicoNaoCadastraCategoria() throws Exception {
        mvc.perform(get("/app/categorias/novo")).andExpect(status().isForbidden());
        mvc.perform(post("/app/categorias").with(csrf()).param("nome", "Rede"))
            .andExpect(status().isForbidden());

        verify(service, never()).criar(any());
    }

    @Test
    void adminCadastraEVoltaParaALista() throws Exception {
        mvc.perform(post("/app/categorias").with(csrf()).param("nome", "Rede").param("descricao", "Wi-Fi"))
            .andExpect(redirectedUrl("/app/categorias"))
            .andExpect(flash().attribute("sucesso", "Categoria cadastrada."));

        verify(service).criar(new CategoriaRequest("Rede", "Wi-Fi"));
    }

    @Test
    void nomeRepetidoApareceNoCampo() throws Exception {
        when(service.criar(any(CategoriaRequest.class)))
            .thenThrow(new RegraNegocioException("Já existe categoria com o nome Rede"));

        mvc.perform(post("/app/categorias").with(csrf()).param("nome", "Rede"))
            .andExpect(view().name("categorias/form"))
            .andExpect(content().string(containsString("Já existe categoria com o nome Rede")));
    }

    @Test
    void edicaoAbreOFormularioPreenchido() throws Exception {
        when(service.buscar(1L)).thenReturn(new CategoriaResponse(1L, "Hardware", "Equipamentos"));

        mvc.perform(get("/app/categorias/1/editar"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Editar categoria")))
            .andExpect(content().string(containsString("value=\"Hardware\"")));
    }

    @Test
    void categoriaComChamadosNaoEExcluida() throws Exception {
        doThrow(new RegraNegocioException("Categoria 1 possui chamados e não pode ser excluída"))
            .when(service).excluir(1L);

        mvc.perform(post("/app/categorias/1/excluir").with(csrf()))
            .andExpect(redirectedUrl("/app/categorias"))
            .andExpect(flash().attribute("erro", "Categoria 1 possui chamados e não pode ser excluída"));
    }
}
