package br.com.sistemas.chamados.web;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;

import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;

/**
 * Erros das telas viram páginas HTML. O GlobalExceptionHandler da API responde
 * JSON e vale para todos os controllers; este vem antes (maior precedência), mas
 * só para os controllers do pacote web.
 */
@ControllerAdvice(basePackages = "br.com.sistemas.chamados.web")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WebExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ModelAndView naoEncontrado(RecursoNaoEncontradoException ex) {
        return pagina404(ex.getMessage());
    }

    /** Ex.: /app/clientes/abc. Para a tela, é só um endereço que não existe. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ModelAndView enderecoInvalido(MethodArgumentTypeMismatchException ex) {
        return pagina404(null);
    }

    private ModelAndView pagina404(String mensagem) {
        ModelAndView mv = new ModelAndView("error/404", HttpStatus.NOT_FOUND);
        mv.addObject("mensagem", mensagem);
        return mv;
    }
}
