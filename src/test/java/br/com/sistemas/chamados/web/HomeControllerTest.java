package br.com.sistemas.chamados.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.logout;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import br.com.sistemas.chamados.config.SecurityConfig;

/** Login por formulário das telas, separado do HTTP Basic da API. */
@WebMvcTest(HomeController.class)
@Import(SecurityConfig.class)
class HomeControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void telaSemLoginVaiParaOLogin() throws Exception {
        mvc.perform(get("/app"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
    }

    @Test
    void paginaDeLoginAbreSemLogin() throws Exception {
        mvc.perform(get("/login"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("name=\"username\"")));
    }

    @Test
    void loginComSenhaCertaEntraNoSistema() throws Exception {
        mvc.perform(formLogin().user("tecnico").password("tecnico123"))
            .andExpect(authenticated().withUsername("tecnico"))
            .andExpect(redirectedUrl("/app"));
    }

    @Test
    void loginComSenhaErradaVoltaComErro() throws Exception {
        mvc.perform(formLogin().user("tecnico").password("errada"))
            .andExpect(unauthenticated())
            .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void inicioMostraUsuarioEPerfil() throws Exception {
        mvc.perform(get("/app"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Bem-vindo, <span>admin</span>")))
            .andExpect(content().string(containsString("Administrador")));
    }

    @Test
    void sairVoltaParaOLogin() throws Exception {
        mvc.perform(logout())
            .andExpect(redirectedUrl("/login?saiu"));
    }

    @Test
    void apiContinuaRespondendo401EmJsonSemRedirecionar() throws Exception {
        mvc.perform(get("/clientes"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.erro").value("Autenticação necessária"));
    }
}
