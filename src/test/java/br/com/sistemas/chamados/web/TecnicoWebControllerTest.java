package br.com.sistemas.chamados.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
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
import br.com.sistemas.chamados.dto.TecnicoRequest;
import br.com.sistemas.chamados.dto.TecnicoResponse;
import br.com.sistemas.chamados.entity.Perfil;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.service.TecnicoService;

@WebMvcTest(TecnicoWebController.class)
@Import(SecurityConfig.class)
@WithMockUser(username = "admin", roles = "ADMIN")
class TecnicoWebControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private TecnicoService service;

    @Test
    @WithMockUser(username = "usuario", roles = "USUARIO")
    void usuarioVeAListaSemOsBotoesDeAdmin() throws Exception {
        when(service.listar()).thenReturn(List.of(
            new TecnicoResponse(1L, "Carlos Lima", "carlos@empresa.com", Perfil.TECNICO)));

        mvc.perform(get("/app/tecnicos"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("carlos@empresa.com")))
            .andExpect(content().string(not(containsString("Novo técnico"))));
    }

    @Test
    @WithMockUser(username = "tecnico", roles = "TECNICO")
    void tecnicoNaoCadastraTecnico() throws Exception {
        mvc.perform(post("/app/tecnicos").with(csrf())
                .param("nome", "X").param("email", "x@empresa.com").param("perfil", "ADMIN"))
            .andExpect(status().isForbidden());

        verify(service, never()).criar(any());
    }

    @Test
    void adminCadastraComPerfil() throws Exception {
        mvc.perform(post("/app/tecnicos").with(csrf())
                .param("nome", "Carlos Lima").param("email", "carlos@empresa.com").param("perfil", "TECNICO"))
            .andExpect(redirectedUrl("/app/tecnicos"))
            .andExpect(flash().attribute("sucesso", "Técnico cadastrado."));

        verify(service).criar(new TecnicoRequest("Carlos Lima", "carlos@empresa.com", Perfil.TECNICO));
    }

    @Test
    void semPerfilVoltaComErro() throws Exception {
        mvc.perform(post("/app/tecnicos").with(csrf())
                .param("nome", "Carlos Lima").param("email", "carlos@empresa.com").param("perfil", ""))
            .andExpect(view().name("tecnicos/form"))
            .andExpect(content().string(containsString("Perfil é obrigatório")));
    }

    @Test
    void emailRepetidoApareceNoCampo() throws Exception {
        when(service.criar(any(TecnicoRequest.class)))
            .thenThrow(new RegraNegocioException("Já existe técnico com o e-mail carlos@empresa.com"));

        mvc.perform(post("/app/tecnicos").with(csrf())
                .param("nome", "Carlos").param("email", "carlos@empresa.com").param("perfil", "TECNICO"))
            .andExpect(view().name("tecnicos/form"))
            .andExpect(content().string(containsString("Já existe técnico com o e-mail carlos@empresa.com")));
    }

    @Test
    void edicaoMostraOPerfilSelecionado() throws Exception {
        when(service.buscar(1L)).thenReturn(new TecnicoResponse(1L, "Bruna", "bruna@empresa.com", Perfil.ADMIN));

        mvc.perform(get("/app/tecnicos/1/editar"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("value=\"ADMIN\" selected=\"selected\"")));
    }
}
