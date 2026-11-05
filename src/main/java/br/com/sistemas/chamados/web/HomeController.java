package br.com.sistemas.chamados.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Telas: a raiz manda para a página inicial, que só é vista depois do login.
 * O nome e o perfil de quem está logado vêm do UsuarioLogadoAdvice.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String raiz() {
        return "redirect:/app";
    }

    @GetMapping("/app")
    public String inicio() {
        return "inicio";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
