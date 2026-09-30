package br.com.dunnastecnologia.chamados.infrastructure.service;

import br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser;
import br.com.dunnastecnologia.chamados.domain.model.Administrador;
import br.com.dunnastecnologia.chamados.domain.model.AreaComum;
import br.com.dunnastecnologia.chamados.domain.model.Morador;
import br.com.dunnastecnologia.chamados.domain.model.Reserva;
import br.com.dunnastecnologia.chamados.domain.model.StatusReserva;
import br.com.dunnastecnologia.chamados.infrastructure.exception.BusinessRuleException;
import br.com.dunnastecnologia.chamados.infrastructure.repository.AdministradorRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.AreaComumRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.MoradorRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.ReservaRepository;
import br.com.dunnastecnologia.chamados.infrastructure.service.support.AuthenticatedUserValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prova, com banco de verdade (nao mock) e duas threads reais tentando aprovar
 * ao mesmo tempo, que a trava pessimista (PESSIMISTIC_WRITE) na area comum
 * impede a dupla-reserva: das duas aprovacoes concorrentes para reservas com
 * horario conflitante na mesma area, exatamente uma tem sucesso e a outra e
 * rejeitada com BusinessRuleException.
 *
 * Sem essa trava, as duas threads poderiam ler "sem conflito" ao mesmo tempo
 * (antes de qualquer uma commitar) e ambas seriam aprovadas — a piscina, por
 * exemplo, ficaria com dois grupos aprovados no mesmo horario.
 *
 * @Transactional(NOT_SUPPORTED) e essencial aqui: sem isso, o @DataJpaTest
 * embrulharia o proprio metodo de teste numa transacao que so seria commitada
 * (e desfeita) no final, e as duas threads (com suas proprias conexoes) nunca
 * enxergariam os dados de setup criados no metodo de teste.
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:reserva-concorrencia;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
@Import({ReservaService.class, AuthenticatedUserValidator.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ReservaConcorrenciaIntegrationTest {

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private AreaComumRepository areaComumRepository;

    @Autowired
    private MoradorRepository moradorRepository;

    @Autowired
    private AdministradorRepository administradorRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Test
    void duasAprovacoesConcorrentesParaHorariosConflitantesDevemResultarEmApenasUmaAprovada() throws InterruptedException {
        AreaComum area = new AreaComum();
        area.setNome("Piscina Concorrencia " + UUID.randomUUID());
        area.setAtiva(Boolean.TRUE);
        area = areaComumRepository.save(area);

        Morador morador = new Morador();
        morador.setNome("Morador Teste");
        morador.setEmail("morador.concorrencia." + UUID.randomUUID() + "@teste.local");
        morador.setSenha("senha-hash");
        morador.setAtivo(Boolean.TRUE);
        morador = moradorRepository.save(morador);

        Administrador admin = new Administrador();
        admin.setNome("Admin Teste");
        admin.setEmail("admin.concorrencia." + UUID.randomUUID() + "@teste.local");
        admin.setSenha("senha-hash");
        admin.setAtivo(Boolean.TRUE);
        admin = administradorRepository.save(admin);

        LocalDate amanha = LocalDate.now().plusDays(1);

        Reserva reservaA = new Reserva();
        reservaA.setAreaComum(area);
        reservaA.setMorador(morador);
        reservaA.setData(amanha);
        reservaA.setHoraInicio(LocalTime.of(10, 0));
        reservaA.setHoraFim(LocalTime.of(11, 0));
        reservaA.setStatus(StatusReserva.SOLICITADA);
        reservaA.setDataSolicitacao(LocalDateTime.now());
        reservaA = reservaRepository.save(reservaA);

        Reserva reservaB = new Reserva();
        reservaB.setAreaComum(area);
        reservaB.setMorador(morador);
        reservaB.setData(amanha);
        reservaB.setHoraInicio(LocalTime.of(10, 30));
        reservaB.setHoraFim(LocalTime.of(11, 30));
        reservaB.setStatus(StatusReserva.SOLICITADA);
        reservaB.setDataSolicitacao(LocalDateTime.now());
        reservaB = reservaRepository.save(reservaB);

        AuthenticatedUser adminAutenticado = new AuthenticatedUser(admin.getId(), admin.getEmail(), "ROLE_ADMINISTRADOR");

        CyclicBarrier largada = new CyclicBarrier(2);
        AtomicReference<Object> resultadoA = new AtomicReference<>();
        AtomicReference<Object> resultadoB = new AtomicReference<>();
        UUID reservaAId = reservaA.getId();
        UUID reservaBId = reservaB.getId();

        Runnable tarefaA = () -> {
            try {
                largada.await();
                resultadoA.set(reservaService.aprovarReserva(adminAutenticado, reservaAId));
            } catch (Exception e) {
                resultadoA.set(e);
            }
        };
        Runnable tarefaB = () -> {
            try {
                largada.await();
                resultadoB.set(reservaService.aprovarReserva(adminAutenticado, reservaBId));
            } catch (Exception e) {
                resultadoB.set(e);
            }
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.submit(tarefaA);
        executor.submit(tarefaB);
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS), "As duas aprovacoes concorrentes nao terminaram a tempo");

        boolean aFoiAprovada = resultadoA.get() instanceof Reserva;
        boolean bFoiAprovada = resultadoB.get() instanceof Reserva;

        // exatamente uma das duas deve ter sido aprovada, nunca as duas nem nenhuma
        assertNotEquals(aFoiAprovada, bFoiAprovada, "As duas reservas conflitantes nao podem ser aprovadas ao mesmo tempo");

        Object resultadoRejeitado = aFoiAprovada ? resultadoB.get() : resultadoA.get();
        assertInstanceOf(BusinessRuleException.class, resultadoRejeitado);

        Reserva reservaAAposTeste = reservaRepository.findById(reservaAId).orElseThrow();
        Reserva reservaBAposTeste = reservaRepository.findById(reservaBId).orElseThrow();
        long totalAprovadas = (reservaAAposTeste.getStatus() == StatusReserva.APROVADA ? 1 : 0)
                + (reservaBAposTeste.getStatus() == StatusReserva.APROVADA ? 1 : 0);
        assertEquals(1, totalAprovadas, "Deve haver exatamente uma reserva aprovada para o horario conflitante");
    }
}