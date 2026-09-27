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
        </main>
    </div>
</div>
<%@ include file="/WEB-INF/jsp/fragments/scripts.jspf" %>
</body>
</html>