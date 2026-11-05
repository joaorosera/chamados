package br.com.sistemas.chamados.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.EnumMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.sistemas.chamados.config.SecurityConfig;
import br.com.sistemas.chamados.dto.DashboardResponse;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.service.DashboardService;

/** Inclui o HomeController para conferir também o link do menu e da página inicial. */
@WebMvcTest(controllers = {DashboardWebController.class, HomeController.class})
@Import(SecurityConfig.class)
@WithMockUser(username = "admin", roles = "ADMIN")
class DashboardWebControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private DashboardService service;

    private DashboardResponse resumo() {
        Map<StatusChamado, Long> porStatus = new EnumMap<>(StatusChamado.class);
        porStatus.put(StatusChamado.ABERTO, 2L);
        porStatus.put(StatusChamado.EM_ATENDIMENTO, 1L);
        porStatus.put(StatusChamado.AGUARDANDO_CLIENTE, 1L);
        porStatus.put(StatusChamado.FECHADO, 1L);
        Map<Prioridade, Long> porPrioridade = new EnumMap<>(Prioridade.class);
        porPrioridade.put(Prioridade.BAIXA, 0L);
        porPrioridade.put(Prioridade.MEDIA, 1L);
        porPrioridade.put(Prioridade.ALTA, 2L);
        porPrioridade.put(Prioridade.CRITICA, 1L);
        return new DashboardResponse(5, 4, 2, porStatus, porPrioridade);
    }

    @Test
    void adminVeOsNumerosELinksParaALista() throws Exception {
        when(service.resumo()).thenReturn(resumo());

        mvc.perform(get("/app/dashboard"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Pendentes sem técnico")))
            .andExpect(content().string(containsString("href=\"/app/chamados?status=ABERTO\">2</a>")))
            // ALTA: 2 de 4 pendentes = 50%
            .andExpect(content().string(containsString("width: 50%")));
    }

    @Test
    void semChamadosNaoDividePorZero() throws Exception {
        Map<StatusChamado, Long> zeradoStatus = new EnumMap<>(StatusChamado.class);
        Map<Prioridade, Long> zeradoPrioridade = new EnumMap<>(Prioridade.class);
        for (Prioridade p : Prioridade.values()) {
            zeradoPrioridade.put(p, 0L);
        }
        when(service.resumo()).thenReturn(new DashboardResponse(0, 0, 0, zeradoStatus, zeradoPrioridade));

        mvc.perform(get("/app/dashboard"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("width: 0%")));
    }

    @Test
    @WithMockUser(username = "tecnico", roles = "TECNICO")
    void tecnicoNaoAcessaODashboard() throws Exception {
        mvc.perform(get("/app/dashboard")).andExpect(status().isForbidden());
    }

    @Test
    void adminVeODashboardNoMenu() throws Exception {
        mvc.perform(get("/app"))
            .andExpect(content().string(containsString("href=\"/app/dashboard\"")));
    }

    @Test
    @WithMockUser(username = "tecnico", roles = "TECNICO")
    void tecnicoNaoVeODashboardNoMenu() throws Exception {
        mvc.perform(get("/app"))
            .andExpect(content().string(not(containsString("/app/dashboard"))));
    }
}
