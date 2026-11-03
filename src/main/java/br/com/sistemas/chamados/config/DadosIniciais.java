package br.com.sistemas.chamados.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import br.com.sistemas.chamados.dto.CategoriaRequest;
import br.com.sistemas.chamados.dto.ChamadoRequest;
import br.com.sistemas.chamados.dto.ClienteRequest;
import br.com.sistemas.chamados.dto.ComentarioRequest;
import br.com.sistemas.chamados.dto.TecnicoRequest;
import br.com.sistemas.chamados.entity.Perfil;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.service.CategoriaService;
import br.com.sistemas.chamados.service.ChamadoService;
import br.com.sistemas.chamados.service.ClienteService;
import br.com.sistemas.chamados.service.ComentarioService;
import br.com.sistemas.chamados.service.TecnicoService;

/**
 * Dados de demonstração, carregados só com o profile "demo"
 * (./mvnw spring-boot:run -Dspring-boot.run.profiles=demo).
 *
 * Sem o profile o banco começa vazio, como os testes do guia esperam (ex.: o
 * DELETE /clientes/1 retornaria 409 se o cliente 1 já tivesse chamados). Tudo
 * passa pelos Services, então as regras valem também aqui: o histórico de
 * status, a data de fechamento e a atribuição saem como em uma chamada à API.
 * Os e-mails e nomes não coincidem com os dos exemplos do README.
 */
@Component
@Profile("demo")
public class DadosIniciais implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DadosIniciais.class);

    private final ClienteService clientes;
    private final CategoriaService categorias;
    private final TecnicoService tecnicos;
    private final ChamadoService chamados;
    private final ComentarioService comentarios;

    public DadosIniciais(ClienteService clientes, CategoriaService categorias,
                         TecnicoService tecnicos, ChamadoService chamados,
                         ComentarioService comentarios) {
        this.clientes = clientes;
        this.categorias = categorias;
        this.tecnicos = tecnicos;
        this.chamados = chamados;
        this.comentarios = comentarios;
    }

    @Override
    public void run(String... args) {
        // Com PostgreSQL os dados persistem: só popula um banco vazio.
        if (!clientes.listar().isEmpty()) {
            log.info("Dados de demonstração não carregados: o banco já tem clientes");
            return;
        }

        Long mariana = clientes.criar(new ClienteRequest(
            "Mariana Costa", "mariana.costa@demo.com", "47991110001")).id();
        Long pedro = clientes.criar(new ClienteRequest(
            "Pedro Almeida", "pedro.almeida@demo.com", "47991110002")).id();
        Long oficina = clientes.criar(new ClienteRequest(
            "Oficina Boa Vista", "contato@boavista-demo.com", "4734330003")).id();

        Long equipamentos = categorias.criar(new CategoriaRequest(
            "Equipamentos", "Computadores, impressoras e periféricos")).id();
        Long rede = categorias.criar(new CategoriaRequest(
            "Rede e Internet", "Wi-Fi, cabeamento e lentidão")).id();
        Long sistemas = categorias.criar(new CategoriaRequest(
            "Sistemas e Acessos", "Senhas, permissões e erros em sistemas")).id();

        Long bruna = tecnicos.criar(new TecnicoRequest(
            "Bruna Martins", "bruna.martins@demo.com", Perfil.ADMIN)).id();
        Long rafael = tecnicos.criar(new TecnicoRequest(
            "Rafael Souza", "rafael.souza@demo.com", Perfil.TECNICO)).id();

        Long notebook = chamados.criar(new ChamadoRequest("Notebook não liga",
            "Não acende nem com o carregador conectado", Prioridade.ALTA, mariana, equipamentos)).id();
        chamados.atribuir(notebook, rafael);
        chamados.alterarStatus(notebook, StatusChamado.EM_ATENDIMENTO);
        comentarios.criar(notebook, new ComentarioRequest(
            "Fonte testada e funcionando; verificando a placa", "Rafael Souza"));

        chamados.criar(new ChamadoRequest("Sem acesso ao Wi-Fi",
            "A rede da sala de reuniões não aparece", Prioridade.MEDIA, pedro, rede));

        Long senha = chamados.criar(new ChamadoRequest("Senha do ERP bloqueada",
            "Usuário do caixa bloqueado após três tentativas", Prioridade.CRITICA, oficina, sistemas)).id();
        chamados.atribuir(senha, bruna);
        chamados.alterarStatus(senha, StatusChamado.EM_ATENDIMENTO);
        comentarios.criar(senha, new ComentarioRequest(
            "Senha provisória enviada; aguardando o cliente confirmar o acesso", "Bruna Martins"));
        chamados.alterarStatus(senha, StatusChamado.AGUARDANDO_CLIENTE);

        Long toner = chamados.criar(new ChamadoRequest("Impressora sem toner",
            "Impressão sai falhada", Prioridade.BAIXA, oficina, equipamentos)).id();
        chamados.atribuir(toner, rafael);
        chamados.alterarStatus(toner, StatusChamado.EM_ATENDIMENTO);
        comentarios.criar(toner, new ComentarioRequest("Toner substituído", "Rafael Souza"));
        chamados.alterarStatus(toner, StatusChamado.FECHADO);

        chamados.criar(new ChamadoRequest("Lentidão na rede",
            "Sistema demora para abrir no fim da tarde", Prioridade.ALTA, mariana, rede));

        log.info("Dados de demonstração carregados: 3 clientes, 3 categorias, 2 técnicos e 5 chamados");
    }
}
