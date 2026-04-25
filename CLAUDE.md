# AuditVault - Arquitetura e Decisões

## Visão Geral
AuditVault é um sistema de auditoria event-sourced para APIs REST.

## Stack Tecnológica
- **Linguagem**: Java 17
- **Framework**: Spring Boot 3.2
- **Banco de Dados**: PostgreSQL
- **Padrões Arquiteturais**: Clean Architecture, CQRS, Event Sourcing
- **Testes**: JUnit 5, Testcontainers

## Convenções de Código
- **Arquitetura Limpa**: Separação estrita entre `domain`, `application`, `infrastructure`, `presentation`.
- **TDD Mandatório**: Testes sempre devem ser escritos antes da implementação (Red-Green-Refactor).

## Architecture Decision Records (ADRs)

### ADR-001: Uso do PostgreSQL com JSONB para o Event Store
- **Contexto**: Precisamos armazenar eventos de auditoria com payloads de estrutura variável (dependendo da entidade auditada).
- **Decisão**: Utilizar PostgreSQL com uma coluna `JSONB` para o `payload` ao invés de adotar um banco de dados NoSQL (ex: MongoDB).
- **Justificativa**: Manter a infraestrutura simples para os usuários da biblioteca. A maioria das aplicações Spring Boot já utiliza um banco relacional. O suporte nativo do PostgreSQL a JSONB nos dá flexibilidade de schema (essencial para o Event Store) sem exigir componentes de infraestrutura adicionais.

### ADR-002: Separação estrita entre Entidades de Domínio e Entidades JPA
- **Contexto**: Na modelagem de domínio com Spring Data JPA, é comum usar anotações (`@Entity`, `@Table`) diretamente nas classes de domínio.
- **Decisão**: Adotar a separação estrita proposta pela Clean Architecture. O pacote `domain` conterá apenas POJOs puros (ou classes imutáveis com Lombok, mas sem dependências de infraestrutura). A camada `infrastructure` conterá as entidades JPA e adapters de repositório.
- **Justificativa**: Evitar acoplamento do modelo de domínio com detalhes de persistência, mantendo o core focado nas regras de negócio e imutabilidade do evento. Isto também facilita o encapsulamento dentro de uma biblioteca, sem vazar configurações de mapeamento de banco de dados para os projetos que a consumirem.

### ADR-003: Uso de ApplicationEventPublisher e @Async
- **Contexto**: A interceptação de métodos para salvar logs de auditoria não deve impactar o tempo de resposta das APIs.
- **Decisão**: Utilizar o `ApplicationEventPublisher` nativo do Spring e processar a gravação no banco de forma assíncrona usando `@Async`.
- **Justificativa**: Evita bloquear a thread principal da API auditada. Garante desacoplamento entre o interceptador AOP e a persistência no banco de dados.

### ADR-004: Inversão de dependência para obter o userId (UserContextResolver)
- **Contexto**: Precisamos identificar quem realizou a ação (userId), porém como somos uma biblioteca genérica, não sabemos como a aplicação cliente gerencia autenticação (Spring Security, JWT manual, sessão, etc).
- **Decisão**: Criar uma interface `UserContextResolver` que deve ser implementada pela aplicação cliente para prover o usuário atual.
- **Justificativa**: Mantém a biblioteca independente do framework de segurança utilizado pelo usuário final.

### ADR-005: Mascaramento obrigatório de PII no payload
- **Contexto**: Logs de auditoria são alvos visados. Salvar payloads em texto plano pode vazar dados como senhas, cartões de crédito ou CPF.
- **Decisão**: Ofuscar obrigatoriamente campos sensíveis conhecidos antes da publicação e persistência do evento.
- **Justificativa**: Protege informações sensíveis no banco de dados e obedece às melhores práticas de segurança (OWASP), marcando os eventos que sofreram ofuscação (`obfuscated = true`).

### ADR-006: Uso de Snapshots para Otimização de Consultas
- **Contexto**: No Event Sourcing puro, ler o estado atual exige um "replay" de todos os eventos. Para agregados com vida longa e muitos eventos, o custo de processamento (CPU e Memória) aumenta drasticamente.
- **Decisão**: Implementar a persistência periódica de `Snapshots` do estado condensado de um agregado após um certo threshold (limite de eventos).
- **Justificativa**: Troca uso intensivo de CPU no replay de eventos por um leve aumento no custo de Storage, entregando leituras quase em O(1) + O(K) onde K é o número residual de eventos desde o último snapshot.

### ADR-007: Isolamento da Reconstrução de Leitura (CQRS)
- **Contexto**: A lógica de gravação e a lógica de leitura possuem desafios distintos.
- **Decisão**: Isolar a responsabilidade de leitura e reconstrução de estado no `AuditStateRebuilderService`.
- **Justificativa**: Separação clara das operações de escrita (Command) das consultas (Query) seguindo o padrão CQRS, permitindo otimizar e escalar o modelo de leitura de forma independente da captação de auditoria.

