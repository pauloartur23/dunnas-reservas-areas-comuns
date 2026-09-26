package br.com.dunnastecnologia.chamados.infrastructure.controller.web;

import br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser;
import br.com.dunnastecnologia.chamados.application.pagination.PageResult;
import br.com.dunnastecnologia.chamados.domain.model.AreaComum;
import br.com.dunnastecnologia.chamados.domain.model.Bloco;
import br.com.dunnastecnologia.chamados.domain.model.Chamado;
import br.com.dunnastecnologia.chamados.domain.model.Comentario;
import br.com.dunnastecnologia.chamados.domain.model.Reserva;
import br.com.dunnastecnologia.chamados.domain.model.StatusChamado;
import br.com.dunnastecnologia.chamados.domain.model.TipoChamado;
import br.com.dunnastecnologia.chamados.domain.model.Unidade;
import br.com.dunnastecnologia.chamados.domain.model.Usuario;
import br.com.dunnastecnologia.chamados.infrastructure.exception.UnauthorizedOperationException;
import br.com.dunnastecnologia.chamados.infrastructure.security.adapter.UserDetailsImpl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Component
public class WebControllerSupport {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public AuthenticatedUser authenticatedUser(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof UserDetailsImpl userDetails)) {
            throw new UnauthorizedOperationException("Usuario nao autenticado");
        }

        Usuario usuario = userDetails.getUsuario();
        return new AuthenticatedUser(usuario.getId(), usuario.getEmail(), usuario.getRole());
    }

    public boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                && authentication.getPrincipal() instanceof UserDetailsImpl;
    }

    public org.springframework.data.domain.PageRequest pageRequest(Integer page, Integer size) {
        int resolvedPage = page == null || page < 0
                ? DEFAULT_PAGE
                : page;

        int resolvedSize = size == null || size <= 0
                ? DEFAULT_SIZE
                : Math.min(size, MAX_SIZE);

        return org.springframework.data.domain.PageRequest.of(resolvedPage, resolvedSize);
    }

    public Map<String, Object> pageMetadata(PageResult<?> pageResult) {
        return Map.of(
                "page", pageResult.page(),
                "size", pageResult.size(),
                "totalElements", pageResult.totalElements(),
                "totalPages", pageResult.totalPages(),
                "hasPrevious", pageResult.page() > 0,
                "hasNext", pageResult.page() + 1 < pageResult.totalPages()
        );
    }

    public String homePathForRole(String role) {
        return switch (role) {
            case "ROLE_ADMINISTRADOR" -> "/admin";
            case "ROLE_COLABORADOR" -> "/colaborador";
            case "ROLE_MORADOR" -> "/morador";
            default -> "/";
        };
    }

    public String roleLabel(String role) {
        return switch (role) {
            case "ROLE_ADMINISTRADOR" -> "Administrador";
            case "ROLE_COLABORADOR" -> "Colaborador";
            case "ROLE_MORADOR" -> "Morador";
            default -> role;
        };
    }

    public String userTypeLabel(Usuario usuario) {
        return roleLabel(usuario.getRole());
    }

    public <T> List<Map<String, Object>> mapContent(List<T> content, Function<T, Map<String, Object>> mapper) {
        return content.stream().map(mapper).toList();
    }

    public Map<String, Object> toBlocoMap(Bloco bloco) {
        return Map.of(
                "id", bloco.getId(),
                "identificacao", bloco.getIdentificacao(),
                "quantidadeAndares", bloco.getQuantidadeAndares(),
                "apartamentosPorAndar", bloco.getApartamentosPorAndar()
        );
    }

    public Map<String, Object> toUnidadeMap(Unidade unidade) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", unidade.getId());
        values.put("identificacao", unidade.getIdentificacao());
        values.put("andar", unidade.getAndar());
        values.put("blocoId", unidade.getBloco() == null ? null : unidade.getBloco().getId());
        values.put("blocoIdentificacao", unidade.getBloco() == null ? null : unidade.getBloco().getIdentificacao());
        return values;
    }

    public Map<String, Object> toUsuarioMap(Usuario usuario) {
        return Map.of(
                "id", usuario.getId(),
                "nome", usuario.getNome(),
                "email", usuario.getEmail(),
                "role", usuario.getRole(),
                "tipo", userTypeLabel(usuario)
        );
    }

    public Map<String, Object> toTipoChamadoMap(TipoChamado tipoChamado) {
        return Map.of(
                "id", tipoChamado.getId(),
                "titulo", tipoChamado.getTitulo(),
                "prazoHoras", tipoChamado.getPrazoHoras()
        );
    }

    public Map<String, Object> toStatusChamadoMap(StatusChamado statusChamado) {
        return Map.of(
                "id", statusChamado.getId(),
                "nome", statusChamado.getNome(),
                "inicialPadrao", Boolean.TRUE.equals(statusChamado.getInicialPadrao())
        );
    }

    public Map<String, Object> toChamadoMap(Chamado chamado) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", chamado.getId());
        values.put("descricao", chamado.getDescricao());
        values.put("dataAbertura", chamado.getDataAbertura());
        values.put("dataAberturaFormatada", formatDateTime(chamado.getDataAbertura()));
        values.put("dataFinalizacao", chamado.getDataFinalizacao());
        values.put("dataFinalizacaoFormatada", formatDateTime(chamado.getDataFinalizacao()));
        values.put("finalizado", chamado.getDataFinalizacao() != null);
        values.put("moradorId", chamado.getMorador() == null ? null : chamado.getMorador().getId());
        values.put("moradorNome", chamado.getMorador() == null ? null : chamado.getMorador().getNome());
        values.put("unidadeId", chamado.getUnidade() == null ? null : chamado.getUnidade().getId());
        values.put("unidadeIdentificacao", chamado.getUnidade() == null ? null : chamado.getUnidade().getIdentificacao());
        values.put(
                "blocoIdentificacao",
                chamado.getUnidade() == null || chamado.getUnidade().getBloco() == null
                        ? null
                        : chamado.getUnidade().getBloco().getIdentificacao()
        );
        values.put("tipoChamadoId", chamado.getTipoChamado() == null ? null : chamado.getTipoChamado().getId());
        values.put(
                "tipoChamadoTitulo",
                chamado.getTipoChamado() == null ? null : chamado.getTipoChamado().getTitulo()
        );
        values.put("statusId", chamado.getStatus() == null ? null : chamado.getStatus().getId());
        values.put("statusNome", chamado.getStatus() == null ? null : chamado.getStatus().getNome());
        return values;
    }

    public Map<String, Object> toComentarioMap(Comentario comentario) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", comentario.getId());
        values.put("mensagem", comentario.getMensagem());
        values.put("dataCriacao", comentario.getDataCriacao());
        values.put("dataCriacaoFormatada", formatDateTime(comentario.getDataCriacao()));
        values.put("autorId", comentario.getAutor() == null ? null : comentario.getAutor().getId());
        values.put("autorNome", comentario.getAutor() == null ? null : comentario.getAutor().getNome());
        values.put("autorRole", comentario.getAutor() == null ? null : roleLabel(comentario.getAutor().getRole()));
        values.put(
                "anexos",
                comentario.getAnexos().stream()
                        .map(anexo -> {
                            Map<String, Object> anexoValues = new LinkedHashMap<>();
                            anexoValues.put("id", anexo.getId());
                            anexoValues.put("nomeArquivo", anexo.getNomeArquivo());
                            anexoValues.put("contentType", anexo.getContentType());
                            anexoValues.put("tamanhoBytes", anexo.getTamanhoBytes());
                            anexoValues.put("tamanhoFormatado", formatBytes(anexo.getTamanhoBytes()));
                            return anexoValues;
                        })
                        .toList()
        );
        return values;
    }

    public Map<String, Object> toAnexoMap(
            UUID id,
            String nomeArquivo,
            String contentType,
            long tamanhoBytes
    ) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", id);
        values.put("nomeArquivo", nomeArquivo);
        values.put("contentType", contentType);
        values.put("tamanhoBytes", tamanhoBytes);
        values.put("tamanhoFormatado", formatBytes(tamanhoBytes));
        return values;
    }

    public Map<String, Object> toAreaComumMap(AreaComum areaComum) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", areaComum.getId());
        values.put("nome", areaComum.getNome());
        values.put("descricao", areaComum.getDescricao());
        values.put("ativa", Boolean.TRUE.equals(areaComum.getAtiva()));
        return values;
    }

    public Map<String, Object> toReservaMap(Reserva reserva) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", reserva.getId());
        values.put("areaComumId", reserva.getAreaComum() == null ? null : reserva.getAreaComum().getId());
        values.put("areaComumNome", reserva.getAreaComum() == null ? null : reserva.getAreaComum().getNome());
        values.put("moradorId", reserva.getMorador() == null ? null : reserva.getMorador().getId());
        values.put("moradorNome", reserva.getMorador() == null ? null : reserva.getMorador().getNome());
        values.put("data", reserva.getData());
        values.put("horaInicio", reserva.getHoraInicio());
        values.put("horaFim", reserva.getHoraFim());
        values.put("status", reserva.getStatus().name());
        values.put("motivoNegacao", reserva.getMotivoNegacao());
        values.put("dataSolicitacao", reserva.getDataSolicitacao());
        values.put("dataSolicitacaoFormatada", formatDateTime(reserva.getDataSolicitacao()));
        values.put("dataDecisao", reserva.getDataDecisao());
        values.put("dataDecisaoFormatada", formatDateTime(reserva.getDataDecisao()));
        values.put("dataCancelamento", reserva.getDataCancelamento());
        return values;
    }

    public UploadedFileData optionalUploadedFile(MultipartFile arquivo, String errorMessage) {
        if (arquivo == null || arquivo.isEmpty()) {
            return null;
        }
        return uploadedFile(arquivo, errorMessage);
    }

    public UploadedFileData requiredUploadedFile(
            MultipartFile arquivo,
            String emptyFileMessage,
            String errorMessage
    ) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException(emptyFileMessage);
        }
        return uploadedFile(arquivo, errorMessage);
    }

    public ResponseEntity<byte[]> downloadResponse(
            String nomeArquivo,
            String contentType,
            long tamanhoBytes,
            byte[] conteudo
    ) {
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (contentType != null && !contentType.isBlank()) {
            mediaType = MediaType.parseMediaType(contentType);
        }

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nomeArquivo).build().toString()
                )
                .contentType(mediaType)
                .contentLength(tamanhoBytes)
                .body(conteudo);
    }

    private String formatDateTime(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return DATE_TIME_FORMATTER.format(value);
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double kilobytes = bytes / 1024.0;
        if (kilobytes < 1024) {
            return String.format(Locale.US, "%.1f KB", kilobytes);
        }
        double megabytes = kilobytes / 1024.0;
        return String.format(Locale.US, "%.1f MB", megabytes);
    }

    private UploadedFileData uploadedFile(MultipartFile arquivo, String errorMessage) {
        try {
            return new UploadedFileData(
                    arquivo.getOriginalFilename(),
                    arquivo.getContentType(),
                    arquivo.getSize(),
                    arquivo.getBytes()
            );
        } catch (Exception exception) {
            throw new IllegalArgumentException(errorMessage, exception);
        }
    }

    public record UploadedFileData(
            String nomeArquivo,
            String contentType,
            long tamanhoBytes,
            byte[] conteudo
    ) {
    }
}