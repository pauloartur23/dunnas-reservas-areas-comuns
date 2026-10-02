# ESPECIFICACAO.md

> Decisões técnicas, interpretações e imprecisões encontradas no
> desenvolvimento da funcionalidade de **Reservas de Áreas Comuns**
> (Desafio nº 003/2026, Dunnas Tecnologia), sobre o sistema de
> gerenciamento de chamados para condomínio recebido como base.

## 1. Decisões técnicas e justificativas

### 1.1 Stack escolhida
Optei pela **Opção 2 (Java + Spring Boot + JSP + PostgreSQL)** por ser a
stack que eu já domino melhor, reduzindo risco de erros por
desconhecimento de framework e permitindo focar o tempo disponível no
entendimento do sistema legado e na qualidade da solução, em vez de na
curva de aprendizado de uma stack nova (Ruby on Rails).

### 1.2 Modelagem de dados
- `AreaComum`: entidade nova, com `nome`, `descricao` (opcional) e `ativa`
  (booleano, padrão `true`). O campo `ativa` segue o mesmo padrão já usado
  em `Usuario.ativo`: áreas nunca são deletadas fisicamente, apenas
  desativadas — implementa RN-01-01 (área retirada/desativada não aceita
  novas solicitações, mas preserva reservas existentes).
- `Reserva`: referencia `AreaComum` e `Morador`; campos `data`,
  `horaInicio`, `horaFim` separados (em vez de dois `LocalDateTime`) para
  facilitar comparação de intervalos dentro do mesmo dia. Estado
  modelado como enum `StatusReserva` (`SOLICITADA`, `APROVADA`, `NEGADA`,
  `CANCELADA`), persistido como `STRING` (não `ORDINAL`) para não quebrar
  dados existentes caso o conjunto de estados mude no futuro.
- Campos de rastreabilidade adicionados além do mínimo pedido:
  `administradorDecisao` (quem decidiu), `motivoNegacao`,
  `dataSolicitacao`, `dataDecisao`, `dataCancelamento` — atendem RF-04
  ("decisão administrativa rastreável") e dão suporte ao diferencial de
  auditoria.
- `UUID` como chave primária em todas as tabelas novas, consistente com
  o padrão de todas as entidades existentes no projeto.

### 1.3 Tratamento de concorrência (RN-01-07 / CA-01-08)
O projeto base **não implementa nenhum tratamento de concorrência** em
nenhuma entidade (verificado em `Chamado.java` e `ChamadoService.java` —
sem `@Version`, sem lock, sem constraint). Como o desafio exige
explicitamente que decisões simultâneas não produzam duas reservas
`APROVADA` conflitantes, essa garantia não podia ser copiada do código
existente e precisou ser desenhada do zero.

**Decisão**: combinar duas camadas de proteção:
1. **Lock pessimista** (`SELECT ... FOR UPDATE`) sobre as reservas
   concorrentes da mesma área no momento da aprovação, forçando
   serialização de decisões simultâneas.
2. **Constraint de exclusão no PostgreSQL** (`EXCLUDE USING gist`) sobre
   `area_comum_id` + intervalo de tempo, filtrando apenas reservas
   `APROVADA`, como rede de segurança final garantida pelo próprio
   banco — independente de qualquer caminho de código que possa vir a
   ignorar o lock.

Motivo de não usar apenas uma das duas: lock pessimista sozinho depende
de todo o código de aprovação passar pelo mesmo caminho disciplinado;
constraint sozinha resolveria o conflito mas devolveria um erro de banco
"cru" em vez de uma negação de negócio tratada. A combinação cobre tanto
a robustez (garantia no nível de dado) quanto a experiência (erro de
negócio claro no caminho feliz).

### 1.4 Padrão de consultas via funções PL/pgSQL
Identificado que o projeto base usa um padrão consistente e **ativo**
(confirmado via busca no código-fonte, não apenas nas migrations) de
mover lógica de consulta e autorização para funções PL/pgSQL no banco
(ex.: `fn_listar_chamados_do_morador`, `fn_morador_pode_ver_chamado`),
chamadas a partir dos `Repository` via `@Query(nativeQuery = true)`.

