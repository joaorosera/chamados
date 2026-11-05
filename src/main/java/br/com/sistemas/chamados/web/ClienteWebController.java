package br.com.sistemas.chamados.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.com.sistemas.chamados.dto.ClienteResponse;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.service.ChamadoService;
import br.com.sistemas.chamados.service.ClienteService;
import jakarta.validation.Valid;

/** Telas de clientes: lista, cadastro, edição, detalhe (com os chamados) e exclusão. */
@Controller
@RequestMapping("/app/clientes")
public class ClienteWebController {

    private final ClienteService clienteService;
    private final ChamadoService chamadoService;

    public ClienteWebController(ClienteService clienteService, ChamadoService chamadoService) {
        this.clienteService = clienteService;
        this.chamadoService = chamadoService;
    }

    @GetMapping
    public String lista(Model model) {
        model.addAttribute("clientes", clienteService.listar());
        return "clientes/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("clienteForm", new ClienteForm());
        return "clientes/form";
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute ClienteForm clienteForm, BindingResult erros,
                        RedirectAttributes redirect) {
        if (erros.hasErrors()) {
            return "clientes/form";
        }
        try {
            ClienteResponse salvo = clienteService.criar(clienteForm.toRequest());
            redirect.addFlashAttribute("sucesso", "Cliente cadastrado.");
            return "redirect:/app/clientes/" + salvo.id();
        } catch (RegraNegocioException e) {
            // Única regra do cadastro: e-mail repetido. Mostra a mensagem no próprio campo.
            erros.rejectValue("email", "duplicado", e.getMessage());
            return "clientes/form";
        }
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, @RequestParam(defaultValue = "0") int pagina,
                          Model model) {
        model.addAttribute("cliente", clienteService.buscar(id));
        model.addAttribute("chamados", chamadoService.listarPorCliente(id,
            PageRequest.of(pagina, 10, Sort.by(Sort.Direction.DESC, "dataAbertura"))));
        return "clientes/detalhe";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("clienteForm", ClienteForm.de(clienteService.buscar(id)));
        return "clientes/form";
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute ClienteForm clienteForm,
                            BindingResult erros, RedirectAttributes redirect) {
        clienteForm.setId(id);
        if (erros.hasErrors()) {
            return "clientes/form";
        }
        try {
            clienteService.atualizar(id, clienteForm.toRequest());
            redirect.addFlashAttribute("sucesso", "Cliente atualizado.");
            return "redirect:/app/clientes/" + id;
        } catch (RegraNegocioException e) {
            erros.rejectValue("email", "duplicado", e.getMessage());
            return "clientes/form";
        }
    }

    /** Só o admin chega aqui (regra no SecurityConfig). */
    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            clienteService.excluir(id);
            redirect.addFlashAttribute("sucesso", "Cliente excluído.");
            return "redirect:/app/clientes";
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            return "redirect:/app/clientes/" + id;
        }
    }
}