### ADR-008: Paginação obrigatória na API REST
- **Contexto**: Uma tabela `audit_events` em produção pode acumular milhões de registros. Um `findAll()` sem paginação causaria um carregamento completo em memória, resultando em erros OOM (OutOfMemoryError) e degradação severa de performance.
- **Decisão**: Todos os endpoints de listagem da API de auditoria são obrigados a aceitar parâmetros `Pageable` do Spring Data e retornar `Page<T>`. Nenhum endpoint pode retornar coleções completas sem paginação.
- **Justificativa**: Proteção contra erros de memória, controle previsível de tempo de resposta e melhores práticas de design de API REST.

### ADR-009: Uso do Spring Batch para geração de relatórios PDF
- **Contexto**: Gerar um relatório PDF com todos os eventos de auditoria de um agregado é uma operação potencialmente longa. Expor isso em um endpoint HTTP síncrono causaria timeouts e má experiência ao cliente.
- **Decisão**: Usar o Spring Batch para processar a exportação em chunks (lotes de 100 eventos), em background, de forma assíncrona. O endpoint retorna imediatamente HTTP 202 (Accepted) com o `JobExecutionId` para tracking.
- **Justificativa**: Garante resiliência (suporte a restart em caso de falha), escalabilidade e ausência de timeout HTTP para operações de longa duração.

### ADR-010: Uso de Angular 17+ com Standalone Components e Signals
- **Contexto**: A UI de auditoria requer gerenciamento de estado assíncrono (carregamento, paginação, timelines) e componentes leves que não exijam uma arquitetura engessada de módulos (NgModules).
- **Decisão**: Adotar Angular 17+ utilizando exclusivamente Standalone Components e a nova reatividade baseada em *Signals*.
- **Justificativa**: Melhora consideravelmente a performance (menos checagem de zona suja/Zone.js overhead) e simplifica drasticamente o código (code-splitting natural), além de ser o caminho oficial do framework para o futuro.

### ADR-011: Implementação de visualização de Diff de JSON
- **Contexto**: Na interface, analisar mutações de eventos complexos apenas lendo texto bruto não atende a uma auditoria corporativa com boa UX.
- **Decisão**: Criar um componente de visualização dedicado (JsonDiffViewer) para formatar e destacar de forma estruturada os payloads JSON dos eventos na Timeline.
- **Justificativa**: Entrega o valor principal de um sistema de auditoria: facilidade em localizar e compreender a alteração de estado (o que mudou, como estava, como ficou).

### ADR-013: Real-Time Updates via Server-Sent Events (SSE)
- **Contexto**: O dashboard de auditoria precisa refletir os eventos de forma reativa e instantânea, sem a necessidade de refresh manual por parte do usuário.
- **Decisão**: Usar Server-Sent Events (SSE) no lugar de WebSockets.
- **Justificativa**: Logs de auditoria são dados intrinsecamente unidirecionais (Servidor -> Cliente). SSE usa o protocolo HTTP padrão, lida nativamente com reconexões no browser (EventSource) e passa mais facilmente por firewalls e proxies corporativos sem exigir upgrade de protocolo (101 Switching Protocols).

### ADR-014: Full-Text Search com Elasticsearch
- **Contexto**: Consultas estruturadas via SQL (CQRS) resolvem os relatórios de eventos por Agregado, mas pesquisar dentro do payload JSON de milhares de eventos no Postgres (`LIKE` ou operadores jsonb) não escala bem e prejudica a performance transacional do banco.
- **Decisão**: Adotar Elasticsearch para indexar assincronamente todos os eventos salvos, disponibilizando busca *Full-Text* altamente otimizada em todos os campos.
- **Justificativa**: Desacopla as buscas complexas textuais do banco principal e provê velocidade e escalabilidade ideais para a experiência do usuário, mantendo a responsabilidade transacional primária (Write Model) segura no PostgreSQL.

### ADR-015: Containerização com Docker e Orquestração Local com Docker Compose
- **Contexto**: O sistema agora exige múltiplos serviços (PostgreSQL, Elasticsearch, Backend Java e Frontend Nginx/Angular), o que torna a configuração local complexa e sujeita a divergências de ambiente ("funciona na minha máquina").
- **Decisão**: Usar Docker e Docker Compose como padrão absoluto para orquestração e execução do projeto de forma local ou on-premise simples.
- **Justificativa**: Garante a Developer Experience (DX) e a portabilidade máxima. Com um único comando (`docker-compose up -d`), todas as dependências sobem prontas e configuradas.

### ADR-016: Observabilidade SRE com Spring Boot Actuator e Prometheus
- **Contexto**: Para uso corporativo e deployment em produção, precisamos monitorar a saúde da API, uso de memória, latência e métricas do pool do banco de dados e do Elasticsearch.
- **Decisão**: Integrar `spring-boot-starter-actuator` em conjunto com `micrometer-registry-prometheus`.
- **Justificativa**: É o padrão de mercado (Cloud-Native) para exportar métricas. Permite acoplar o AuditVault facilmente em qualquer malha de monitoramento existente (Grafana/Prometheus) dos clientes da biblioteca.
