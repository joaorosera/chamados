package br.com.sistemas.chamados.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import br.com.sistemas.chamados.dto.DashboardResponse;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.repository.ClienteRepository;
import br.com.sistemas.chamados.service.DashboardService;

/**
 * Sobe a aplicação inteira com o profile "demo" e confere o que foi carregado.
 * Banco H2 próprio: os outros @SpringBootTest usam jdbc:h2:mem:chamadosdb na mesma JVM.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:demotest")
@ActiveProfiles("demo")
class DadosIniciaisTest {

    @Autowired
    private DashboardService dashboard;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private DadosIniciais dadosIniciais;

    @Test
    void carregaChamadosEmVariosStatus() {
        DashboardResponse resumo = dashboard.resumo();

        assertThat(resumo.total()).isEqualTo(5);
        assertThat(resumo.porStatus())
            .containsEntry(StatusChamado.ABERTO, 2L)
            .containsEntry(StatusChamado.EM_ATENDIMENTO, 1L)
            .containsEntry(StatusChamado.AGUARDANDO_CLIENTE, 1L)
            .containsEntry(StatusChamado.FECHADO, 1L);
        assertThat(resumo.pendentesSemTecnico()).isEqualTo(2);
    }

    @Test
    void naoDuplicaQuandoOBancoJaTemDados() throws Exception {
        long antes = clienteRepository.count();

        dadosIniciais.run();

        assertThat(clienteRepository.count()).isEqualTo(antes);
    }
}
