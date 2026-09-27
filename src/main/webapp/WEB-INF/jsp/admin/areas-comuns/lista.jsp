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
                        <h2>Cadastrar area comum</h2>
                    </div>
                </div>

                <form method="post" action="${ctx}/admin/areas-comuns" class="stack-form">
                    <%@ include file="/WEB-INF/jsp/fragments/csrf.jspf" %>
                    <label class="field">
                        <span>Nome</span>
                        <input type="text" name="nome" maxlength="255" required>
                    </label>
                    <label class="field">
                        <span>Descricao</span>
                        <textarea name="descricao" rows="2" maxlength="255"></textarea>
                    </label>
                    <button type="submit" class="btn btn-primary">Cadastrar area</button>
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
                                    <th>Situacao</th>
                                    <th></th>
                                </tr>
                                </thead>
                                <tbody>
                                <c:forEach items="${areasComuns}" var="area">
                                    <tr>
                                        <td>${area.nome}</td>
                                        <td>${area.descricao}</td>
                                        <td><span class="status-pill">${area.ativa ? 'Ativa' : 'Inativa'}</span></td>
                                        <td class="cell-actions">
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