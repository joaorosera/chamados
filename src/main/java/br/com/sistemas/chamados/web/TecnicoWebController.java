package br.com.sistemas.chamados.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.com.sistemas.chamados.entity.Perfil;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.service.TecnicoService;
import jakarta.validation.Valid;

/** Telas de técnicos. Todos veem a lista; cadastrar, editar e excluir é só do admin. */
@Controller
@RequestMapping("/app/tecnicos")
public class TecnicoWebController {

    private final TecnicoService service;

    public TecnicoWebController(TecnicoService service) {
        this.service = service;
    }

    /** Opções do select de perfil, em todas as telas deste controller. */
    @ModelAttribute("perfis")
    public Perfil[] perfis() {
        return Perfil.values();
    }

    @GetMapping
    public String lista(Model model) {
        model.addAttribute("tecnicos", service.listar());
        return "tecnicos/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("tecnicoForm", new TecnicoForm());
        return "tecnicos/form";
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute TecnicoForm tecnicoForm, BindingResult erros,
                        RedirectAttributes redirect) {
        if (erros.hasErrors()) {
            return "tecnicos/form";
        }
        try {
            service.criar(tecnicoForm.toRequest());
            redirect.addFlashAttribute("sucesso", "Técnico cadastrado.");
            return "redirect:/app/tecnicos";
        } catch (RegraNegocioException e) {
            erros.rejectValue("email", "duplicado", e.getMessage());
            return "tecnicos/form";
        }
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("tecnicoForm", TecnicoForm.de(service.buscar(id)));
        return "tecnicos/form";
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute TecnicoForm tecnicoForm,
                            BindingResult erros, RedirectAttributes redirect) {
        tecnicoForm.setId(id);
        if (erros.hasErrors()) {
            return "tecnicos/form";
        }
        try {
            service.atualizar(id, tecnicoForm.toRequest());
            redirect.addFlashAttribute("sucesso", "Técnico atualizado.");
            return "redirect:/app/tecnicos";
        } catch (RegraNegocioException e) {
            erros.rejectValue("email", "duplicado", e.getMessage());
            return "tecnicos/form";
        }
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            service.excluir(id);
            redirect.addFlashAttribute("sucesso", "Técnico excluído.");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/app/tecnicos";
    }
}
