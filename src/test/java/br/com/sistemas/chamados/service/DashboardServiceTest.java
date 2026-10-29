package br.com.sistemas.chamados.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import br.com.sistemas.chamados.dto.DashboardResponse;
import br.com.sistemas.chamados.entity.Categoria;
import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.entity.Cliente;
import br.com.sistemas.chamados.entity.Perfil;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.entity.Tecnico;
import br.com.sistemas.chamados.repository.CategoriaRepository;
import br.com.sistemas.chamados.repository.ChamadoRepository;
import br.com.sistemas.chamados.repository.ClienteRepository;
import br.com.sistemas.chamados.repository.TecnicoRepository;

/** Roda as consultas de contagem de verdade no H2, junto com o Service que monta o resumo. */
@DataJpaTest
@Import(DashboardService.class)
class DashboardServiceTest {

    @Autowired
    private DashboardService service;
    @Autowired
    private ChamadoRepository repository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private CategoriaRepository categoriaRepository;
    @Autowired
    private TecnicoRepository tecnicoRepository;

    @Test
    void semChamadosTudoZerado() {
        DashboardResponse resumo = service.resumo();

        assertThat(resumo.total()).isZero();
        assertThat(resumo.porStatus()).hasSize(StatusChamado.values().length)
            .allSatisfy((status, total) -> assertThat(total).isZero());
        assertThat(resumo.pendentesPorPrioridade()).hasSize(Prioridade.values().length)
            .allSatisfy((prioridade, total) -> assertThat(total).isZero());
    }

    @Test
    void contaPorStatusEPendentesPorPrioridade() {
        Cliente ana = clienteRepository.save(new Cliente("Ana Souza", "ana@exemplo.com", null));
        Categoria hardware = categoriaRepository.save(new Categoria("Hardware", null));
        Tecnico carlos = tecnicoRepository.save(new Tecnico("Carlos", "carlos@empresa.com", Perfil.TECNICO));

        repository.save(new Chamado("C1", "d", Prioridade.ALTA, ana, hardware));
        Chamado emAtendimento = new Chamado("C2", "d", Prioridade.ALTA, ana, hardware);
        emAtendimento.setStatus(StatusChamado.EM_ATENDIMENTO);
        emAtendimento.setTecnico(carlos);
        repository.save(emAtendimento);
        Chamado fechado = new Chamado("C3", "d", Prioridade.CRITICA, ana, hardware);
        fechado.setStatus(StatusChamado.FECHADO);
        repository.save(fechado);

        DashboardResponse resumo = service.resumo();

        assertThat(resumo.total()).isEqualTo(3);
        assertThat(resumo.pendentes()).isEqualTo(2);
        assertThat(resumo.pendentesSemTecnico()).isEqualTo(1);
        assertThat(resumo.porStatus())
            .containsEntry(StatusChamado.ABERTO, 1L)
            .containsEntry(StatusChamado.EM_ATENDIMENTO, 1L)
            .containsEntry(StatusChamado.AGUARDANDO_CLIENTE, 0L)
            .containsEntry(StatusChamado.FECHADO, 1L);
        // O CRITICA está fechado, então não conta como pendente.
        assertThat(resumo.pendentesPorPrioridade())
            .containsEntry(Prioridade.ALTA, 2L)
            .containsEntry(Prioridade.CRITICA, 0L);
    }
}
