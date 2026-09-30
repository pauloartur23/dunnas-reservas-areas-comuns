<%@ include file="/WEB-INF/jsp/fragments/taglibs.jspf" %>
<!DOCTYPE html>
<html lang="pt-BR">
<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>
<body data-page="admin-reservas">
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
                        <h2>Todas as reservas</h2>
                        <p class="section-subtitle">Aprove, negue ou cancele solicitacoes.</p>
                    </div>
                </div>

                <form method="get" action="${ctx}/admin/reservas" class="filter-grid">
                    <label class="field">
                        <span>Status</span>
                        <select name="status">
                            <option value="">Todos</option>
                            <option value="SOLICITADA" ${filtroStatus eq 'SOLICITADA' ? 'selected' : ''}>Solicitada</option>
                            <option value="APROVADA" ${filtroStatus eq 'APROVADA' ? 'selected' : ''}>Aprovada</option>
                            <option value="NEGADA" ${filtroStatus eq 'NEGADA' ? 'selected' : ''}>Negada</option>
                            <option value="CANCELADA" ${filtroStatus eq 'CANCELADA' ? 'selected' : ''}>Cancelada</option>
                        </select>
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
                    <label class="field">
                        <span>Data</span>
                        <input type="date" name="data" value="${filtroData}">
                    </label>
                    <div class="button-row align-end">
                        <button type="submit" class="btn btn-primary">Filtrar</button>
                        <a href="${ctx}/admin/reservas" class="btn btn-secondary">Limpar</a>
                    </div>
                </form>
            </section>

            <section class="card">
                <c:choose>
                    <c:when test="${empty reservas}">
                        <div class="empty-state">
                            <h3>Nenhuma reserva encontrada</h3>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="table-wrap">
                            <table class="data-table">
                                <thead>
                                <tr>
                                    <th>Area</th>
                                    <th>Morador</th>
                                    <th>Data</th>
                                    <th>Horario</th>
                                    <th>Status</th>
                                    <th></th>
                                </tr>
                                </thead>
                                <tbody>
                                <c:forEach items="${reservas}" var="reserva">
                                    <tr>
                                        <td>${reserva.areaComumNome}</td>
                                        <td>${reserva.moradorNome}</td>
                                        <td>${reserva.data}</td>
                                        <td>${reserva.horaInicio} - ${reserva.horaFim}</td>
                                        <td><span class="status-pill">${reserva.status}</span></td>
                                        <td class="cell-actions">
                                            <c:if test="${reserva.status eq 'SOLICITADA'}">
                                                <form method="post" action="${ctx}/admin/reservas/${reserva.id}/aprovar" class="inline-panel">
                                                    <%@ include file="/WEB-INF/jsp/fragments/csrf.jspf" %>
                                                    <input type="hidden" name="_method" value="patch">
                                                    <button type="submit" class="btn btn-primary">Aprovar</button>
                                                </form>
                                                <form method="post" action="${ctx}/admin/reservas/${reserva.id}/negar" class="inline-panel" data-confirm="Tem certeza que deseja negar esta reserva?">
                                                    <%@ include file="/WEB-INF/jsp/fragments/csrf.jspf" %>
                                                    <input type="hidden" name="_method" value="patch">
                                                    <input type="text" name="motivo" placeholder="Motivo" required maxlength="255">
                                                    <button type="submit" class="btn btn-secondary">Negar</button>
                                                </form>
                                            </c:if>
                                            <c:if test="${reserva.status eq 'SOLICITADA' or reserva.status eq 'APROVADA'}">
                                                <form method="post" action="${ctx}/admin/reservas/${reserva.id}/cancelar" class="inline-panel" data-confirm="Tem certeza que deseja cancelar esta reserva?">
                                                    <%@ include file="/WEB-INF/jsp/fragments/csrf.jspf" %>
                                                    <input type="hidden" name="_method" value="patch">
                                                    <button type="submit" class="btn btn-danger">Cancelar</button>
                                                </form>
                                            </c:if>
                                        </td>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>

                <div class="pagination">
                    <c:if test="${reservasPage.hasPrevious}">
                        <a class="btn btn-secondary" href="${ctx}/admin/reservas?page=${reservasPage.page - 1}&size=${reservasPage.size}&status=${filtroStatus}&areaComumId=${filtroAreaComumId}&data=${filtroData}">Anterior</a>
                    </c:if>
                    <span>Pagina ${reservasPage.page + 1} de ${reservasPage.totalPages == 0 ? 1 : reservasPage.totalPages}</span>
                    <c:if test="${reservasPage.hasNext}">
                        <a class="btn btn-secondary" href="${ctx}/admin/reservas?page=${reservasPage.page + 1}&size=${reservasPage.size}&status=${filtroStatus}&areaComumId=${filtroAreaComumId}&data=${filtroData}">Proxima</a>
                    </c:if>
                </div>
            </section>
        </main>
    </div>
</div>
<%@ include file="/WEB-INF/jsp/fragments/scripts.jspf" %>
</body>
</html>