# Sistema de Gerenciamento de Chamados para Condomínio

Este projeto implementa um sistema de gerenciamento de chamados para condomínio, com separação por perfis de acesso, controle de estrutura física do condomínio e acompanhamento completo do ciclo de vida do chamado.

> Este README documenta o projeto base recebido (gestão de chamados) e a funcionalidade de **Reservas de Áreas Comuns**, entregue como evolução dele (Desafio nº 003/2026). As decisões técnicas, interpretações de pontos não detalhados e imprecisões encontradas no código recebido estão em [`ESPECIFICACAO.md`](./ESPECIFICACAO.md).

## Índice Remissivo

- [Funcionalidades Seguidas](#funcionalidades-seguidas)
  - [Administrador](#administrador)
  - [Colaborador](#colaborador)
  - [Morador](#morador)
  - [Chamado](#chamado)
  - [Área Comum e Reserva](#área-comum-e-reserva)
- [Detalhamento do Sistema](#detalhamento-do-sistema)
  - [Principais funcionalidades](#principais-funcionalidades)
- [Padrões de Projeto Utilizados](#padrões-de-projeto-utilizados)
- [Estrutura do Projeto e Princípios de Arquitetura](#estrutura-do-projeto-e-princípios-de-arquitetura)
  - [Árvore de diretórios](#árvore-de-diretórios)
  - [Estrutura do projeto](#estrutura-do-projeto)
  - [Clean Architecture no projeto](#clean-architecture-no-projeto)
  - [Princípios SOLID aplicados ao projeto](#princípios-solid-aplicados-ao-projeto)
  - [Decisões arquiteturais e relação com o projeto](#decisões-arquiteturais-e-relação-com-o-projeto)
- [Diagrama Relacional](#diagrama-relacional)
  - [Documentação dos Modelos](#documentação-dos-modelos)
  - [Tabelas](#tabelas)
  - [Relacionamentos principais](#relacionamentos-principais)
  - [Migrations do Banco](#migrations-do-banco)
- [Endpoints Web](#endpoints-web)
  - [Endpoints públicos](#endpoints-públicos)
  - [Endpoints do administrador](#endpoints-do-administrador)
  - [Endpoints do colaborador](#endpoints-do-colaborador)
  - [Endpoints do morador](#endpoints-do-morador)
  - [Endpoints de Áreas Comuns e Reservas](#endpoints-de-áreas-comuns-e-reservas)
- [Executar o Projeto](#executar-o-projeto)
  - [Variáveis de ambiente](#variáveis-de-ambiente)
  - [Execução com Docker Compose](#execução-com-docker-compose)
  - [Inicialização e Credenciais Padrão (Bootstrap)](#inicialização-e-credenciais-padrão-bootstrap)

# Funcionalidades Seguidas

## Administrador
- Cadastrar blocos com identificação, quantidade de andares e apartamentos por andar.

- Gerar automaticamente as unidades com padrão de identificação por bloco, andar e apartamento.

- Cadastrar moradores.

- Vincular moradores a uma ou mais unidades.

- Cadastrar Colaborador.

- Cadastrar tipos de chamados com título e prazo máximo de resolução (SLA).

- Vincular colaborador a um ou mais tipos de chamados.

- Cadastrar os status possíveis dos chamados.

- Definir um status como padrão.
  - Iniciar o Chamado com esse Status.
  - Sistema ja inicia com um Status padrão mas pode ser modificado.

- Os status reservados `Solicitado`, `Atrasado` e `Finalizado` nao podem ser editados na tela administrativa.

- Deletar Usuarios (Soft Delete).

- Visualizar chamados dentro do seu escopo.

- Filtrar chamados por status, morador e data de abertura.

- Alterar status dos chamados.

- Finalizar chamados.

- Comentar em chamados dentro do seu escopo.

- Anexar arquivo no comentário dentro do seu escopo.

- Visualisar é baixar anexos no chamado

- Baixar anexos dos comentários dentro do seu escopo.

- Cadastrar e manter áreas comuns (nome, descrição, horário e dias de funcionamento), ativando ou desativando cada uma.

- Consultar todas as reservas do condomínio (de qualquer morador), com filtro por status, área e data.

- Aprovar ou negar (com motivo obrigatório) solicitações de reserva.

- Cancelar qualquer reserva ainda não iniciada, de qualquer morador.

## Colaborador
- Visualizar chamados dentro do seu escopo.

- Filtrar chamados por status, tipo, unidade e data de abertura.

- Alterar status dos chamados até a finalização.

- Finalizar chamados.

- Comentar em chamados dentro do seu escopo.

- Anexar arquivo no comentário dentro do seu escopo.

- Visualisar é baixar anexos no chamado

- Baixar anexos dos comentários dentro do seu escopo.


## Morador
- Estar vinculado a uma ou mais unidades por um administrador.

- Selecionar uma de suas unidades para abrir chamado.

- Abrir chamado informando tipo, descrição e anexos.

- Visualizar chamados das suas unidades.

- Filtrar meus chamados por status, unidade, tipo e data de abertura.

- Comentar apenas nos próprios chamados e nos chamados das unidades às quais está vinculado.

- Anexar arquivo na abertura do chamado.

- Anexar arquivo em comentários do chamado.

- Reabrir chamado finalizado.

- Consultar a disponibilidade de uma área comum para uma data e um intervalo de horário.

- Solicitar a reserva de uma área comum (nasce com status `SOLICITADA`).

- Acompanhar apenas as próprias reservas (lista com filtro por status, área e data, e detalhe com histórico de eventos).

- Cancelar a própria reserva, enquanto ela ainda não tiver começado.

## Chamado

- Data de início definida no momento em que o chamado for iniciado.
- Data de finalização definida no momento em que o chamado for concluído.
- Se algum chamado que esteja com status diferente de finalisado estiver : horario_atual > horario_inicio + SLA. Será marcado como atrasado.
  - Essa funcionalidade foi implementada via scheduler para evitar custo a cada acesso.

### Regras gerais do sistema
- O sistema possui blocos, andares e unidades.
- Toda a unidade deve ser criada automaticamente a partir da configuração do bloco.
- Todo chamado deve iniciar com um status padrão.
- Apenas administradores e colaboradores podem alterar o status.
- A data de finalização deve ser registrada quando o chamado for concluído.
- Comentários compõem o histórico de interações do chamado.
- Chamados finalizados se tornam imutáveis para comentário, anexo e alteração de status.
- Ao reabrir(Morador) um chamado finalizado, a data de finalização é removida.
- Na reabertura pelo morador, o status volta para `Solicitado` ou `Atrasado`, conforme a comparação entre horário atual e `data_abertura + SLA`.
- Campos textuais persistidos possuem limite de `255` caracteres com validação na interface, na aplicação e no banco.
- Anexos do chamado e de comentários possuem limite máximo de `5 MB`.

## Área Comum e Reserva

Funcionalidade independente da autorização de visitantes: uma reserva existe sem depender de nenhum outro fluxo do sistema, e não altera o funcionamento existente dos chamados.

### Ciclo de vida da reserva

`SOLICITADA` → `APROVADA` ou `NEGADA` (decisão do administrador) → `CANCELADA` (morador dono ou administrador, antes do início). `NEGADA` e `CANCELADA` são estados terminais: nunca voltam a `SOLICITADA` nem podem ser aprovados depois.

### Regras gerais

- Somente o administrador mantém áreas comuns; área retirada ou desativada para de aceitar novas solicitações, mas reservas já existentes seguem o ciclo normal (não são apagadas).
- Toda solicitação exige área, data e horário de início/fim; o horário de fim deve ser posterior ao de início, e o horário inicial precisa ser futuro em relação ao momento da solicitação.
- A disponibilidade de uma área considera ocupados apenas os intervalos de reservas `APROVADA`; uma `SOLICITADA` é pendente e não garante o horário.
- Duas reservas da mesma área conflitam quando os intervalos se sobrepõem; áreas diferentes nunca conflitam entre si.
- Uma reserva só é aprovada se, no momento da decisão, não conflitar com outra `APROVADA` da mesma área — inclusive quando duas aprovações são tentadas ao mesmo tempo (ver [Tratamento de concorrência](./ESPECIFICACAO.md)).
- Negar uma solicitação exige um motivo não vazio, que fica registrado no histórico.
- O morador só acompanha as próprias reservas; o administrador acompanha as de todos.
- Cancelamento (pelo morador dono ou pelo administrador) só é permitido enquanto a reserva estiver `SOLICITADA` ou `APROVADA` e o horário inicial ainda não tiver começado.
- Toda mudança de estado (solicitação, aprovação, negação, cancelamento) gera um evento imutável de histórico/auditoria, com autor, data e (quando houver) motivo.

As 15 regras de negócio detalhadas (RN-01-01 a RN-01-15), os critérios de aceite e as decisões técnicas (como o `LocalDateTime.now()` do servidor é a referência temporal única usada em toda a funcionalidade) estão documentados em [`ESPECIFICACAO.md`](./ESPECIFICACAO.md).

# Detalhamento do Sistema
## O sistema gira em torno de quatro frentes principais:

- Administradores mantêm a estrutura do condomínio, usuários, vínculos, tipos de chamado e status.
- Moradores acessam a suas unidades vinculadas, abrem chamados, anexam arquivos e registram comentários.
- Colaboradores acompanham os chamados acessíveis dentro do seu contexto e atualizam o andamento até a finalização.
- O banco sustenta tanto as entidades centrais quanto as regras de visibilidade, inclusive com consultas nativas para filtrar chamados por perfil.

## Principais funcionalidades

### Gestão da estrutura do condomínio

- Cadastro de blocos com identificação, quantidade de andares e apartamentos por andar.
- Geração e manutenção de unidades vinculadas ao bloco.
- Vínculo entre moradores e unidades, permitindo que um mesmo morador tenha acesso a mais de uma unidade.
- Vinculo entre colaborador e Tipo de Chamado, permitindo que um mesmo colaborador tenha acesso a mais de um tipo de chamado.

### Gestão de usuários e acesso

- Separação de perfis entre administrador, colaborador e morador.
- Autenticação e autorização com Spring Security e JWT.
- Controle de telas e operações por papel, refletido tanto nos controllers quanto nas validações de serviço.

### Operação de chamados

- Abertura de chamado por morador com unidade, tipo e descrição.
- Inclusão opcional de anexo já na abertura do chamado.
- Definição de status inicial padrão a partir de configuração persistida.
- Atualização de status e finalização por administrador ou colaborador.
- Morador consegue Reabrir chamados que já foram finalisados.
- Aplicação automática do status `Atrasado` por job agendado quando o SLA expira.
  - Via Scheduler para não sobrecarregar as chamadas ao front fasendo cálculos constantes desnecessários.
- Listagem paginada e filtrada conforme perfil de acesso.
  - Listagem usa Paginação para melhor desempenho.
  - Filtros para melhorar a pesquisa e experiencia do usuário.
- O projeto pode seguir o timezone configurado por ambiente, alinhando aplicação e banco para cálculos de SLA, atraso e finalização.

### Histórico e evidências

- Registro de comentários vinculados ao chamado com autoria.
- Inclusão e download de anexos.
  - Inclusão no chamado:Morador
  - Inclusão em comentário:Morador , Colaborador , Administrador
  - Download no chamado:Morador , Colaborador , Administrador
  - Download em comentário:Morador , Colaborador , Administrador
- Persistência de datas de abertura e finalização para rastreabilidade operacional.

### Validação de entrada

- Campos persistidos como nome, email, senha, identificação, título, descrição e mensagem são normalizados com `trim`, obrigatoriedade e limite de `255` caracteres.
- Os limites são aplicados em três níveis: formulário web, camada de serviço e banco de dados.
- Anexos validam nome do arquivo, `content_type`, tamanho informado, integridade do conteúdo e limite máximo de `5 MB`.
- Quando o upload excede o limite configurado, a interface retorna uma mensagem amigável ao usuário.


# Padrões de Projeto Utilizados

O projeto utiliza padrões de projeto e padrões arquiteturais de forma prática para separar entrada HTTP, casos de uso, regras de negócio, persistência, segurança e processamento assíncrono.

### MVC / Controller

- Os controllers em `infrastructure/controller/web` representam a camada Controller do MVC para renderizar páginas JSP.
- Classes como `AdminWebController`, `MoradorWebController`, `ColaboradorWebController`, `AuthWebController` e `HomeWebController` montam o modelo da tela, aplicam paginação/filtros e retornam views.
- Os controllers em `infrastructure/controller/api` concentram ações HTTP disparadas pelos formulários, como cadastro, atualização, exclusão, vínculos, comentários, anexos e alteração de status.
- Essa separação reduz o tamanho dos controllers de página e deixa mais claro o que renderiza tela e o que executa ação.

### Facade de Casos de Uso

- As interfaces em `application/UserCase` funcionam como fachadas de comportamento da aplicação.
- Elas expõem operações coesas por contexto, como administração, morador, colaborador, chamado, comentário, anexo, usuário, tipo de chamado e status.
- Os controllers dependem desses contratos em vez de depender diretamente de detalhes de persistência.

### Service Layer / Application Service

- Os services em `infrastructure/service` implementam os casos de uso e centralizam a regra de aplicação.
- Classes como `ChamadoService`, `UsuarioService`, `AdminService`, `MoradorService`, `ColaboradorService`, `ComentarioService`, `AnexoChamadoService` e `AnexoComentarioService` coordenam validações, autorização, persistência e regras de fluxo.
- Componentes em `service/support`, como validação de usuário autenticado, acesso ao chamado e validação de entrada, concentram lógicas auxiliares reutilizadas por mais de um serviço.

### Repository

- Os repositories em `infrastructure/repository` seguem o padrão Repository.
- Eles abstraem o acesso ao banco com Spring Data JPA, evitando espalhar consultas SQL, JPQL e consultas nativas pelas regras de negócio.
- Repositórios como `ChamadoRepository`, `UsuarioRepository`, `MoradorRepository`, `UnidadeRepository` e `TipoChamadoRepository` concentram operações de persistência e consultas específicas.

### Adapter

- O projeto utiliza Adapter na integração com o Spring Security.
- A classe `UserDetailsImpl` em `infrastructure/security/adapter` adapta a entidade `Usuario` para a interface `UserDetails` exigida pelo framework.
- Isso permite que a autenticação do Spring trabalhe com o modelo do sistema sem acoplar a entidade diretamente ao contrato externo.

### Herança e Polimorfismo por Papel

- A hierarquia `Usuario -> Administrador | Colaborador | Morador` representa especializações do usuário por papel.
- Cada subtipo define seu papel por meio de `getRole()`, influenciando autorização, navegação e escopo das operações.
- Esse uso é mais próximo de polimorfismo por especialização do que de Strategy clássico, porque não há troca dinâmica de algoritmo em tempo de execução.

### Support / Helper de Camada Web

- `WebControllerSupport` concentra operações auxiliares da camada web, como usuário autenticado, paginação, conversão de dados para a view, upload e resposta de download.
- Ele evita repetição entre controllers, mas permanece restrito à infraestrutura web.
- Os formulários em `controller/web/form` continuam simples e não dependem desse suporte.

### Template Method do Framework

- O Spring Boot e o Spring Security aplicam internamente o padrão Template Method em pontos como autenticação, filtros e ciclo de requisição.
- O projeto aproveita isso ao plugar implementações próprias, como `AuthenticationService` e `JwtAuthenticationFilter`.

### Scheduled Job

- `ChamadoAtrasoScheduler` representa um job agendado para processar chamados atrasados de forma assíncrona.
- Essa abordagem evita recalcular atraso em toda requisição web.
- O scheduler delega a regra para o serviço responsável, mantendo o processamento periódico separado da navegação das telas.

## Estrutura do Projeto e Princípios de Arquitetura

Este projeto foi organizado em camadas que separam domínio, regras de negócio, adaptação web, persistência e segurança.

## Árvore de diretórios

```text
gerenciador-chamados/                        -> raiz do projeto com código, build, docker e documentação
├── src/
│   ├── main/
│   │   ├── java/br/com/dunnastecnologia/chamados/
│   │   │   ├── application/                 -> contratos e modelos de apoio da camada de aplicação
│   │   │   │   ├── Security/                -> contexto do usuário autenticado usado nos casos de uso
│   │   │   │   ├── pagination/              -> abstrações de paginação usadas pelos casos de uso
│   │   │   │   └── UserCase/                -> interfaces de casos de uso por contexto: admin, morador, colaborador e chamados
│   │   │   ├── domain/                      -> núcleo do negócio e entidades persistidas
│   │   │   │   ├── model/                   -> entidades como Usuario, Bloco, Unidade, Chamado, Comentario, Anexos e Status
│   │   │   │   └── validation/              -> limites de tamanho e regras reutilizáveis de validação
│   │   │   └── infrastructure/              -> implementação técnica da aplicação
│   │   │       ├── config/                  -> configuração Spring, segurança, bootstrap, OpenAPI e schedulers
│   │   │       ├── controller/              -> entrada HTTP da aplicação
│   │   │       │   ├── api/                 -> ações HTTP/CRUD por entidade e papel, chamadas pelos formulários das telas
│   │   │       │   └── web/                 -> controllers que renderizam páginas JSP e suporte da camada web
│   │   │       │       └── form/            -> objetos de formulário usados no binding das requisições web
│   │   │       ├── exception/               -> exceções de regra de negócio e de acesso
│   │   │       ├── repository/              -> acesso a dados com Spring Data JPA
│   │   │       ├── security/                -> autenticação, JWT e adaptação ao Spring Security
│   │   │       │   └── adapter/             -> adaptação do usuário do domínio para contratos do Spring Security
│   │   │       └── service/                 -> implementação dos casos de uso e regras de negócio
│   │   │           └── support/             -> componentes auxiliares compartilhados pelos serviços
│   │   ├── resources/
│   │   │   ├── db/migration/                -> migrations versionadas do banco com Flyway
│   │   │   ├── static/                      -> recursos estáticos da interface, como CSS, JS e imagens
│   │   │   └── templates/                   -> diretório reservado de templates
│   │   └── webapp/WEB-INF/jsp/              -> views JSP protegidas, organizadas por papel, contexto funcional e fragments
│   │       ├── admin/                       -> telas administrativas do sistema
│   │       │   ├── blocos/                  -> listagem e detalhe da estrutura de blocos e unidades
│   │       │   ├── chamados/                -> fila administrativa, detalhe e ações sobre chamados
│   │       │   ├── escopo-colaborador/      -> gestão do escopo operacional por tipo de chamado
│   │       │   ├── status-chamado/          -> cadastro e administração dos status do fluxo
│   │       │   ├── tipos-chamado/           -> catálogo de tipos de chamado e SLA
│   │       │   ├── usuarios/                -> gestão cadastral de administradores, colaboradores e moradores
│   │       │   └── vinculos-morador/        -> vínculo entre moradores e unidades
│   │       ├── auth/                        -> tela de login e fluxos públicos de autenticação
│   │       ├── colaborador/                 -> telas operacionais do colaborador
│   │       │   └── chamados/                -> fila de atendimento e detalhe do chamado no escopo do colaborador
│   │       ├── fragments/                   -> head, topbar, sidebar, alerts e partes reutilizáveis das views
│   │       └── morador/                     -> telas do morador
│   │           └── chamados/                -> abertura, listagem, acompanhamento e reabertura de chamados
├── .env                                     -> variáveis locais usadas pelo docker compose e pela aplicação
├── docker-compose.yml                       -> orquestração local da aplicação e do PostgreSQL
├── Dockerfile                               -> imagem da aplicação Java
├── schema-drawio.sql                        -> SQL usado para importar o schema relacional no Draw.io
├── diagrama-relacional.drawio.svg           -> diagrama relacional exportado
├── pom.xml                                  -> dependências, plugins e build Maven
├── README.md                                -> documentação funcional, arquitetural e operacional
```

## Estrutura do projeto

### `src/main/java/br/com/dunnastecnologia/chamados/application`

- Concentra os contratos de caso de uso do sistema, como `AdminUseCases`, `MoradorUseCases`, `ColaboradorUseCases`, `ChamadoUseCase`, `UsuarioUseCase`, `ComentarioUseCase`, `TipoChamadoUseCase` e `StatusChamadoUseCase`.
- Também abriga modelos de apoio, como `AuthenticatedUser`, `PageRequest` e `PageResult`.
- A decisão de manter interfaces nessa camada deixa explícito o que o sistema faz, sem acoplar essa definição a controller, banco ou framework web.

### `src/main/java/br/com/dunnastecnologia/chamados/domain`

- Contém o núcleo do negócio persistido no sistema, representado por entidades como `Chamado`, `Bloco`, `Unidade`, `Usuario`, `Administrador`, `Colaborador`, `Morador`, `Comentario`, `AnexoChamado`, `AnexoComentario`, `TipoChamado` e `StatusChamado`.
- Essa camada expressa os conceitos centrais do problema do condomínio e o relacionamento entre eles.
- `validation` centraliza limites de tamanho usados pelas entidades, serviços e validações de entrada.
- A decisão de manter os modelos do domínio separados facilita a evolução da regra de negócio sem misturar detalhes de interface ou infraestrutura.

### `src/main/java/br/com/dunnastecnologia/chamados/infrastructure`

- Reúne a implementação concreta da aplicação.
- `controller/web` expõe os endpoints que renderizam páginas JSP para administrador, colaborador, morador, home e autenticação.
- `controller/api` concentra as ações HTTP/CRUD disparadas pelas telas, separadas por entidade e escopo, como blocos, usuários, chamados, vínculos, status e tipos de chamado.
- `controller/web/form` concentra objetos simples de entrada vindos dos formulários web.
- `service` implementa os contratos definidos na camada `application`.
- `service/support` reúne apoio transversal usado pelos serviços, evitando duplicação de regras auxiliares.
- `repository` encapsula o acesso ao banco com Spring Data JPA e consultas nativas.
- `config` centraliza configurações de segurança, bootstrap inicial, Swagger/OpenAPI, views JSP, agendamento e propriedades da aplicação.
- `security` implementa autenticação, JWT, filtro de autenticação e adaptação para Spring Security.
- `exception` concentra exceções de regra de negócio, acesso e tratamento de falhas da camada web.

### `src/main/resources`

- `db/migration` indica o uso de migrações versionadas para controlar a estrutura do banco.
- `static` concentra CSS, JavaScript e imagens da interface.
- `templates` está presente como diretório reservado, enquanto as telas renderizadas atualmente ficam em JSP.
- A separação entre código Java, migrations e recursos de interface deixa mais claro o que é backend, frontend e infraestrutura de banco.

### `src/main/webapp/WEB-INF/jsp`

- Organiza as telas por perfil de acesso, como `admin`, `colaborador`, `morador`, `auth` e `fragments`.
- A decisão de segmentar as views por papel acompanha a regra de negócio do sistema e reduz mistura entre fluxos de cada ator.
- `admin/blocos` concentra a estrutura física do condomínio.
- `admin/chamados` concentra supervisão e gestão dos chamados.
- `admin/escopo-colaborador` e `admin/vinculos-morador` apoiam as vinculações operacionais.
- `admin/status-chamado` e `admin/tipos-chamado` centralizam parâmetros do fluxo.
- `admin/usuarios` reúne manutenção cadastral dos perfis.
- `colaborador/chamados` concentra a fila operacional e o detalhe do atendimento.
- `morador/chamados` reúne abertura, listagem, histórico, anexos e reabertura.
- `auth` isola a entrada pública do sistema.
- `fragments` evita repetição de layout, navegação, mensagens e cabeçalhos entre as telas.

## Estrutura do sistema

## Clean Architecture no projeto

O projeto não segue uma implementação acadêmica pura de Clean Architecture, mas apresenta uma aproximação clara por camadas e por direção de responsabilidade.

### Domínio no centro

- As entidades em `domain/model` representam o núcleo do negócio e descrevem o problema do condomínio sem depender de controller web.
- A decisão de centralizar `Chamado`, `Unidade`, `Bloco`, `Usuario` e demais modelos no domínio preserva o vocabulário principal do sistema.

### Casos de uso como contrato da aplicação

- Interfaces como `ChamadoUseCase` e `ComentarioUseCase` definem o comportamento esperado do sistema em termos de ação de negócio.
- A decisão de expor contratos na camada `application` reduz acoplamento entre a regra de negócio e a tecnologia usada para executar essa regra.

### Infraestrutura como detalhe de implementação

- Controllers, repositories, JWT, JSP e configurações ficam em `infrastructure`.
- Isso reforça a ideia de que web, banco e segurança são mecanismos de entrega e persistência, não o centro da regra de negócio.
- A implementação concreta de um caso de uso, como `ChamadoService`, fica fora da definição abstrata do caso de uso.

### Dependência apontando para dentro

- Os controllers dependem dos contratos da camada `application`, não diretamente de implementações concretas de serviço.
- Os services implementam interfaces de caso de uso, o que reduz dependência da camada superior sobre detalhes concretos.
- A decisão melhora manutenção e troca de adaptadores, mesmo que o projeto ainda use repositories concretos da infraestrutura.

## Princípios SOLID aplicados ao projeto

### S - Single Responsibility Principle

- `MoradorWebController` cuida do fluxo web do morador, sem concentrar regra pesada de negócio.
- `ChamadoService` concentra a orquestração do caso de uso de chamados.
- `ChamadoRepository` fica responsável por persistência e consultas.
- `ChamadoAccessSupport` e `AuthenticatedUserValidator` encapsulam validações transversais de acesso.
- Essa separação reduz classes inchadas e torna mais previsível onde alterar cada comportamento.

### O - Open/Closed Principle

- A hierarquia `Usuario -> Administrador | Colaborador | Morador` permite estender comportamento por perfil sem reescrever a base comum.
- Novos casos de uso podem ser adicionados com novas interfaces e serviços sem quebrar contratos existentes.
- A decisão de separar formulários, controllers e serviços também ajuda a estender entradas e saídas sem contaminar o domínio.
- O ponto mais forte aqui é a extensibilidade por especialização e por novos serviços.

### L - Liskov Substitution Principle

- `Administrador`, `Colaborador` e `Morador` especializam `Usuario` preservando a identidade comum do usuário.
- `Comentario` aponta para `Usuario` como autor, o que faz sentido porque qualquer subtipo válido pode ocupar esse papel.
- A decisão reforça substituição segura no domínio sem exigir estruturas paralelas para cada tipo de autor.

### I - Interface Segregation Principle

- Os contratos de caso de uso estão separados por contexto: `ChamadoUseCase`, `TipoChamadoUseCase`, `ComentarioUseCase`, `UsuarioUseCase` e outros.
- Isso evita uma interface única e inflada com todas as operações do sistema.
- Cada controller ou serviço consumidor depende apenas das operações que realmente precisa.
- A separação melhora a clareza e reduz impacto de mudanças em contextos não relacionados.

### D - Dependency Inversion Principle

- A camada web depende de abstrações da aplicação, como `ChamadoUseCase`, em vez de depender diretamente de `ChamadoService`.
- `ChamadoService` implementa o contrato da aplicação e pode ser trocado por outra implementação sem alterar o controller.
- Essa decisão reduz acoplamento entre entrada web e regra de negócio.
- O princípio aparece de forma parcial, porque a persistência ainda é acessada por repositories concretos da infraestrutura.

## Decisões arquiteturais e relação com o projeto

### Separar por `application`, `domain` e `infrastructure`

- Essa divisão torna o projeto mais legível para manutenção.
- Também deixa claro o que é regra de negócio, o que é definição de caso de uso e o que é adaptação técnica.
- No contexto deste sistema, isso ajuda porque há muitos fluxos por papel de usuário e várias regras de autorização.

### Organizar controllers por perfil

- `AdminWebController`, `ColaboradorWebController` e `MoradorWebController` seguem os atores do problema.
- A decisão conversa diretamente com o enunciado, que define permissões diferentes para cada tipo de usuário.
- Isso reduz condicionais espalhadas e deixa cada jornada mais simples de entender.

### Centralizar regra de acesso em suporte e repositório

- A validação do usuário autenticado é concentrada em componentes de apoio.
- Parte da visibilidade do chamado foi empurrada para consultas de banco, como as funções usadas em `ChamadoRepository`.
- A decisão busca consistência de autorização e evita repetir a mesma regra em muitos pontos do código.

### Separar histórico textual e binário

- `Comentario` e `AnexoChamado` foram modelados em estruturas diferentes.
- Isso faz sentido porque mensagem e arquivo possuem ciclo de vida, formato e custo de armazenamento distintos.
- Para o sistema de chamados, essa separação simplifica a evolução e a consulta do histórico.

### Usar herança para tipos de usuário

- A tabela `usuarios` guarda a identidade comum, enquanto tabelas filhas materializam os papéis.
- Essa decisão reflete diretamente a regra de negócio, que possui três tipos de usuário com capacidades diferentes.
- Também evita duplicação de `nome`, `email` e `senha` em várias tabelas independentes.

# Diagrama Relacional

## Documentação dos Modelos

Este trecho descreve o modelo relacional implementado hoje no projeto a partir das entidades JPA em `src/main/java/br/com/dunnastecnologia/chamados/domain/model`.

![Diagrama relacional](./diagrama-relacional.drawio.svg)

## Visão geral do domínio

O domínio está organizado em quatro blocos principais:

- Estrutura do condomínio: `blocos` e `unidades`.
- Identidade e acesso: `administradores`, `colaboradores` e `moradores`.
- Operação do chamado: `chamados`, `status_chamado`, `tipos_chamado` e a tabela associativa `colaborador_tipo_chamado`.
- Histórico e evidências: `comentarios`, `anexos_chamado`, `anexos_comentario` e a tabela associativa `morador_unidade`.

## Decisões gerais de modelagem

- A hierarquia de usuários usa `@Inheritance(strategy = InheritanceType.JOINED)`, o que separa os dados comuns em `usuarios` e deixa os papéis específicos em tabelas filhas.
- O relacionamento entre morador e unidade foi modelado como muitos-para-muitos, coerente com o enunciado que permite um morador vinculado a uma ou mais unidades.
- O chamado guarda referências diretas para morador, unidade, tipo e status, reduzindo ambiguidade na auditoria de quem abriu, para qual unidade e em que estado está.
- O histórico foi separado entre comentários e anexos para manter responsabilidade clara: texto de interação em uma tabela e binário em outra.
- Como comentários também podem receber arquivo, o modelo distingue anexo do chamado e anexo do comentário, preservando a origem de cada evidência.
- O banco também reforça a entrada com `NOT NULL` e `CHECK CONSTRAINTS` para tamanho dos campos textuais e limite dos anexos.

## Tabelas

### `usuarios`

Campos principais: `id`, `nome`, `email`, `senha`, `ativo`.

- Centraliza atributos comuns de autenticação e identificação para todos os perfis.
- `email` único impede duplicidade de login entre administradores, colaboradores e moradores.
- `ativo` sustenta a estratégia de soft delete aplicada aos usuários sem quebrar integridade referencial.
- `nome`, `email` e `senha` possuem obrigatoriedade e limite de `255` caracteres.
- A decisão por uma tabela base evita repetição de colunas nas tabelas filhas.
- Como a estratégia é `JOINED`, consultas de um tipo específico preservam a especialização sem perder a identidade única do usuário.

### `administradores`

Campos principais: `id` herdado de `usuarios`.

- Existe como especialização explícita porque o sistema diferencia regras de acesso por papel.
- A tabela separada segue a estratégia de herança adotada no projeto.
- Mesmo sem campos extras hoje, a separação prepara o modelo para futuras permissões administrativas sem alterar `usuarios`.

### `colaboradores`

Campos principais: `id` herdado de `usuarios`.

- Representa o perfil operacional que atua apenas na gestão de chamados.
- A tabela própria mantém coerência com os demais perfis e com a regra de autorização por role.
- O escopo operacional do colaborador é materializado pela tabela associativa `colaborador_tipo_chamado`, que define por quais tipos ele é responsável.

### `moradores`

Campos principais: `id` herdado de `usuarios`.

- Separa o perfil que pode abrir chamados e comentar apenas no próprio contexto.
- A especialização permite aplicar regras de negócio específicas sem misturar com outros perfis.
- O vínculo com unidades foi extraído para uma tabela associativa, o que acomoda moradores com mais de uma unidade.

### `morador_unidade`

Campos principais: `morador_id`, `unidade_id`.

- Foi criada implicitamente pelo `@ManyToMany` entre `Morador` e `Unidade`.
- Resolve o requisito de um morador poder estar vinculado a uma ou mais unidades.
- Também deixa aberta a possibilidade de vários moradores compartilharem uma mesma unidade, caso isso seja necessário no negócio.

### `blocos`

Campos principais: `id`, `identificacao`, `quantidade_andares`, `apartamentos_por_andar`.

- Materializa a estrutura macro do condomínio pedida no enunciado.
- `identificacao` permite reconhecer cada prédio de forma direta.
- `identificacao` é obrigatória e limitada a `255` caracteres.
- `quantidade_andares` e `apartamentos_por_andar` guardam a configuração que permite gerar unidades automaticamente.

### `unidades`

Campos principais: `id`, `identificacao`, `andar`, `bloco_id`.

- Cada unidade pertence a um único bloco, por isso o relacionamento é muitos-para-um para `blocos`.
- `andar` foi persistido na própria unidade; assim o projeto evita uma tabela intermediária de andares.
- `identificacao` concentra o padrão legível da unidade, alinhado ao requisito de reconhecer bloco, andar e apartamento.
- `identificacao` também é protegida por restrição de tamanho no banco.

### `tipos_chamado`

Campos principais: `id`, `titulo`, `prazo_horas`.

- Atende ao requisito administrativo de cadastrar categorias de chamado.
- `prazo_horas` traduz o SLA máximo de resolução pedido no enunciado.
- Separar o tipo do chamado evita repetir título e prazo em cada ocorrência.
- `titulo` é obrigatório e limitado a `255` caracteres.

### `colaborador_tipo_chamado`

Campos principais: `colaborador_id`, `tipo_chamado_id`.

- Materializa o escopo do colaborador por tipo de chamado.
- Resolve o relacionamento muitos-para-muitos entre `colaboradores` e `tipos_chamado`.
- Permite que um colaborador atue em vários tipos e que um mesmo tipo tenha vários colaboradores responsáveis.

### `status_chamado`

Campos principais: `id`, `nome`, `inicial_padrao`.

- Atende ao requisito de cadastrar os estados possíveis de um chamado.
- `inicial_padrao` existe para identificar qual status deve ser aplicado na abertura.
- Essa decisão evita fixar o status inicial em código e deixa a regra configurável pelo administrador.
- `nome` é obrigatório e limitado a `255` caracteres.

### `chamados`

Campos principais: `id`, `descricao`, `data_abertura`, `data_finalizacao`, `morador_id`, `unidade_id`, `tipo_chamado_id`, `status_id`.

- É a tabela central do sistema porque conecta quem abriu, onde ocorreu, qual é o tipo e qual é o estado atual.
- `morador_id` registra o autor da abertura, importante para permissão e rastreabilidade.
- `unidade_id` garante que o chamado esteja vinculado a uma unidade específica do morador.
- `tipo_chamado_id` desacopla a classificação do incidente do registro operacional.
- `status_id` permite evolução do fluxo sem alterar a estrutura do chamado.
- `data_abertura` e `data_finalizacao` atendem ao requisito de controle temporal, inclusive a marcação quando o chamado for concluído.
- `descricao` é obrigatória e limitada a `255` caracteres.

### `comentarios`

Campos principais: `id`, `mensagem`, `data_criacao`, `autor_id`, `chamado_id`.

- Atende ao requisito de histórico de interações dentro do chamado.
- `autor_id` aponta para `usuarios`, o que permite comentário por morador, colaborador ou administrador sem duplicar estrutura.
- `chamado_id` garante que cada comentário esteja associado a um único fluxo de atendimento.
- `data_criacao` preserva a ordem cronológica das interações.
- `mensagem` é obrigatória e limitada a `255` caracteres.

### `anexos_comentario`

Campos principais: `id`, `comentario_id`, `nome_arquivo`, `content_type`, `tamanho_bytes`, `conteudo`.

- Registra arquivos vinculados a um comentário específico do histórico.
- Permite diferenciar evidências gerais do chamado de evidências anexadas em uma interação pontual.
- O relacionamento muitos-para-um com `comentarios` mantém a rastreabilidade de qual usuário anexou o arquivo dentro da conversa.
- `nome_arquivo` e `content_type` são limitados a `255` caracteres, e o conteúdo binário é validado até `5 MB`.

### `anexos_chamado`

Campos principais: `id`, `chamado_id`, `nome_arquivo`, `content_type`, `tamanho_bytes`, `conteudo`.

- Atende ao requisito de anexos no chamado.
- O relacionamento muitos-para-um com `chamados` permite vários arquivos por ocorrência.
- `content_type` e `tamanho_bytes` ajudam na validação e no tratamento de download.
- `conteudo` em `BYTEA` indica a decisão de armazenar o binário diretamente no banco, simplificando a consistência transacional.
- `nome_arquivo`, `content_type`, `tamanho_bytes` e `conteudo` possuem restrições de consistência e limite máximo de `5 MB`.

### `areas_comuns`

Campos principais: `id`, `nome`, `descricao`, `ativa`, `horario_abertura`, `horario_fechamento`, `duracao_maxima_minutos`.

- Mantida exclusivamente pelo administrador; `ativa = false` bloqueia novas solicitações sem apagar reservas existentes.
- `nome` é único (ignorando maiúscula/minúscula), garantido tanto na aplicação quanto por constraint no banco.
- `horario_abertura`/`horario_fechamento` e os dias de funcionamento (tabela `area_comum_dia_funcionamento`, N:N com `DayOfWeek`) são opcionais: uma área sem horário/dias cadastrados funciona em qualquer horário e dia.
- `duracao_maxima_minutos` é opcional: quando cadastrado, limita a duração de uma reserva nessa área (em minutos); sem valor, a área não tem limite de duração.

### `reservas`

Campos principais: `id`, `area_comum_id`, `morador_id`, `data`, `hora_inicio`, `hora_fim`, `status`, `motivo_negacao`, `administrador_decisao_id`, `data_solicitacao`, `data_decisao`, `data_cancelamento`, `versao`.

- `status` assume `SOLICITADA`, `APROVADA`, `NEGADA` ou `CANCELADA`, refletindo o ciclo de vida descrito em [Área Comum e Reserva](#área-comum-e-reserva).
- `morador_id` garante que o morador acompanhe somente as próprias reservas; `administrador_decisao_id` registra quem aprovou/negou.
- Uma constraint `EXCLUDE USING gist` garante, no próprio banco, que nunca existam duas reservas `APROVADA` da mesma área com horário sobreposto — proteção redundante ao lock pessimista aplicado na camada de serviço antes de decidir uma solicitação.
- `motivo_negacao` é obrigatório apenas quando `status = NEGADA`.

### `reserva_historico`

Campos principais: `id`, `reserva_id`, `status_anterior`, `status_novo`, `autor_tipo`, `autor_nome`, `observacao`, `data_evento`.

- Registro de auditoria: cada mudança de estado da reserva (solicitação, aprovação, negação, cancelamento) grava um evento imutável aqui, com quem decidiu e quando.
- `observacao` guarda o motivo da negação quando aplicável.

## Relacionamentos principais

- `usuarios` 1:1 `administradores`
- `usuarios` 1:1 `colaboradores`
- `usuarios` 1:1 `moradores`
- `moradores` N:N `unidades` via `morador_unidade`
- `blocos` 1:N `unidades`
- `colaboradores` N:N `tipos_chamado` via `colaborador_tipo_chamado`
- `moradores` 1:N `chamados`
- `unidades` 1:N `chamados`
- `tipos_chamado` 1:N `chamados`
- `status_chamado` 1:N `chamados`
- `usuarios` 1:N `comentarios`
- `chamados` 1:N `comentarios`
- `chamados` 1:N `anexos_chamado`
- `comentarios` 1:N `anexos_comentario`
- `administradores` 1:N `areas_comuns` (mantém)
- `areas_comuns` 1:N `area_comum_dia_funcionamento`
- `areas_comuns` 1:N `reservas`
- `moradores` 1:N `reservas`
- `administradores` 1:N `reservas` (via `administrador_decisao_id`)
- `reservas` 1:N `reserva_historico`

## Aderência ao enunciado

O modelo cobre bem a base do problema:

- Estrutura de condomínio com bloco e unidade.
- Perfis de administrador, colaborador e morador.
- Vínculo de morador com uma ou mais unidades.
- Escopo de colaborador por tipo de chamado.
- Abertura de chamados com tipo, descrição, anexos e status.
- Comentários com autoria e histórico.
- Anexos tanto no chamado quanto em comentários.
- Reabertura do chamado pelo morador com recalculo do status em função do SLA.

## Migrations do Banco

As migrations do projeto ficam em [`src/main/resources/db/migration`](./src/main/resources/db/migration).

O projeto usa Flyway para versionar a estrutura do banco e a evolução das funções SQL usadas pela aplicação.

### Como a pasta está organizada

- arquivos no formato `V{numero}__descricao.sql`
- cada arquivo representa uma etapa versionada da evolução do banco
- a execução acontece em ordem crescente de versão

### Responsabilidade das migrations atuais

- `V1`: estrutura inicial das tabelas e índices principais
- `V2`: regras de autorização do morador
- `V3`: regras administrativas e geração automática de unidades
- `V4`: regras operacionais do colaborador
- `V5`: funções de apoio usadas pelos repositories
- `V6`: consolidação das regras de visibilidade dos chamados
- `V7`: suporte ao status inicial padrão
- `V8`: estrutura de anexos dos chamados
- `V9`: filtro administrativo por morador
- `V10`: filtro administrativo por prefixo do nome do morador
- `V11`: filtros do colaborador por tipo e unidade
- `V12`: suporte a soft delete de usuários
- `V13`: escopo do colaborador por tipos de chamado
- `V14`: status `Atrasado` e suporte ao fluxo de SLA vencido
- `V15`: estrutura de anexos em comentários
- `V16`: limites de entrada para campos persistidos e restrições de tamanho dos anexos
- `V17`: filtro por data de abertura e ordenação dos chamados mais antigos no topo para admin e colaborador
- `V18`: filtros da listagem `Meus Chamados` do morador por status, unidade, tipo e data
- `V19`: tabela `areas_comuns`
- `V20`: tabela `reservas`, incluindo a constraint `EXCLUDE` que impede conflito de reservas `APROVADA` no próprio banco
- `V21`: funções de listagem e busca de reservas (apoio a autorização por morador/administrador)
- `V22`: horário de funcionamento opcional das áreas comuns
- `V23`: dias de funcionamento opcionais das áreas comuns (tabela `area_comum_dia_funcionamento`)
- `V24`: unicidade de nome de área comum (ignorando maiúscula/minúscula)
- `V25`: tabela `reserva_historico` (registro de auditoria das reservas)
- `V26`: duração máxima opcional (em minutos) de reserva por área comum

## Endpoints Web

Esta aplicação expõe sua interface principal pela pasta `src/main/java/br/com/dunnastecnologia/chamados/infrastructure/controller/web`.
Esses endpoints retornam páginas JSP e usam autenticação com sessão via Spring Security.

### Como acessar a interface

- A rota `GET /` funciona como ponto de entrada. Se o usuário não estiver autenticado, redireciona para `/login`. Se estiver autenticado, redireciona para o painel correspondente ao perfil.
- A tela de login está em `GET /login`.
- O envio do formulário de login acontece em `POST /login`, usando os campos `username` para o email e `password` para a senha.
- Após autenticar, o sistema redireciona automaticamente para:
  - `/admin` para administradores.
  - `/colaborador` para colaboradores.
  - `/morador` para moradores.

### Paginação web

As listagens da interface usam os parâmetros de query string `page` e `size`.

- `page` é baseado em zero.
  - `page=0` representa a primeira página.
  - `page=1` representa a segunda página.
- `size` define quantos registros serão exibidos por página.
- O tamanho padrão é `10` itens por página na maioria das listagens.
- O tamanho máximo aceito é `100`.
- Se `page` vier negativo ou `size` vier zerado ou negativo, o sistema volta para os valores padrão.

Exemplos:

- `/admin/blocos?page=0&size=10`
- `/admin/chamados?page=1&size=20`
- `/colaborador/chamados?page=0&size=15`
- `/morador/chamados?page=2&size=5`

Algumas telas usam paginação interna fixa apenas para montar cards e resumos do dashboard, sem expor esses parâmetros na URL.

### Endpoints públicos

#### `GET /`

- Acesso inicial da aplicação.
- Redireciona para `/login` quando não existe sessão autenticada.
- Redireciona para o painel do perfil quando o usuário já está autenticado.

#### `GET /login`

- Exibe a página de login da aplicação.
- Pode receber `?error=true` para falha de autenticação e `?logout=true` para logout concluído.

#### `POST /login`

- Processa o login via Spring Security.
- Deve ser enviado a partir do formulário web com `username`, `password` e token CSRF.
- Se autenticado com sucesso, o usuário é enviado para o painel do seu papel.

### Endpoints do administrador

Todos os endpoints abaixo exigem usuário com papel `ADMINISTRADOR`.

#### `GET /admin`

- Exibe o dashboard do administrador.
- Mostra totais de blocos, usuários, tipos de chamado, status e chamados.
- Também carrega uma lista resumida de chamados recentes.

#### `GET /admin/vinculos-morador`

- Exibe a tela de vínculo entre moradores e unidades.
- Aceita filtros `moradorId`, `blocoId`, `moradorEmail`, `semUnidadeEmail`, `semUnidadePage` e `semUnidadeSize`.
- É usado para administrar o relacionamento entre morador e unidade.

#### `GET /admin/escopo-colaborador`

- Exibe a tela de definição de escopo operacional do colaborador.
- Aceita `colaboradorId` para abrir um colaborador específico e `colaboradorEmail` para filtrar por email.
- Permite visualizar e preparar a vinculação de tipos de chamado ao colaborador.

#### `GET /admin/blocos`

- Lista os blocos cadastrados.
- Usa paginação por `page` e `size`.
- Serve para consulta da estrutura física do condomínio.

#### `POST /admin/blocos`

- Cadastra um novo bloco.
- Recebe os dados do formulário de bloco.
- Ao cadastrar, a aplicação executa a criação da estrutura do bloco e das unidades derivadas.
- Valida a identificação com obrigatoriedade e limite de `255` caracteres.

#### `GET /admin/blocos/{blocoId}`

- Exibe os detalhes de um bloco específico.
- Lista as unidades do bloco com paginação por `page` e `size`.
- É a tela usada para consultar a composição de apartamentos gerada para o bloco.

#### `GET /admin/usuarios`

- Lista todos os usuários do sistema.
- Usa paginação por `page` e `size`.
- Permite consultar administradores, colaboradores e moradores em uma única visão.

#### `POST /admin/usuarios`

- Cadastra um novo usuário.
- O tipo do usuário define se ele será administrador, colaborador ou morador.
- Valida nome, email e senha com obrigatoriedade, `trim` e limite de `255` caracteres.

#### `GET /admin/usuarios/{usuarioId}`

- Exibe o detalhe de um usuário.
- Para moradores, pode receber `blocoId` para mostrar as unidades do bloco e facilitar vínculo ou desvínculo.
- Para colaboradores, mostra os tipos de chamado já vinculados.

#### `POST /admin/usuarios/{usuarioId}`

- Atualiza os dados cadastrais do usuário.
- Mantém o papel original do registro.

#### `POST /admin/usuarios/{usuarioId}/remover`

- Executa a remoção lógica do usuário.
- Na prática, a aplicação marca o usuário como inativo em vez de apagar fisicamente o registro.

#### `POST /admin/moradores/{moradorId}/unidades/{unidadeId}/vincular`

- Vincula diretamente uma unidade a um morador.
- Pode receber `blocoId` e `dashboard=true` para voltar ao contexto de origem após a operação.

#### `POST /admin/moradores/{moradorId}/unidades/vincular`

- Faz o mesmo vínculo de morador com unidade, mas usando dados enviados por formulário.
- Pode receber `dashboard=true` para retornar à tela de vínculos.

#### `POST /admin/moradores/{moradorId}/unidades/{unidadeId}/desvincular`

- Remove o vínculo entre morador e unidade.
- Também aceita `blocoId` e `dashboard=true` para redirecionamento contextual.

#### `POST /admin/colaboradores/{colaboradorId}/tipos-chamado`

- Vincula um tipo de chamado ao escopo do colaborador.
- Pode receber `dashboard=true` para retornar à tela de escopo.

#### `POST /admin/colaboradores/{colaboradorId}/tipos-chamado/{tipoChamadoId}/remover`

- Remove um tipo de chamado do escopo do colaborador.
- Pode receber `dashboard=true` para retornar à tela de escopo.

#### `GET /admin/tipos-chamado`

- Lista os tipos de chamado cadastrados.
- Usa paginação por `page` e `size`.
- Pode receber `tipoId` para carregar um tipo específico em modo de edição.

#### `POST /admin/tipos-chamado`

- Cadastra um novo tipo de chamado.
- Define título e prazo em horas para SLA.
- Valida o título com obrigatoriedade e limite de `255` caracteres.

#### `POST /admin/tipos-chamado/{tipoId}`

- Atualiza um tipo de chamado existente.
- Redireciona de volta para a mesma tela com o registro em foco.

#### `GET /admin/status-chamado`

- Lista os status possíveis do fluxo.
- Usa paginação por `page` e `size`.
- Pode receber `statusId` para carregar um status em modo de edição.
- Os status reservados `Solicitado`, `Atrasado` e `Finalizado` aparecem como bloqueados para edição na interface.

#### `POST /admin/status-chamado`

- Cadastra um novo status de chamado.
- Valida o nome com obrigatoriedade e limite de `255` caracteres.

#### `POST /admin/status-chamado/{statusId}`

- Atualiza o nome de um status existente.
- Não permite alterar os status reservados `Solicitado`, `Atrasado` e `Finalizado`, mesmo por requisição direta.

#### `POST /admin/status-chamado/{statusId}/inicial-padrao`

- Define qual status será usado como padrão na abertura de novos chamados.

#### `GET /admin/chamados`

- Lista os chamados visíveis ao administrador.
- Usa paginação por `page` e `size`.
- Aceita filtros `statusId`, `moradorNome` e `dataAbertura`.
- Mantém os chamados mais antigos no topo da lista.
- É a tela central de acompanhamento administrativo dos atendimentos.

#### `GET /admin/chamados/{chamadoId}`

- Exibe os detalhes de um chamado.
- Mostra dados do chamado, status disponíveis, comentários e anexos.
- Comentários e anexos são carregados em lotes internos de até `100` itens.
- Se o chamado já estiver finalizado, a tela mantém visualização, mas bloqueia novas mutações.

#### `POST /admin/chamados/{chamadoId}/status`

- Atualiza o status do chamado.
- Usado pelo administrador para movimentar o fluxo de atendimento.
- Não permite alteração quando o chamado já está finalizado.

#### `POST /admin/chamados/{chamadoId}/finalizar`

- Finaliza o chamado.
- A finalização define a data de encerramento do registro.

#### `POST /admin/chamados/{chamadoId}/comentarios`

- Adiciona um comentário ao chamado.
- Aceita `multipart/form-data` com arquivo opcional no campo `arquivo`.
- O comentário passa a compor o histórico de interação.
- Não permite inclusão quando o chamado já está finalizado.
- Valida a mensagem com limite de `255` caracteres e o arquivo com tamanho máximo de `5 MB`.

#### `GET /admin/chamados/{chamadoId}/comentarios/{comentarioId}/anexos/{anexoId}`

- Baixa um anexo vinculado a um comentário do chamado.
- Retorna o arquivo com `Content-Disposition: attachment`.

#### `GET /admin/chamados/{chamadoId}/anexos/{anexoId}`

- Baixa um anexo do chamado.
- Retorna o arquivo com `Content-Disposition: attachment`.

#### `GET /admin/areas-comuns`

- Lista as áreas comuns cadastradas e permite abrir o formulário de cadastro/edição.

#### `POST /admin/areas-comuns`

- Cadastra uma nova área comum (nome, descrição, horário e dias de funcionamento opcionais).
- Recusa nome duplicado (ignorando maiúscula/minúscula).

#### `PATCH /admin/areas-comuns/{areaComumId}`

- Atualiza nome, descrição, horário e dias de uma área comum existente.

#### `PATCH /admin/areas-comuns/{areaComumId}/disponibilidade`

- Ativa ou desativa uma área comum. Área desativada para de aceitar novas solicitações, mas não afeta reservas já existentes.

#### `GET /admin/reservas`

- Lista as reservas de todos os moradores, com filtro por status, área comum e data, e paginação.

#### `PATCH /admin/reservas/{reservaId}/aprovar`

- Aprova uma solicitação, desde que não exista reserva `APROVADA` conflitante no momento da decisão.

#### `PATCH /admin/reservas/{reservaId}/negar`

- Nega uma solicitação; exige motivo não vazio, preservado no histórico.

#### `PATCH /admin/reservas/{reservaId}/cancelar`

- Cancela qualquer reserva (`SOLICITADA` ou `APROVADA`) ainda não iniciada.

### Endpoints do colaborador

Todos os endpoints abaixo exigem usuário com papel `COLABORADOR`.

#### `GET /colaborador`

- Exibe o dashboard do colaborador.
- Mostra chamados abertos dentro do escopo do colaborador e o total correspondente.

#### `GET /colaborador/chamados`

- Lista os chamados disponíveis no escopo do colaborador.
- Usa paginação por `page` e `size`.
- Aceita filtros `statusId`, `tipoChamadoId`, `unidade` e `dataAbertura`.
- Mantém os chamados mais antigos no topo da fila operacional.
- É a principal tela operacional do colaborador.

#### `GET /colaborador/chamados/{chamadoId}`

- Exibe o detalhe de um chamado dentro do escopo permitido ao colaborador.
- Mostra status possíveis, comentários e anexos.
- Comentários e anexos são carregados em lotes internos de até `100` itens.
- Se o chamado já estiver finalizado, a tela mantém visualização, mas bloqueia novas mutações.

#### `POST /colaborador/chamados/{chamadoId}/status`

- Atualiza o status do chamado.
- Permite avançar o tratamento até a conclusão.
- Não permite alteração quando o chamado já está finalizado.

#### `POST /colaborador/chamados/{chamadoId}/finalizar`

- Finaliza o chamado.
- Após isso, o registro sai da lista operacional de chamados abertos.

#### `POST /colaborador/chamados/{chamadoId}/comentarios`

- Adiciona comentário ao chamado dentro do escopo do colaborador.
- Aceita `multipart/form-data` com arquivo opcional no campo `arquivo`.
- Não permite inclusão quando o chamado já está finalizado.
- Valida a mensagem com limite de `255` caracteres e o arquivo com tamanho máximo de `5 MB`.

#### `GET /colaborador/chamados/{chamadoId}/comentarios/{comentarioId}/anexos/{anexoId}`

- Faz download de um anexo vinculado a um comentário do chamado.

#### `GET /colaborador/chamados/{chamadoId}/anexos/{anexoId}`

- Faz download de um anexo vinculado ao chamado.

### Endpoints do morador

Todos os endpoints abaixo exigem usuário com papel `MORADOR`.

#### `GET /morador`

- Exibe o dashboard do morador.
- Mostra um resumo das unidades vinculadas e dos chamados já abertos.

#### `GET /morador/chamados`

- Lista os chamados do próprio morador.
- Usa paginação por `page` e `size`.
- Aceita filtros `statusId`, `unidadeId`, `tipoChamadoId` e `dataAbertura`.
- É a tela de acompanhamento pessoal dos chamados abertos e concluídos.

#### `GET /morador/chamados/novo`

- Exibe o formulário para abertura de chamado.
- Carrega as unidades do morador e os tipos de chamado disponíveis.

#### `POST /morador/chamados`

- Abre um novo chamado.
- Recebe a unidade, o tipo de chamado e a descrição.
- Aceita `multipart/form-data` com arquivo opcional no campo `arquivo`.
- Após criar, redireciona para a tela de detalhe do chamado aberto.
- Valida a descrição com obrigatoriedade e limite de `255` caracteres, além do anexo opcional com tamanho máximo de `5 MB`.

#### `GET /morador/chamados/{chamadoId}`

- Exibe o detalhe de um chamado do próprio morador.
- Mostra comentários e anexos ligados ao atendimento.
- Comentários e anexos são carregados em lotes internos de até `100` itens.
- Quando o chamado estiver finalizado, a tela oculta os formulários de comentário e anexo e exibe a ação de reabertura.

#### `POST /morador/chamados/{chamadoId}/comentarios`

- Adiciona um comentário ao chamado do morador.
- Aceita `multipart/form-data` com arquivo opcional no campo `arquivo`.
- Não permite inclusão quando o chamado já está finalizado.
- Valida a mensagem com limite de `255` caracteres e o arquivo com tamanho máximo de `5 MB`.

#### `GET /morador/chamados/{chamadoId}/comentarios/{comentarioId}/anexos/{anexoId}`

- Baixa um anexo vinculado a um comentário do chamado.

#### `POST /morador/chamados/{chamadoId}/anexos`

- Envia um novo anexo para o chamado.
- O arquivo é enviado como `multipart/form-data` no campo `arquivo`.
- Não permite inclusão quando o chamado já está finalizado.
- O upload respeita o limite máximo de `5 MB`.

#### `POST /morador/chamados/{chamadoId}/reabrir`

- Reabre um chamado finalizado acessível ao morador.
- Remove a `dataFinalizacao`.
- Reaplica o status como `Solicitado` ou `Atrasado`, conforme a regra `agora > data_abertura + SLA`.

#### `GET /morador/chamados/{chamadoId}/anexos/{anexoId}`

- Baixa um anexo de um chamado ao qual o morador tem acesso.

### Endpoints de Áreas Comuns e Reservas

Todos os endpoints abaixo exigem usuário com papel `MORADOR` (a menos que indicado). O colaborador não participa deste fluxo e não tem acesso a nenhuma destas rotas.

#### `GET /morador/areas-comuns`

- Lista as áreas comuns disponíveis.
- Se área, data e intervalo de horário forem informados na query string, consulta a disponibilidade (considera ocupados apenas os intervalos com reserva `APROVADA`).

#### `POST /morador/reservas`

- Solicita a reserva de uma área comum para uma data e intervalo; nasce com status `SOLICITADA`.

#### `GET /morador/reservas`

- Lista somente as reservas do próprio morador, com filtro por status, área e data, e paginação.

#### `GET /morador/reservas/{reservaId}`

- Exibe o detalhe de uma reserva do próprio morador, incluindo o histórico de eventos (auditoria).
- Reserva de outro morador responde como não encontrada, sem expor o registro.

#### `PATCH /morador/reservas/{reservaId}/cancelar`

- Cancela a própria reserva, desde que ainda esteja `SOLICITADA` ou `APROVADA` e o horário inicial não tenha começado.

# Executar o Projeto

## Variáveis de ambiente

O arquivo `.env` **não é obrigatório**: toda variável usada pelo `docker-compose.yml` já tem um valor padrão de desenvolvimento embutido (sintaxe `${VAR:-padrao}`), então `docker compose up` sobe o projeto sozinho, do zero, sem nenhum passo antes. Isso é proposital — credenciais nunca devem ir para o repositório, e por isso não existe (e não deve existir) um `.env` versionado.

Se quiser customizar algum valor (outra senha de banco, outro segredo de token, outro timezone), crie um `.env` na raiz a partir do [`.env.example`](./.env.example) e ajuste o que quiser; o Docker Compose lê esse arquivo automaticamente e ele sobrescreve os padrões.

```env
DB_USER=postgres
DB_PASSWORD=postgres
DB_NAME=gerenciador_chamados
APP_TIMEZONE=America/Sao_Paulo
TOKEN=uma-chave-secreta-de-sua-escolha
APP_BOOTSTRAP_ADMIN_NOME=Administrador
APP_BOOTSTRAP_ADMIN_EMAIL=admin@condominio.local
APP_BOOTSTRAP_ADMIN_SENHA=admin123
APP_CHAMADO_ATRASO_SCHEDULER_INITIAL_DELAY_MS=30000
APP_CHAMADO_ATRASO_SCHEDULER_FIXED_DELAY_MS=60000
```

### Finalidade de cada variável

- `DB_USER`: usuário do PostgreSQL usado pelo container do banco e pela aplicação.
- `DB_PASSWORD`: senha do PostgreSQL usada no banco e na conexão da aplicação.
- `DB_NAME`: nome do banco de dados que será criado no container.
- `APP_TIMEZONE`: timezone usado pela aplicação Java e pelo PostgreSQL para cálculos de data e hora, como SLA, atraso e finalização.
- `TOKEN`: segredo usado pela camada de segurança para geração e validação de autenticação baseada em token.
- `APP_BOOTSTRAP_ADMIN_NOME`: nome do administrador padrão criado automaticamente.
- `APP_BOOTSTRAP_ADMIN_EMAIL`: email usado no login do administrador padrão.
- `APP_BOOTSTRAP_ADMIN_SENHA`: senha inicial do administrador padrão.
- `APP_CHAMADO_ATRASO_SCHEDULER_INITIAL_DELAY_MS`: tempo inicial, em milissegundos, antes da primeira execução do scheduler.
- `APP_CHAMADO_ATRASO_SCHEDULER_FIXED_DELAY_MS`: intervalo, em milissegundos, entre uma execução e outra do scheduler.

### Valores padrão do projeto

- O projeto mantém, por padrão, o administrador bootstrap habilitado com nome `Administrador`, email `admin@condominio.local` e senha `admin123`.
- O scheduler de atraso permanece habilitado com `initial delay` de `30000 ms` e `fixed delay` de `60000 ms`.
- O timezone padrão de execução está definido como `America/Sao_Paulo`, mas pode ser ajustado para acompanhar o fuso horário do ambiente.
- Todos esses valores (e o usuário/senha/nome do banco) já vêm definidos como padrão diretamente no `docker-compose.yml`; um `.env` só é necessário para sobrescrevê-los.

### Limites de upload

- A aplicação usa `spring.servlet.multipart.max-file-size=5MB`.
- A aplicação usa `spring.servlet.multipart.max-request-size=5MB`.
- Quando esse limite é ultrapassado na interface web, o usuário recebe a mensagem `O arquivo enviado excede o limite de 5 MB.`.

## Execução com Docker Compose

### Pré-requisitos

- Docker instalado.
- Docker Compose disponível no ambiente.

### Passos para subir o ambiente

Nenhum passo de configuração é necessário antes. Basta executar, na raiz do projeto:

```bash
docker compose up --build
```

(o `--build` só é necessário na primeira vez ou depois de alterar o código; em execuções seguintes, `docker compose up` sozinho é suficiente.)

### O que será iniciado

- Serviço `db`: container PostgreSQL 16 na porta `5432`.
- Serviço `app`: aplicação Java na porta `8080`.

### Como o `docker-compose.yml` se relaciona com o projeto

- O serviço `db` usa as variáveis `DB_USER`, `DB_PASSWORD` e `DB_NAME` para inicializar o PostgreSQL.
- O serviço `app` usa essas mesmas variáveis para montar a URL JDBC e credenciais da aplicação.
- O serviço `app` também recebe, via ambiente, as variáveis de bootstrap do administrador, do scheduler de atraso e de timezone.
- O serviço `db` também recebe o timezone configurado para manter coerência entre `LocalDateTime.now()` na aplicação e `now()` nas rotinas SQL.
- A aplicação só sobe depois de o banco estar saudável, por causa do `depends_on` com `healthcheck`.
- Cada variável tem um valor padrão embutido diretamente no `docker-compose.yml` (sintaxe `${VAR:-padrao}`); um `.env` opcional, se existir, sobrescreve esses padrões — mas nunca é obrigatório nem deve ser versionado.

### Comandos interativos úteis nos containers

Subir o ambiente:

```bash
docker compose up --build
```

Subir em background:

```bash
docker compose up -d --build
```

Ver containers em execução:

```bash
docker ps
```

Ver logs da aplicação:

```bash
docker  logs -f app_condominio
```

Ver logs do banco:

```bash
docker logs -f postgres_condominio
```

Entrar em shell no container da aplicação:

```bash
docker compose exec app bash
```

Entrar em shell no container do PostgreSQL:

```bash
docker compose exec db sh
```

Abrir terminal `psql` no banco:

```bash
docker compose exec db psql -U ${DB_USER} -d ${DB_NAME}
```

Listar tabelas no PostgreSQL:

```sql
\dt
```

Consultar histórico do Flyway:

```sql
SELECT installed_rank, version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

Verificar a página inicial via linha de comando:

```bash
curl -i http://localhost:8080/
```

Verificar a tela de login via linha de comando:

```bash
curl -i http://localhost:8080/login
```

Parar o ambiente:

```bash
docker compose down
```

Parar e remover volumes:

```bash
docker compose down -v
```

## Inicialização e Credenciais Padrão (Bootstrap)

### `src/main/java/br/com/dunnastecnologia/chamados/infrastructure/config/AdminBootstrapConfig.java`

### Credenciais de Acesso (Administrador)
Se o banco de dados estiver vazio, o sistema criará automaticamente um usuário administrador padrão. Você pode usar essas credenciais para fazer o primeiro login no painel:

- E-mail: admin@condominio.local

- Senha: admin123

(Nota: Estes valores podem ser facilmente customizados pelas variáveis `APP_BOOTSTRAP_ADMIN_*` no `.env` e no `docker-compose.yml`.)

### Status de Chamados Obrigatórios
A inicialização também garante a integridade do fluxo de trabalho do condomínio. O código verifica e cria automaticamente os status essenciais do sistema:

- Solicitado: É o status de entrada. O sistema é configurado para marcá-lo automaticamente como o Status Inicial Padrão, ou seja, todo novo chamado aberto por um morador cairá neste status.

- Finalizado: Garante que o status de encerramento do ciclo de vida de um chamado sempre exista no banco de dados.

- Atrasado: Garante que o sistema possa marcar automaticamente chamados vencidos quando o prazo do SLA for ultrapassado.

Esses três status são tratados como reservados pela aplicação e nao podem ser renomeados pela tela administrativa.

### Scheduler de atraso

- O agendamento que marca chamados vencidos como `Atrasado` também pode ser configurado por ambiente.
- As variáveis usadas são `APP_CHAMADO_ATRASO_SCHEDULER_ENABLED`, `APP_CHAMADO_ATRASO_SCHEDULER_INITIAL_DELAY_MS` e `APP_CHAMADO_ATRASO_SCHEDULER_FIXED_DELAY_MS`.
- Os valores padrão mantidos no projeto são `true`, `30000` e `60000`, respectivamente.

### Timezone da aplicação

- O projeto também pode ser configurado com `APP_TIMEZONE`.
- Essa configuração é repassada para a JVM e para o PostgreSQL no ambiente Docker.
- O objetivo é manter consistência entre horários de abertura, finalização, cálculo de SLA e marcação de atraso.

### Acessando a Aplicação

- Interface Web (Página de Login): http://localhost:8080/
- Swagger: http://localhost:8080/swagger-ui/index.html