**Decisão**: seguir o mesmo padrão para Reservas — funções como
`fn_area_comum_esta_disponivel` e `fn_reserva_conflita` no banco, em vez
de montar a lógica de disponibilidade/conflito inteiramente em Java —
para manter consistência arquitetural com o restante do projeto (ponto
de avaliação explícito: "consistência da solução com as decisões já
presentes no projeto").

### 1.5 Estrutura de controllers
Verificado que, apesar dos nomes de pacote (`controller/api` e `controller/web`),
**nenhum dos dois é uma API REST/JSON** — ambos são `@Controller` (não
`@RestController`), retornam nomes de view ou `redirect:`, e ficam sob a
cadeia de segurança de sessão/JSP, não sob `/api/**` (JWT). A separação real
é por responsabilidade: `web` = páginas de leitura (GET, `Model`), `api` =
processamento de formulário e mutação (POST/PATCH, `RedirectAttributes`).

**Decisão**: seguir essa mesma separação para Reservas, criando 4
controllers (`MoradorReservaWebController`, `MoradorReservaApiController`,
`AdminReservaWebController`, `AdminReservaApiController`), em vez de expor
uma API REST/JSON nova — por consistência com o restante do projeto.

### 1.6 Ordem entre trava e leitura nas decisões
Toda mudança de estado de uma reserva (aprovar, negar, cancelar) descobre
primeiro a área da reserva, tranca a linha da área (`SELECT ... FOR UPDATE`) e
só depois lê a reserva. Ler antes de trancar permitiria decidir com base em
estado velho (por exemplo, um cancelamento concorrente). O lock é por área, e
não por reserva, porque duas reservas diferentes da mesma área disputam o
mesmo intervalo (CA-01-08).

### 1.7 Visualização em agenda/calendário (Escopo funcional #5, RN-01-09, CA-01-02, CA-01-09)
O enunciado pede acompanhamento "em lista e agenda/calendário" e que a
consulta de disponibilidade "distinga" reservas aprovadas de solicitações
pendentes. A tela de consulta (`/morador/areas-comuns`) resolve isso
ponto a ponto (um horário específico: disponível ou não), e a listagem
(`/…/reservas`) resolve o acompanhamento em tabela — mas nenhuma das
duas dá uma visão de conjunto do dia.

**Decisão**: adicionei uma tela de **agenda do dia**
(`/morador/reservas/agenda` e `/admin/reservas/agenda`), que agrupa por
área comum as reservas `APROVADA` (ocupam o horário) e `SOLICITADA`
(pendentes, não ocupam, mas aparecem para quem consulta não pensar que o
horário está livre) de uma data, ordenadas por horário de início —
satisfazendo a distinção pedida em CA-01-02/CA-01-09 sem introduzir
nenhuma regra de negócio nova: a tela só agrupa e filtra, em Java, o
resultado dos mesmos métodos de serviço já testados
(`listarMinhasReservas`/`listarReservasParaAdmin`), sem nova consulta ao
banco, nova migration nem mudança em `ReservaService`. Optei por uma
agenda "por dia" (não uma grade de calendário mensal com arrastar/soltar,
que o próprio enunciado exclui do escopo) por ser suficiente para o
critério de aceite e por poder ser entregue com baixo risco, reaproveitando
código já coberto pelos 76 testes existentes.

### 1.8 Duração máxima de reserva por área comum (decisão própria)
O enunciado não pede limite de duração por reserva. Decidi adicionar essa
regra por conta própria, no mesmo espírito do horário de funcionamento
(1.2): sem ela, uma única reserva poderia ocupar uma área comum por um
intervalo de tempo desproporcional (por exemplo, o dia inteiro), o que não
parece uma restrição realista para uma área compartilhada.

**Decisão**: adicionei um campo opcional `duracao_maxima_minutos` em
`areas_comuns` (migration `V26`). Quando cadastrado, uma reserva cujo
intervalo (`hora_fim - hora_inicio`) exceda esse limite é recusada, tanto
na consulta de disponibilidade quanto na solicitação. Sem valor cadastrado
(`null`), a área mantém o comportamento anterior, sem limite de duração —
não quebra nenhuma área já existente. Deliberadamente **não** implementei
um limite de "quantidade de reservas por morador por semana": o enunciado
lista "reservas recorrentes ou regras complexas de repetição" em "Fora do
escopo", e uma regra de cota semanal se encaixa nessa categoria.

## 2. Interpretações de pontos não detalhados no enunciado

- Campos de `AreaComum` além de `nome`: o desafio não detalha o cadastro
  de área além de mencioná-lo em RF-01. Assumi um conjunto mínimo
  (`nome`, `descricao` opcional, `ativa`) e documento aqui essa
  interpretação — pode ser expandido conforme necessidade percebida
  durante o desenvolvimento da tela de cadastro.

- Horário de funcionamento das áreas: o enunciado não prevê. Decisão própria,
  tomada porque um cadastro sem isso ficaria irreal (área aceitando reserva de
  madrugada). Foram adicionados os campos opcionais `horario_abertura` e
  `horario_fechamento` (migration V22): ou os dois vazios, ou os dois
  preenchidos com fechamento posterior à abertura. Área sem horário não tem
  restrição, o que preserva exatamente o comportamento descrito no enunciado.
  Com horário, consulta e solicitação fora da janela são recusadas. A regra
  vale só para novas solicitações: alterar o horário depois não mexe em
  reservas existentes (RN-01-01).
- "Incluir, alterar ou retirar" área (RN-01-01): implementado como cadastrar,
  editar (nome, descrição e horário) e ativar/desativar. "Retirar" significa
  desativar; a área nunca é apagada.
- Aprovação depois do início: recusada (CA-01-12). Negação depois do início:
  mantida permitida, pois o enunciado só restringe aprovar e cancelar.
- Uma reserva não atravessa a meia-noite. Guardei uma data com horário de
  início e de fim, e o fim precisa ser posterior ao início (RN-01-02). Uma
  festa das 22:00 às 02:00 precisa ser dividida em duas reservas. Limitação
  conhecida.

## 3. Imprecisões / comportamentos encontrados no código recebido

- `JwtAuthenticationFilter.java` usa uma API deprecated do Spring
  Security (aviso emitido no build, não é erro). Não corrigido por estar
  fora do escopo desta funcionalidade (regra do desafio: não alterar o
  funcionamento existente dos chamados/autenticação). Registrado aqui
  como observação.
- Nenhuma entidade do sistema original implementa controle de
  concorrência (sem `@Version`, sem locks). Não é um problema para o
  fluxo de chamados como está hoje, mas é uma limitação real da base
  que não podia ser herdada para Reservas, dado o requisito explícito
  de RN-01-07. Não alterei o código de chamados para corrigir isso —
  fora do escopo desta entrega — apenas registrei a observação.
- `WebExceptionHandler` (`@ControllerAdvice`) só cobre o pacote
  `controller.web`; controllers do pacote `controller.api` (que, apesar
  do nome, também são controllers web tradicionais, não REST — ver seção
  1.5) não têm nenhuma exceção tratada, incluindo os já existentes de
  chamados (`MoradorChamadoApiController` etc.). Uma `BusinessRuleException`
  lançada a partir deles hoje provavelmente resulta em erro genérico (HTTP
    500) em vez do redirecionamento amigável usado no resto do sistema.
         **Decisão**: não alterei o `WebExceptionHandler` existente (isso mudaria
         o comportamento observável dos chamados, violando RNF-01). Em vez disso,
         criei um `@ControllerAdvice` novo e isolado (`ReservaApiExceptionHandler`),
         restrito via `assignableTypes` exatamente aos meus dois controllers novos
         (`MoradorReservaApiController`, `AdminReservaApiController`) — resolve o
         problema para esta funcionalidade sem tocar em nada pré-existente. O
         mesmo problema para os controllers de chamados fica registrado aqui, mas
         não corrigido, por estar fora do escopo desta entrega.

- O botão de fechar as mensagens de alerta aparece com um símbolo quebrado
  ("Ã" seguido de um quadrado), o que indica problema de codificação de
  caracteres no fragmento de alertas. É cosmético e vem da base (não mexi
  nesse fragmento). A confirmar se as mensagens dos chamados mostram o mesmo
  símbolo. Não corrigido, por estar fora do escopo.

- **`README.md` do projeto base estava ausente do repositório.** Ele existia
  no commit inicial (1219 linhas, com toda a documentação do sistema de
  chamados), mas foi apagado no mesmo dia, em um commit posterior do próprio
  setup do projeto base (`d521e6a "feat(env) adicionando env.example"`,
  anterior ao início do meu trabalho de Reservas) — ao que tudo indica, sem
  intenção, ao adicionar o `.env.example`. **Decisão**: recuperei o conteúdo
  original a partir do histórico do Git (`git show <commit>:README.md`) e o
  restaurei na raiz do repositório, atualizando-o com a documentação desta
  funcionalidade (funcionalidades por perfil, tabelas novas, endpoints,
  migrations e instruções de execução), em vez de escrever um documento
  novo do zero — mantendo a exigência do desafio de "atualizar a
  documentação que já existe, em vez de criar um documento paralelo".

- **`docker-compose.yml` nunca repassava a variável `TOKEN` para o container
  da aplicação.** A variável estava declarada (por engano) no bloco de
  ambiente do serviço `db`, que não a utiliza, e nunca aparecia no bloco do
  serviço `app`. Como `application.properties` usa
  `api.security.token.secret=${TOKEN}` sem valor padrão, isso significa que
  o container da aplicação **nunca teria conseguido iniciar** via
  `docker compose up` — o Spring Boot falha com
  `Could not resolve placeholder 'TOKEN'`. Esse erro já existia no
  `docker-compose.yml` do commit inicial do projeto base, antes de qualquer
  alteração minha (`git log --follow` confirma que o arquivo nunca foi
  tocado desde então). Combinado com a dependência de um arquivo `.env`
  obrigatório (sem valores padrão, então `docker compose up` sozinho também
  falhava por falta do arquivo), isso quer dizer que **o diferencial "subir
  o projeto localmente apenas com o comando `docker compose up`" nunca
  havia funcionado nesta base, em nenhum momento**. **Correção**: movi
  `TOKEN` para o serviço `app` e dei a cada variável um valor padrão de
  desenvolvimento diretamente no `docker-compose.yml` (sintaxe
  `${VAR:-padrao}`), eliminando a necessidade de um `.env` sem deixar
  nenhuma credencial versionada — quem quiser customizar valores ainda pode
  criar um `.env` local (nunca commitado) a partir do `.env.example`, que o
  Compose lê e usa para sobrescrever os padrões.

## 4. Fora do escopo (decisão minha, além do que o enunciado já exclui)

- Horário de funcionamento por dia da semana, feriados e bloqueios pontuais:
  só existe uma janela diária única por área.
- Edição de uma reserva já criada (data ou horário): o morador cancela e
  solicita de novo.
- Limite de reservas por morador ou por unidade e antecedência mínima ou
  máxima para solicitar (duração máxima já é tratada na seção 1.8).
- Expiração automática de solicitação pendente cujo horário já passou: ela
  continua SOLICITADA no histórico, pois o enunciado não define um estado
  para isso.
- Tela de detalhe de reserva para o administrador: as ações (aprovar, negar,
  cancelar) ficam na própria listagem.

## 5. Documentação do uso de IA

Usei um assistente de IA (Claude) como apoio ao longo do desenvolvimento,
principalmente de duas formas: para explicar partes do código-base que eu
não estava entendendo bem a coerência/relação entre elas (a partir daí eu
ia expandindo, entendendo as regras de negócio e montando o escopo, as
entidades etc. por conta própria); e para depurar bugs de teste comigo —
eu ia dizendo o que achava que podia ser a causa, ele ia devolvendo
ideias, e a gente foi conversando até sair do problema.

**Exemplos disso:**
- Os testes de controller nunca tinham rodado. Fomos conversando sobre
  possíveis causas até chegar que faltava um mock de `JwtService` no
  `@WebMvcTest`. Depois disso os testes continuavam falhando por
  autenticação; seguimos investigando juntos até entender que
  `addFilters = false` desligava o mecanismo que o Spring Security Test
  usa pra simular o usuário logado, e corrigimos isso.
- Eu não entendia por que existiam pastas `web` e `api` separadas fazendo
  praticamente a mesma coisa nos controllers; perguntei, entendi que a
  separação era por responsabilidade (leitura vs. mutação), e segui esse
  mesmo padrão para Reservas.
- Conversamos sobre como impedir que duas aprovações conflitantes
  acontecessem ao mesmo tempo; discuti as alternativas (trava na
  aplicação vs. restrição no próprio banco) e decidi usar as duas juntas.
- Não sabia por que várias consultas do projeto ficavam em funções SQL
  dentro do banco em vez de só em Java; perguntei, entendi o padrão
  (Flyway + funções PL/pgSQL chamadas pelos repositories), e segui esse
  mesmo padrão nas consultas novas de Reservas.
- Depois de entregar a funcionalidade, perguntei se fazia sentido
  adicionar uma duração máxima configurável por área comum, e também se o
  enunciado pedia algum limite de "reservas por semana"; discutimos o
  enunciado e concluí que duração máxima era uma boa decisão própria, mas
  um limite semanal se encaixava na exclusão de "reservas recorrentes ou
  regras complexas de repetição" do próprio enunciado (seção 1.8).
- Depois de ter a funcionalidade pronta, pedi uma revisão de ponta a ponta
  no nível que eu imaginava ser cobrado numa entrega pra uma empresa
  grande: se havia alguma credencial exposta (no código e em todo o
  histórico do Git) e se faltava cobrir alguma regra de negócio do PDF do
  desafio. Não apareceu nenhuma credencial exposta, mas a revisão achou
  uma lacuna real: o enunciado pede explicitamente pra "exercitar
  autorização por MORADOR, ADMINISTRADOR e COLABORADOR, incluindo acesso
  indevido e tentativa de consultar dados de outro morador", e eu só
  tinha essa regra coberta nos testes de serviço, nunca na camada HTTP
  dos controllers. Escrevi o `ReservaWebControllerIntegrationTest` pra
  cobrir isso; na primeira rodada, 8 dos 14 testes falharam, e fomos
  investigando juntos a causa a partir do log real do Maven, até entender
  que o `@WebMvcTest` não carrega o `@EnableMethodSecurity` da
  configuração principal (só os controllers listados na anotação), então
  o `@PreAuthorize` não era aplicado nesse tipo de teste fatiado —
  precisei declarar essa anotação direto na classe de teste e ajustar
  como as negações de acesso são verificadas.

**O que não deleguei:** as regras de negócio e o código, a stack, o
escopo final da entrega, as interpretações dos pontos não detalhados do
enunciado, quais imprecisões do código-base valia corrigir ou só
registrar, e o aceite de cada parte antes de commitar.

**Como validei:** rodando `mvn test` a cada mudança (94 testes, 0
falhas), conferindo a cobertura no IntelliJ, testando os fluxos
manualmente, e revisando as regras de negócio do enunciado uma a uma
contra o código.

## 6. Testes automatizados

### 6.1 Testes escritos para a nova funcionalidade
- **`ReservaServiceTest`** (23 testes unitários, `JUnit 5` + `Mockito`,
  seguindo o mesmo estilo já usado em `ChamadoServiceTest`): cobre
  unicidade de nome de área (cadastro e atualização), validação de
  horário/dia de funcionamento, validação da duração máxima configurada
  por área (cadastro com valor inválido, consulta e solicitação que
  excedem o limite, e solicitação com duração exatamente igual ao
  limite), solicitação de reserva (sucesso e área inativa), aprovação
  (sucesso, conflito de horário, reserva já iniciada), negação (sucesso e
  motivo vazio) e cancelamento por morador e por administrador (sucesso e
  reserva já cancelada). Todos os cenários também verificam que o
  registro de auditoria (`ReservaHistorico`) é gravado corretamente a
  cada transição de estado.
- **`ReservaConcorrenciaIntegrationTest`** (1 teste de integração,
  `@DataJpaTest` com banco H2 real): prova de forma reproduzível que a
  proteção contra aprovação dupla (RN-01-07 / CA-01-08) funciona de
  verdade, e não só na teoria. Duas `Reserva` conflitantes são criadas,
  e duas threads chamam `aprovarReserva(...)` ao mesmo tempo (via
  `CyclicBarrier` + `ExecutorService`, forçando concorrência real, não
  simulada). O teste verifica que exatamente uma aprovação é bem
  sucedida e a outra lança `BusinessRuleException`, e que o banco
  termina com exatamente uma reserva `APROVADA`. O log SQL do Hibernate
  durante a execução confirma duas instruções `select ... for update`
  (o lock pessimista sendo de fato acionado) e apenas um `insert into
  reserva_historico` / `update reservas` bem-sucedido.
- **`ReservaWebControllerIntegrationTest`** (14 testes de integração,
  `@WebMvcTest` sobre os 4 controllers de Reserva): cobre o caminho feliz
  de cada perfil (morador lista áreas comuns e solicita reserva;
  administrador lista e aprova reserva) e, principalmente, autorização —
  exatamente o que o enunciado pede em "exercitar autorização por
  MORADOR, ADMINISTRADOR e COLABORADOR, incluindo acesso indevido e
  tentativa de consultar dados de outro morador": COLABORADOR tentando
  acessar ou alterar qualquer rota de morador ou de admin (acesso
  negado, sem nenhuma chamada ao serviço), MORADOR tentando acessar
  rotas exclusivas do admin (cadastrar área comum, aprovar/negar
  reserva) e ADMINISTRADOR tentando acessar a listagem do morador —
  todos negados por `@PreAuthorize`. Cobre também isolamento de dados
  entre moradores: um morador tentando visualizar ou cancelar uma
  reserva de outro morador recebe "não encontrada", porque o serviço já
  filtra por `moradorId` e o controller nunca expõe se a reserva de
  outro morador existe de verdade.

### 6.2 Cobertura de código da `ReservaService`
Medida com a ferramenta de cobertura embutida do IntelliJ ("Run with
Coverage") sobre o `ReservaServiceTest`:

| Métrica    | Resultado      |
|------------|----------------|
| Classes    | 100% (1/1)     |
| Métodos    | 77% (24/31)    |
| Linhas     | 80% (167/208)  |
| Branches   | 68% (55/80)    |

Todas as métricas superam com folga o mínimo de 40% exigido pelo
desafio para a lógica de negócio da nova funcionalidade.

### 6.3 Bugs pré-existentes encontrados e corrigidos ao viabilizar os testes
Ao rodar `mvn test` pela primeira vez nesta entrega, descobri que uma
parte relevante da suíte de testes **já existia no projeto recebido, mas
nunca havia executado com sucesso** — os testes falhavam por problemas de
configuração anteriores a esta funcionalidade, não por causa do código de
Reservas. Como esses testes cobrem também `Chamado` (fora do escopo
desta entrega), optei por investigar e corrigir, documentando aqui em
vez de simplesmente ignorar, já que deixá-los quebrados mascararia
regressões futuras em qualquer parte do sistema.

1. **Dependência do H2 ausente do `pom.xml`.** O projeto já tinha um
   teste (`ChamadoRepositoryIntegrationTest`) escrito para rodar contra
   um banco H2 em memória, mas a dependência `com.h2database:h2` nunca
   foi adicionada ao `pom.xml`. Resultado: esse teste (e qualquer outro
   baseado em `@DataJpaTest`) falhava ao iniciar com "Failed to replace
   DataSource with an embedded database". **Correção**: adicionada a
   dependência `h2` em escopo `test`.

2. **SQL nativo do PostgreSQL incompatível com H2.** Depois de
   adicionado o H2, `ChamadoRepository.marcarChamadosAtrasados()`
   continuava falhando: a consulta usava a sintaxe
   `UPDATE ... FROM ...` (específica do PostgreSQL), que o H2 não
   entende. **Correção**: reescrita em HQL portável (usando
   `timestampadd` e subconsultas por entidade em vez de aritmética de
   `interval` do Postgres), deixando o próprio Hibernate traduzir para
   a sintaxe certa de cada banco — a mesma consulta passou a funcionar
   tanto em produção (Postgres) quanto nos testes (H2, que a traduz
   para um `MERGE`).

3. **`JwtService` ausente no contexto dos testes `@WebMvcTest`.** Os 4
   testes de controllers web (`AdminWebControllerIntegrationTest`,
   `AuthAndHomeWebControllerIntegrationTest`,
   `ColaboradorWebControllerIntegrationTest`,
   `MoradorWebControllerIntegrationTest`) falhavam todos com
   "APPLICATION FAILED TO START": o `@WebMvcTest` escaneia o
   `JwtAuthenticationFilter` (por ele implementar `Filter`), cujo
   construtor exige um `JwtService`, mas essa dependência (um `@Service`
   comum) não é escaneada automaticamente por esse tipo de teste fatiado.
   **Correção**: adicionado um dublê (`@MockitoBean`) do `JwtService` em
   cada um dos 4 arquivos.

4. **Autenticação simulada não chegava ao controller.** Mesmo depois da
   correção acima, os mesmos 4 testes continuavam falhando, agora com
   "Usuario nao autenticado": eles usam
   `@AutoConfigureMockMvc(addFilters = false)` para desligar o
   `JwtAuthenticationFilter` durante os testes, mas isso desliga **todos**
   os filtros — inclusive o que o helper padrão do Spring Security Test
   (`SecurityMockMvcRequestPostProcessors.authentication(...)`) usa para
   levar a autenticação simulada até o controller. **Correção**: o
   helper `WebTestAuthenticationFactory.authentication(...)` foi
   reescrito para colocar a autenticação diretamente nos dois lugares
   que o Spring efetivamente lê (`SecurityContextHolder` e
   `request.setUserPrincipal(...)`), sem depender de nenhum filtro.

Com essas 4 correções, a suíte completa do projeto passou de testes que
nunca haviam rodado para **80 testes, 0 falhas** (`mvn test` —
`BUILD SUCCESS`).

## 7. Perguntas que eu faria antes de começar

1. As áreas têm horário de funcionamento, dias fechados, duração mínima ou
   máxima de reserva? (Assumi janela diária opcional, e depois adicionei
   duração máxima opcional por conta própria — seção 1.8.)
2. Existe antecedência mínima ou máxima para solicitar uma reserva?
3. Uma reserva pode atravessar a meia-noite?
4. Uma solicitação pendente cujo horário passou deve expirar sozinha ou
   continuar SOLICITADA? Negar depois do início deve ser permitido?
5. Existe limite de reservas por morador ou por unidade?
6. Qual é o fuso de referência do condomínio? (Uso America/Sao_Paulo, o
   mesmo já configurado na aplicação.)
7. Reservas canceladas ou negadas devem ficar visíveis para sempre ou existe
   prazo de retenção do histórico?