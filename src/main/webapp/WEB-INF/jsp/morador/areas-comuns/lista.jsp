<%@ include file="/WEB-INF/jsp/fragments/taglibs.jspf" %>
<!DOCTYPE html>
<html lang="pt-BR">
<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>
<body data-page="morador-areas-comuns">
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
                        <h2>Areas comuns</h2>
                        <p class="section-subtitle">Consulte a disponibilidade antes de solicitar uma reserva.</p>
                    </div>
                </div>

                <c:choose>
                    <c:when test="${empty areasComuns}">
                        <div class="empty-state">
                            <h3>Nenhuma area comum cadastrada</h3>
                            <p>Aguarde o administrador cadastrar as areas disponiveis.</p>
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
                                </tr>
                                </thead>
                                <tbody>
                                <c:forEach items="${areasComuns}" var="area">
                                    <tr>
                                        <td>${area.nome}</td>
                                        <td>${area.descricao}</td>
                                        <td>
                                            <span class="status-pill">${area.ativa ? 'Ativa' : 'Inativa'}</span>
                                        </td>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>
            </section>

            <section class="card">
                <div class="section-header">
                    <div>
                        <p class="eyebrow">Disponibilidade</p>
                        <h2>Consultar horario</h2>
                        <p class="section-subtitle">Escolha a area, a data e o intervalo desejado.</p>
                    </div>
                </div>

                <form method="get" action="${ctx}/morador/areas-comuns" class="filter-grid">
                    <label class="field">
                        <span>Area comum</span>
                        <select name="areaComumId" required>
                            <option value="">Selecione</option>
                            <c:forEach items="${areasComuns}" var="area">
                                <option value="${area.id}" ${filtroAreaComumId eq area.id ? 'selected' : ''}>${area.nome}</option>
                            </c:forEach>
                        </select>
                    </label>
                    <label class="field">
                        <span>Data</span>
                        <input type="date" name="data" value="${filtroData}" required>
                    </label>
                    <label class="field">
                        <span>Horario inicial</span>
                        <input type="time" name="horaInicio" value="${filtroHoraInicio}" required>
                    </label>
                    <label class="field">
                        <span>Horario final</span>
                        <input type="time" name="horaFim" value="${filtroHoraFim}" required>
                    </label>
                    <div class="button-row align-end">
                        <button type="submit" class="btn btn-primary">Consultar</button>
                    </div>
                </form>

                <c:if test="${not empty resultadoDisponibilidade}">
                    <div class="detail-list">
                        <div>
                            <span>Resultado</span>
                            <strong>
                                <span class="status-pill">
                                    ${resultadoDisponibilidade ? 'Disponivel' : 'Indisponivel'}
                                </span>
                            </strong>
                        </div>
                    </div>

                    <c:if test="${resultadoDisponibilidade}">
                        <form method="post" action="${ctx}/morador/reservas" class="inline-panel">
                            <%@ include file="/WEB-INF/jsp/fragments/csrf.jspf" %>
                            <input type="hidden" name="areaComumId" value="${filtroAreaComumId}">
                            <input type="hidden" name="data" value="${filtroData}">
                            <input type="hidden" name="horaInicio" value="${filtroHoraInicio}">
                            <input type="hidden" name="horaFim" value="${filtroHoraFim}">
                            <button type="submit" class="btn btn-primary">Solicitar esta reserva</button>
                        </form>
                    </c:if>
                </c:if>
            </section>
        </main>
    </div>
</div>
<%@ include file="/WEB-INF/jsp/fragments/scripts.jspf" %>
</body>
</html>