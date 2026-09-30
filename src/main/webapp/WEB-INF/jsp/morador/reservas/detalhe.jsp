<%@ include file="/WEB-INF/jsp/fragments/taglibs.jspf" %>
<!DOCTYPE html>
<html lang="pt-BR">
<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>
<body data-page="morador-reserva-detalhe">
<div class="app-shell">
    <%@ include file="/WEB-INF/jsp/fragments/sidebar.jspf" %>
    <div class="app-main">
        <%@ include file="/WEB-INF/jsp/fragments/topbar.jspf" %>
        <main class="page-content">
            <%@ include file="/WEB-INF/jsp/fragments/alerts.jspf" %>

            <section class="card">
                <div class="section-header">
                    <div>
                        <p class="eyebrow">Reserva</p>
                        <h2>${reserva.areaComumNome}</h2>
                    </div>
                    <span class="status-pill">${reserva.status}</span>
                </div>

                <div class="detail-list">
                    <div><span>Data</span><strong>${reserva.data}</strong></div>
                    <div><span>Horario</span><strong>${reserva.horaInicio} - ${reserva.horaFim}</strong></div>
                    <div><span>Solicitada em</span><strong>${reserva.dataSolicitacaoFormatada}</strong></div>
                    <div>
                        <span>Decisao</span>
                        <strong><c:out value="${empty reserva.dataDecisaoFormatada ? 'Ainda nao decidida' : reserva.dataDecisaoFormatada}" /></strong>
                    </div>
                    <c:if test="${reserva.status eq 'NEGADA'}">
                        <div><span>Motivo da negacao</span><strong>${reserva.motivoNegacao}</strong></div>
                    </c:if>
                </div>

                <c:if test="${reserva.status eq 'SOLICITADA' or reserva.status eq 'APROVADA'}">
                    <form method="post" action="${ctx}/morador/reservas/${reserva.id}/cancelar" class="inline-panel">
                        <%@ include file="/WEB-INF/jsp/fragments/csrf.jspf" %>
                        <input type="hidden" name="_method" value="patch">
                        <button type="submit" class="btn btn-danger">Cancelar reserva</button>
                    </form>
                </c:if>
            </section>

            <section class="card">
                <div class="section-header">
                    <div>
                        <p class="eyebrow">Auditoria</p>
                        <h2>Historico</h2>
                    </div>
                </div>

                <c:choose>
                    <c:when test="${empty historico}">
                        <div class="empty-state compact">
                            <p>Nenhum evento registrado ainda.</p>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="timeline">
                            <c:forEach items="${historico}" var="evento">
                                <article class="timeline-item">
                                    <header>
                                        <strong>${evento.autorNome}</strong>
                                        <span>${evento.autorTipoLabel} &bull; ${evento.dataEventoFormatada}</span>
                                    </header>
                                    <p>
                                        <c:choose>
                                            <c:when test="${empty evento.statusAnteriorLabel}">
                                                Reserva solicitada (status inicial: ${evento.statusNovoLabel})
                                            </c:when>
                                            <c:otherwise>
                                                ${evento.statusAnteriorLabel} &rarr; ${evento.statusNovoLabel}
                                            </c:otherwise>
                                        </c:choose>
                                    </p>
                                    <c:if test="${not empty evento.observacao}">
                                        <p><span>Motivo:</span> ${evento.observacao}</p>
                                    </c:if>
                                </article>
                            </c:forEach>
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