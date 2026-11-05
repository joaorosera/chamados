package br.com.sistemas.chamados.web;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.com.sistemas.chamados.dto.ComentarioRequest;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.exception.ChamadoFechadoException;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.service.ChamadoService;
import br.com.sistemas.chamados.service.ComentarioService;
import br.com.sistemas.chamados.service.TecnicoService;

/**
 * Detalhe do chamado e as ações sobre ele. Cada ação volta para o detalhe com
 * uma mensagem; as regras (chamado fechado, mesmo status...) são do Service.
 * Quem pode o quê está no SecurityConfig: comentar é de todos, mudar status é
 * do técnico e atribuir técnico é do admin.
 */
@Controller
@RequestMapping("/app/chamados/{id}")
public class ChamadoDetalheWebController {

    private final ChamadoService chamadoService;
    private final ComentarioService comentarioService;
    private final TecnicoService tecnicoService;

    public ChamadoDetalheWebController(ChamadoService chamadoService, ComentarioService comentarioService,
                                       TecnicoService tecnicoService) {
        this.chamadoService = chamadoService;
        this.comentarioService = comentarioService;
        this.tecnicoService = tecnicoService;
    }

    @GetMapping
    public String detalhe(@PathVariable Long id, Model model) {
        model.addAttribute("chamado", chamadoService.buscar(id));
        model.addAttribute("historico", chamadoService.historico(id));
        model.addAttribute("tecnicos", tecnicoService.listar());
        model.addAttribute("listaStatus", StatusChamado.values());
        return "chamados/detalhe";
    }

    /** O autor do comentário é quem está logado; na tela não precisa digitar. */
    @PostMapping("/comentarios")
    public String comentar(@PathVariable Long id, @RequestParam String texto, Authentication usuario,
                           RedirectAttributes redirect) {
        if (texto.isBlank() || texto.length() > 1000) {
            redirect.addFlashAttribute("erro", "O comentário deve ter entre 1 e 1000 caracteres.");
            return voltar(id);
        }
        try {
            comentarioService.criar(id, new ComentarioRequest(texto.trim(), usuario.getName()));
            redirect.addFlashAttribute("sucesso", "Comentário registrado.");
        } catch (ChamadoFechadoException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return voltar(id);
    }

    @PostMapping("/status")
    public String alterarStatus(@PathVariable Long id, @RequestParam StatusChamado status,
                                RedirectAttributes redirect) {
        try {
            chamadoService.alterarStatus(id, status);
            redirect.addFlashAttribute("sucesso", "Status alterado.");
        } catch (RegraNegocioException | ChamadoFechadoException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return voltar(id);
    }

    @PostMapping("/atribuir")
    public String atribuir(@PathVariable Long id, @RequestParam Long tecnicoId, RedirectAttributes redirect) {
        try {
            chamadoService.atribuir(id, tecnicoId);
            redirect.addFlashAttribute("sucesso", "Técnico atribuído.");
        } catch (ChamadoFechadoException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return voltar(id);
    }

    private String voltar(Long id) {
        return "redirect:/app/chamados/" + id;
    }
}
