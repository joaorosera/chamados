package br.com.sistemas.chamados.web;

import br.com.sistemas.chamados.dto.TecnicoRequest;
import br.com.sistemas.chamados.dto.TecnicoResponse;
import br.com.sistemas.chamados.entity.Perfil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Formulário de técnico (mesmas validações do TecnicoRequest). */
public class TecnicoForm {

    private Long id;

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
    private String nome;

    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    @Size(max = 120, message = "E-mail deve ter no máximo 120 caracteres")
    private String email;

    @NotNull(message = "Perfil é obrigatório")
    private Perfil perfil;

    public static TecnicoForm de(TecnicoResponse tecnico) {
        TecnicoForm form = new TecnicoForm();
        form.setId(tecnico.id());
        form.setNome(tecnico.nome());
        form.setEmail(tecnico.email());
        form.setPerfil(tecnico.perfil());
        return form;
    }

    public TecnicoRequest toRequest() {
        return new TecnicoRequest(nome, email, perfil);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Perfil getPerfil() { return perfil; }
    public void setPerfil(Perfil perfil) { this.perfil = perfil; }
}
