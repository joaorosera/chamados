package br.com.sistemas.chamados.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.com.sistemas.chamados.dto.ChamadoResponse;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.service.CategoriaService;
import br.com.sistemas.chamados.service.ChamadoService;
import br.com.sistemas.chamados.service.ClienteService;
import jakarta.validation.Valid;

/** Telas de chamados: lista com filtros e paginação, e abertura de chamado. */
@Controller
@RequestMapping("/app/chamados")
public class ChamadoWebController {

    private static final int ITENS_POR_PAGINA = 10;

    private final ChamadoService chamadoService;
    private final ClienteService clienteService;
    private final CategoriaService categoriaService;

    public ChamadoWebController(ChamadoService chamadoService, ClienteService clienteService,
                                CategoriaService categoriaService) {
        this.chamadoService = chamadoService;
        this.clienteService = clienteService;
        this.categoriaService = categoriaService;
    }

    /** Opções dos selects (filtros e formulário). */
    @ModelAttribute
    public void opcoes(Model model) {
        model.addAttribute("listaStatus", StatusChamado.values());
        model.addAttribute("prioridades", Prioridade.values());
    }

    /** Os filtros são opcionais, como no GET /chamados da API. Mais recentes primeiro. */
    @GetMapping
    public String lista(@RequestParam(required = false) StatusChamado status,
                        @RequestParam(required = false) Prioridade prioridade,
                        @RequestParam(required = false) Long clienteId,
                        @RequestParam(defaultValue = "0") int pagina,
                        Model model) {
        PageRequest pageable = PageRequest.of(pagina, ITENS_POR_PAGINA,
            Sort.by(Sort.Direction.DESC, "dataAbertura"));
        model.addAttribute("chamados", chamadoService.listar(status, prioridade, clienteId, pageable));
        model.addAttribute("clientes", clienteService.listar());
        // Devolve os filtros para a tela manter os selects marcados e montar a paginação.
        model.addAttribute("status", status);
        model.addAttribute("prioridade", prioridade);
        model.addAttribute("clienteId", clienteId);
        return "chamados/lista";
    }

    /** ?clienteId=1 já deixa o cliente escolhido (link "Abrir chamado" do detalhe do cliente). */
    @GetMapping("/novo")
    public String novo(@RequestParam(required = false) Long clienteId, Model model) {
        ChamadoForm form = new ChamadoForm();
        form.setClienteId(clienteId);
        model.addAttribute("chamadoForm", form);
        return formulario(model);
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute ChamadoForm chamadoForm, BindingResult erros,
                        Model model, RedirectAttributes redirect) {
        if (erros.hasErrors()) {
            return formulario(model);
        }
        ChamadoResponse criado = chamadoService.criar(chamadoForm.toRequest());
        redirect.addFlashAttribute("sucesso", "Chamado #" + criado.id() + " aberto.");
        return "redirect:/app/chamados";
    }

    private String formulario(Model model) {
        model.addAttribute("clientes", clienteService.listar());
        model.addAttribute("categorias", categoriaService.listar());
        return "chamados/form";
    }
}
