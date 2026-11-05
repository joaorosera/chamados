package br.com.sistemas.chamados.web;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Coloca em todas as telas quem está logado e o que pode fazer, para os templates
 * mostrarem ou esconderem menus e botões com th:if. A hierarquia de papéis entra
 * aqui também: o admin é técnico e o técnico é usuário.
 *
 * Esconder o botão é só conforto: quem garante a permissão é o SecurityConfig.
 */
@ControllerAdvice(basePackages = "br.com.sistemas.chamados.web")
public class UsuarioLogadoAdvice {

    private final RoleHierarchy hierarquia;

    public UsuarioLogadoAdvice(RoleHierarchy hierarquia) {
        this.hierarquia = hierarquia;
    }

    @ModelAttribute
    public void usuarioLogado(Authentication usuario, Model model) {
        if (usuario == null) {
            return; // tela de login
        }
        Set<String> papeis = hierarquia.getReachableGrantedAuthorities(usuario.getAuthorities())
            .stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());

        boolean ehAdmin = papeis.contains("ROLE_ADMIN");
        boolean ehTecnico = papeis.contains("ROLE_TECNICO");

        model.addAttribute("usuarioLogado", usuario.getName());
        model.addAttribute("ehAdmin", ehAdmin);
        model.addAttribute("ehTecnico", ehTecnico);
        model.addAttribute("papel", ehAdmin ? "Administrador" : ehTecnico ? "Técnico" : "Usuário");
    }
}
