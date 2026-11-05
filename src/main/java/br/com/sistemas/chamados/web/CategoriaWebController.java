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

import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.service.CategoriaService;
import jakarta.validation.Valid;

/** Telas de categorias. Todos veem a lista; cadastrar, editar e excluir é só do admin. */
@Controller
@RequestMapping("/app/categorias")
public class CategoriaWebController {

    private final CategoriaService service;

    public CategoriaWebController(CategoriaService service) {
        this.service = service;
    }

    @GetMapping
    public String lista(Model model) {
        model.addAttribute("categorias", service.listar());
        return "categorias/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("categoriaForm", new CategoriaForm());
        return "categorias/form";
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute CategoriaForm categoriaForm, BindingResult erros,
                        RedirectAttributes redirect) {
        if (erros.hasErrors()) {
            return "categorias/form";
        }
        try {
            service.criar(categoriaForm.toRequest());
            redirect.addFlashAttribute("sucesso", "Categoria cadastrada.");
            return "redirect:/app/categorias";
        } catch (RegraNegocioException e) {
            erros.rejectValue("nome", "duplicado", e.getMessage());
            return "categorias/form";
        }
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("categoriaForm", CategoriaForm.de(service.buscar(id)));
        return "categorias/form";
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute CategoriaForm categoriaForm,
                            BindingResult erros, RedirectAttributes redirect) {
        categoriaForm.setId(id);
        if (erros.hasErrors()) {
            return "categorias/form";
        }
        try {
            service.atualizar(id, categoriaForm.toRequest());
            redirect.addFlashAttribute("sucesso", "Categoria atualizada.");
            return "redirect:/app/categorias";
        } catch (RegraNegocioException e) {
            erros.rejectValue("nome", "duplicado", e.getMessage());
            return "categorias/form";
        }
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            service.excluir(id);
            redirect.addFlashAttribute("sucesso", "Categoria excluída.");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/app/categorias";
    }
}
