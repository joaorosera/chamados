package br.com.sistemas.chamados.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import br.com.sistemas.chamados.service.DashboardService;

/** Dashboard do admin: o mesmo resumo do GET /dashboard da API, em tela. */
@Controller
public class DashboardWebController {

    private final DashboardService service;

    public DashboardWebController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/app/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("resumo", service.resumo());
        return "dashboard";
    }
}
