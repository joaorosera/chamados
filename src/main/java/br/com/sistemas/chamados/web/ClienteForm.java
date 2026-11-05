package br.com.sistemas.chamados.web;

import br.com.sistemas.chamados.dto.ClienteRequest;
import br.com.sistemas.chamados.dto.ClienteResponse;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dados do formulário de cliente. É uma classe com getters e setters (e não o
 * record ClienteRequest) porque o th:field do Thymeleaf lê e preenche os campos
 * por getter/setter. As validações são as mesmas do ClienteRequest.
 */
public class ClienteForm {

    /** Null no cadastro; preenchido na edição. */
    private Long id;

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
    private String nome;

    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    private String email;

    @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
    private String telefone;

    public static ClienteForm de(ClienteResponse cliente) {
        ClienteForm form = new ClienteForm();
        form.setId(cliente.id());
        form.setNome(cliente.nome());
        form.setEmail(cliente.email());
        form.setTelefone(cliente.telefone());
        return form;
    }

    public ClienteRequest toRequest() {
        return new ClienteRequest(nome, email, telefone);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
}
