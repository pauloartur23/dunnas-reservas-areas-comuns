<%@ include file="/WEB-INF/jsp/fragments/taglibs.jspf" %>
<!DOCTYPE html>
<html lang="pt-BR">
<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>
<body data-page="admin-areas-comuns">
<div class="app-shell">
    <%@ include file="/WEB-INF/jsp/fragments/sidebar.jspf" %>
    <div class="app-main">
        <%@ include file="/WEB-INF/jsp/fragments/topbar.jspf" %>
        <main class="page-content">
            <%@ include file="/WEB-INF/jsp/fragments/alerts.jspf" %>

            <section class="card">
                <div class="section-header">
                    <div>
                        <p class="eyebrow">Reservas</p>
                        <h2>${empty areaComumEdicao ? 'Cadastrar area comum' : 'Editar area comum'}</h2>
                    </div>
                </div>

                <form method="post"
                      action="${empty areaComumEdicao ? ctx.concat('/admin/areas-comuns') : ctx.concat('/admin/areas-comuns/').concat(areaComumEdicao.id)}"
                      class="stack-form">
                    <%@ include file="/WEB-INF/jsp/fragments/csrf.jspf" %>
                    <c:if test="${not empty areaComumEdicao}">
                        <input type="hidden" name="_method" value="patch">
                    </c:if>
                    <label class="field">
                        <span>Nome</span>
                        <input type="text" name="nome" maxlength="255" required value="${cadastrarAreaComumForm.nome}">
                    </label>
                    <label class="field">
                        <span>Descricao</span>
                        <textarea name="descricao" rows="2" maxlength="255">${cadastrarAreaComumForm.descricao}</textarea>
                    </label>
                    <label class="field">
                        <span>Horario de abertura (opcional)</span>
                        <input type="time" name="horarioAbertura" value="${cadastrarAreaComumForm.horarioAbertura}">
                    </label>
                    <label class="field">
                        <span>Horario de fechamento (opcional)</span>
                        <input type="time" name="horarioFechamento" value="${cadastrarAreaComumForm.horarioFechamento}">
                    </label>
                    <div class="field">
                        <span>Dias de funcionamento (deixe tudo desmarcado para funcionar todos os dias)</span>
                        <div class="checkbox-row">
                            <label><input type="checkbox" name="diasFuncionamento" value="MONDAY" ${cadastrarAreaComumForm.diasFuncionamento.contains('MONDAY') ? 'checked' : ''}> Seg</label>
                            <label><input type="checkbox" name="diasFuncionamento" value="TUESDAY" ${cadastrarAreaComumForm.diasFuncionamento.contains('TUESDAY') ? 'checked' : ''}> Ter</label>
                            <label><input type="checkbox" name="diasFuncionamento" value="WEDNESDAY" ${cadastrarAreaComumForm.diasFuncionamento.contains('WEDNESDAY') ? 'checked' : ''}> Qua</label>
                            <label><input type="checkbox" name="diasFuncionamento" value="THURSDAY" ${cadastrarAreaComumForm.diasFuncionamento.contains('THURSDAY') ? 'checked' : ''}> Qui</label>
                            <label><input type="checkbox" name="diasFuncionamento" value="FRIDAY" ${cadastrarAreaComumForm.diasFuncionamento.contains('FRIDAY') ? 'checked' : ''}> Sex</label>
                            <label><input type="checkbox" name="diasFuncionamento" value="SATURDAY" ${cadastrarAreaComumForm.diasFuncionamento.contains('SATURDAY') ? 'checked' : ''}> Sab</label>
                            <label><input type="checkbox" name="diasFuncionamento" value="SUNDAY" ${cadastrarAreaComumForm.diasFuncionamento.contains('SUNDAY') ? 'checked' : ''}> Dom</label>
                        </div>
                    </div>
                    <div class="button-row">
                        <button type="submit" class="btn btn-primary">
                            ${empty areaComumEdicao ? 'Cadastrar area' : 'Salvar alteracoes'}
                        </button>
                        <c:if test="${not empty areaComumEdicao}">
                            <a href="${ctx}/admin/areas-comuns" class="btn btn-secondary">Cancelar edicao</a>
                        </c:if>
                    </div>
                </form>
            </section>

            <section class="card">
                <div class="section-header">
                    <div>
                        <p class="eyebrow">Cadastro</p>
                        <h2>Areas comuns</h2>
                    </div>
                </div>

                <c:choose>
                    <c:when test="${empty areasComuns}">
                        <div class="empty-state">
                            <h3>Nenhuma area comum cadastrada</h3>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="table-wrap">
                            <table class="data-table">
                                <thead>
                                <tr>
                                    <th>Nome</th>
                                    <th>Descricao</th>
                                    <th>Horario</th>
                                    <th>Dias</th>
                                    <th>Situacao</th>
                                    <th></th>
                                </tr>
                                </thead>
                                <tbody>
                                <c:forEach items="${areasComuns}" var="area">
                                    <tr>
                                        <td>${area.nome}</td>
                                        <td>${area.descricao}</td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${area.temHorarioFuncionamento}">${area.horarioAbertura} - ${area.horarioFechamento}</c:when>
                                                <c:otherwise>Sem restricao</c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>${area.diasFuncionamentoLabel}</td>
                                        <td><span class="status-pill">${area.ativa ? 'Ativa' : 'Inativa'}</span></td>
                                        <td class="cell-actions">
                                            <a href="${ctx}/admin/areas-comuns?areaComumId=${area.id}" class="btn btn-link">Editar</a>
                                            <form method="post" action="${ctx}/admin/areas-comuns/${area.id}/disponibilidade" class="inline-panel">
                                                <%@ include file="/WEB-INF/jsp/fragments/csrf.jspf" %>
                                                <input type="hidden" name="_method" value="patch">
                                                <input type="hidden" name="ativa" value="${area.ativa ? 'false' : 'true'}">
                                                <button type="submit" class="btn btn-secondary">
                                                    ${area.ativa ? 'Desativar' : 'Ativar'}
                                                </button>
                                            </form>
                                        </td>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>
            </section>
        </main>
    </div>
</div>
<%@ include file="/WEB-INF/jsp/fragments/scripts.jspf" %>
</body>
</html>