<%@ include file="/WEB-INF/jsp/fragments/taglibs.jspf" %>
<!DOCTYPE html>
<html lang="pt-BR">
<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>
<body data-page="morador-reservas">
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
                        <h2>Minhas reservas</h2>
                        <p class="section-subtitle">Acompanhe o estado das suas solicitacoes.</p>
                    </div>
                    <div class="button-row">
                        <a href="${ctx}/morador/reservas/agenda" class="btn btn-secondary">Ver agenda do dia</a>
                    </div>
                </div>

                <form method="get" action="${ctx}/morador/reservas" class="filter-grid">
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
                        <a href="${ctx}/morador/reservas" class="btn btn-secondary">Limpar</a>
                    </div>
                </form>
            </section>

            <section class="card">
                <c:choose>
                    <c:when test="${empty reservas}">
                        <div class="empty-state">
                            <h3>Nenhuma reserva encontrada</h3>
                            <p>Altere os filtros ou solicite uma nova reserva em Areas Comuns.</p>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="table-wrap">
                            <table class="data-table">
                                <thead>
                                <tr>
                                    <th>Area</th>
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
                                        <td>${reserva.data}</td>
                                        <td>${reserva.horaInicio} - ${reserva.horaFim}</td>
                                        <td><span class="status-pill">${reserva.status}</span></td>
                                        <td class="cell-actions">
                                            <a href="${ctx}/morador/reservas/${reserva.id}" class="btn btn-link">Detalhar</a>
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
                        <a class="btn btn-secondary" href="${ctx}/morador/reservas?page=${reservasPage.page - 1}&size=${reservasPage.size}&status=${filtroStatus}&areaComumId=${filtroAreaComumId}&data=${filtroData}">Anterior</a>
                    </c:if>
                    <span>Pagina ${reservasPage.page + 1} de ${reservasPage.totalPages == 0 ? 1 : reservasPage.totalPages}</span>
                    <c:if test="${reservasPage.hasNext}">
                        <a class="btn btn-secondary" href="${ctx}/morador/reservas?page=${reservasPage.page + 1}&size=${reservasPage.size}&status=${filtroStatus}&areaComumId=${filtroAreaComumId}&data=${filtroData}">Proxima</a>
                    </c:if>
                </div>
            </section>
        </main>
    </div>
</div>
<%@ include file="/WEB-INF/jsp/fragments/scripts.jspf" %>
</body>
</html>