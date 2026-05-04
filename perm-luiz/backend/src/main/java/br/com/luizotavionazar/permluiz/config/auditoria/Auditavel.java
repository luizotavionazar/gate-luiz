package br.com.luizotavionazar.permluiz.config.auditoria;

import br.com.luizotavionazar.permluiz.domain.auditoria.enums.AcaoAuditoria;
import br.com.luizotavionazar.permluiz.domain.auditoria.enums.CategoriaAuditoria;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditavel {

    AcaoAuditoria acao();

    CategoriaAuditoria categoria() default CategoriaAuditoria.ATIVIDADE;
}
