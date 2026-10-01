<%@ include file="/WEB-INF/jsp/fragments/taglibs.jspf" %>
<!DOCTYPE html>
<html lang="pt-BR">
<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>
<body data-page="morador-reservas-agenda">
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
                        <h2>Agenda do dia</h2>
                        <p class="section-subtitle">Reservas aprovadas (ocupam o horario) e solicitacoes ainda pendentes, por area.</p>
                    </div>
                    <div class="button-row">
                        <a href="${ctx}/morador/reservas" class="btn btn-secondary">Ver em lista</a>
                    </div>
                </div>

                <form method="get" action="${ctx}/morador/reservas/agenda" class="filter-grid">
                    <label class="field">
                        <span>Data</span>
                        <input type="date" name="data" value="${filtroData}">
                    </label>
                    <label class="field">
                        <span>Area comum</span>
                        <select name="areaComumId">
                            <option value="">Todas</option>
                            <c:forEach items="${areasComuns}" var="area">
                                <option value="${area.id}" ${filtroAreaComumId eq area.id ? 'selected' : ''}>${area.nome}</option>
                            </c:forEach>
                        </select>
                    </label>
                    <div class="button-row align-end">
                        <button type="submit" class="btn btn-primary">Ver agenda</button>
                    </div>
                </form>
            </section>

            <c:choose>
                <c:when test="${empty agenda}">
                    <section class="card">
                        <div class="empty-state">
                            <h3>Nenhuma reserva sua nesta data</h3>
                            <p>Voce nao tem reserva aprovada nem solicitacao pendente para o dia ${filtroData}.</p>
                        </div>
                    </section>
                </c:when>
                <c:otherwise>
                    <c:forEach items="${agenda}" var="grupo">
                        <section class="card">
                            <div class="section-header">
                                <div>
                                    <h3>${grupo.areaComumNome}</h3>
                                </div>
                            </div>
                            <div class="table-wrap">
                                <table class="data-table">
                                    <thead>
                                    <tr>
                                        <th>Horario</th>
                                        <th>Situacao</th>
                                        <th></th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    <c:forEach items="${grupo.reservas}" var="reserva">
                                        <tr>
                                            <td>${reserva.horaInicio} - ${reserva.horaFim}</td>
                                            <td>
                                                <c:choose>
                                                    <c:when test="${reserva.status eq 'APROVADA'}">
                                                        <span class="status-pill success">Ocupado (aprovada)</span>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span class="status-pill warning">Pendente (solicitada)</span>
                                                    </c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td class="cell-actions">
                                                <a href="${ctx}/morador/reservas/${reserva.id}" class="btn btn-link">Detalhar</a>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                    </tbody>
                                </table>
                            </div>
                        </section>
                    </c:forEach>
                </c:otherwise>
            </c:choose>
        </main>
    </div>
</div>
<%@ include file="/WEB-INF/jsp/fragments/scripts.jspf" %>
</body>
</html>