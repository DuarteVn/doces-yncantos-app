# CLAUDE.md — Doces Yncantos

Instruções para agentes de IA (Claude Code / Cursor / Copilot) e para devs humanos deste repositório.
**Leia este arquivo inteiro antes de escrever qualquer linha de código.**

---

## 1. Contexto

App mobile Android para a confeitaria **Doces Yncantos**. O problema real: a confeiteira (Yane) gerencia encomendas manualmente por WhatsApp e memória, sem visão confiável de "quem eu atendo em cada data".

O app resolve isso conduzindo **todo** pedido por um funil de confirmação — análise → orçamento (quando aplicável) → aprovação da cliente → sinal pago e confirmado — antes de o pedido entrar na **Agenda**. Só pedido realmente fechado ocupa data.

| Item                                 | Valor |
|--------------------------------------|---|
| Entrega / apresentação               | **14/12/2026** |
| Time                                 | 6 pessoas (Vinícius, Yane, Matheus, George, Leandro, Kamilly) |
| Product Owner / cliente              | Yane (é a confeiteira real) |
| Revisor de PR e Admin do Repositório | Vinícius |
| Plataforma                           | **Android apenas** (sem iOS, sem web) |
| Backlog                              | Trello "Projeto - Android Day" — 42+ histórias, 10 épicos |

### Fontes de verdade, em ordem de prioridade

1. **Trello** — histórias, critérios de aceite, regras de negócio. Vence em caso de conflito.
2. **Protótipo HTML** (não versionado neste repositório — peça o arquivo no grupo) — vence para **números** (preços, tamanhos, mínimos) e para fluxo de telas.
3. Este CLAUDE.md — vence para arquitetura, convenções e contrato de API.

Se Trello e protótipo divergirem, **não escolha sozinho**: registre na tabela da seção 14 e pergunte no grupo.

---

## 2. Stack

### Backend (`/backend`)

| Componente | Versão fixa |
|---|---|
| Java | **17** |
| Spring Boot | **3.3.4** |
| Spring Data JPA | (BOM do Boot) |
| Spring Security | (BOM do Boot) |
| MySQL | 8.x — driver `mysql-connector-j` |
| JWT | **jjwt 0.12.6** (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`) |
| OpenAPI | **springdoc-openapi-starter-webmvc-ui 2.6.0** |
| Migrations | Flyway |
| Build | Maven |

### App Android (`/android`)

| Componente | Escolha |
|---|---|
| Linguagem | **Java** (mesma do backend — menos troca de contexto para o time) |
| UI | Views + XML + **ViewBinding** (sem Compose) |
| minSdk / targetSdk | 26 / 34 |
| Navegação | Navigation Component (single-activity) |
| HTTP | Retrofit 2.11 + OkHttp (logging interceptor) + Gson |
| Async | `ExecutorService` + `LiveData` via ViewModel |
| Imagens | Glide |
| Design | Material 3 |

**Não troque versão de nada sem abrir issue.** Build quebrado por upgrade não combinado bloqueia 5 pessoas.

---

## 3. Estrutura do repositório

Hoje o repositório tem apenas o esqueleto gerado pelas IDEs: um CRUD de `Usuario` no backend e a `MainActivity` vazia no app. Nenhuma regra de negócio das seções 6–8 existe em código ainda.

```
/
├── CLAUDE.md                  ← este arquivo
├── README.md
├── .gitattributes
├── .gitignore                 ← ignora .idea/, *.iml, API/target/, APP/build/, APP/local.properties, .env
├── API/                       ← Spring Boot 3.3.4 · Maven · groupId com.montseubolo · artifactId api · v0.1.0
│   ├── pom.xml                ← web, data-jpa, security, mysql-connector-j, jjwt 0.12.6, springdoc 2.6.0
│   ├── mvnw · mvnw.cmd · .mvn/
│   └── src/main/
│       ├── java/com/montseubolo/api/
│       │   ├── MonteSeuBoloApiApplication.java
│       │   ├── controller/UsuarioController.java
│       │   ├── model/Usuario.java
│       │   ├── repository/UsuarioRepository.java
│       │   └── service/UsuarioService.java · UsuarioServiceImpl.java
│       └── resources/application.properties
└── APP/                       ← Android · Gradle · namespace com.montseubolo.app · v0.1.0
    ├── build.gradle · settings.gradle · gradle.properties · gradlew · gradle/
    └── app/
        ├── build.gradle       ← compileSdk 34 · minSdk 24 · appcompat, material, constraintlayout, retrofit 2.11, okhttp logging
        └── src/
            ├── main/
            │   ├── AndroidManifest.xml
            │   ├── java/com/montseubolo/app/MainActivity.java
            │   └── res/       ← layout/activity_main.xml, values/, drawable/, mipmap-anydpi-v26/, xml/
            ├── test/java/com/montseubolo/app/ExampleUnitTest.java
            └── androidTest/java/com/montseubolo/app/ExampleInstrumentedTest.java
```

---

## 4. Regras de ouro

1. **Preço nunca é calculado no app.** O Android envia a configuração escolhida; o backend devolve o valor. O app só exibe. Isso impede que cliente adultere valor e que app e API divirjam.
2. **Preço nunca é hardcoded no app.** Catálogo, tamanhos, sabores, acréscimos e mínimos vêm de `GET /catalogo`.
3. **Todo pedido passa por análise.** Não existe caminho que pule a etapa `EM_ANALISE`, seja qual for o produto ou valor.
4. **Autorização é validada no backend**, sempre. Esconder um botão no app não é controle de acesso.
5. **Nada de integração de pagamento.** Pix é chave copiável; cartão é link colado manualmente pela confeiteira. Confirmação de recebimento é manual.
6. **Sem push externo** (FCM/SMS/WhatsApp). Notificação é registro no banco, exibido no sininho dentro do app.
7. **Sem chat interno.** Contato é botão que abre `wa.me` com o número da confeitaria.
8. **Escopo é congelado.** Ideia nova = card novo no Trello, não código a mais no PR.
9. **Um PR = um card.** Ver seção 12.
10. **Não invente regra de negócio.** Se não está no Trello nem no protótipo, pergunte.

---

## 5. Backend — arquitetura e convenções

### Camadas

```
Controller  → valida entrada (@Valid), devolve DTO, nunca contém regra
   ↓
Service     → regra de negócio, transações (@Transactional), lança exceção de domínio
   ↓
Repository  → Spring Data, queries
   ↓
Entity      → JPA, sem anotação de validação de request, sem lógica de preço
```

**Entidade JPA nunca sai do controller.** Sempre DTO de response. Motivo: evita expor `senha`, evita `LazyInitializationException` na serialização e desacopla o contrato do banco.

### Nomenclatura

| Tipo | Padrão | Exemplo |
|---|---|---|
| Entidade | singular, PascalCase | `Pedido`, `ItemPedido` |
| Tabela | snake_case plural | `pedidos`, `itens_pedido` |
| Repository | `<Entidade>Repository` | `PedidoRepository` |
| Service | `<Entidade>Service` | `PedidoService` |
| Controller | `<Recurso>Controller` | `PedidoController` |
| Request DTO | `<Ação><Recurso>Request` | `CriarPedidoRequest` |
| Response DTO | `<Recurso>Response` | `PedidoResponse` |
| Endpoint | kebab-case plural | `/api/pedidos/{id}/confirmar-sinal` |

Código, pacotes e nomes de classe em português são aceitos (domínio é em português) — **mas seja consistente**: `Pedido`, não `Order`.

### Tratamento de erro

Toda exceção de domínio herda de `NegocioException` e é convertida por `@RestControllerAdvice` para este formato, sempre:

```json
{
  "timestamp": "2026-09-15T20:14:03Z",
  "status": 422,
  "erro": "PRAZO_MINIMO_NAO_ATENDIDO",
  "mensagem": "A data escolhida precisa ter ao menos 5 dias de antecedência.",
  "campo": "dataEntrega"
}
```

O app Android exibe `mensagem` direto para a usuária — então escreva a mensagem **em português, para a cliente da confeitaria, não para o dev**.

Códigos HTTP: `400` payload inválido · `401` sem/invalid token · `403` perfil sem permissão · `404` não existe · `409` conflito de estado (ex.: aprovar orçamento de pedido já cancelado) · `422` regra de negócio violada.

### Segurança / JWT

- `SecurityFilterChain` stateless, `csrf` desabilitado, `JwtAuthenticationFilter` antes de `UsernamePasswordAuthenticationFilter`.
- Senha com `BCryptPasswordEncoder`. **Nunca** logue, retorne ou compare senha em texto puro.
- Claims do token: `sub` = id do usuário, `perfil` = `CLIENTE|CONFEITEIRA|ADMIN`, `exp` = 24h.
- Secret vem de `application.properties` via variável de ambiente (`JWT_SECRET`). **Nada de secret commitado.**
- Autorização por método: `@PreAuthorize("hasRole('CONFEITEIRA')")` — e `ADMIN` enxerga tudo.
- Regra recorrente: um CLIENTE só acessa **os próprios** pedidos. Valide o dono no service, não só o role.

**Atenção à API da jjwt 0.12.x** — mudou em relação à 0.11 e a maioria dos exemplos na internet está desatualizada:

```java
// 0.12.x — correto
String token = Jwts.builder()
        .subject(usuario.getId().toString())
        .claim("perfil", usuario.getPerfil().name())
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + EXPIRACAO_MS))
        .signWith(getSigningKey())          // sem SignatureAlgorithm
        .compact();

Claims claims = Jwts.parser()               // não parserBuilder()
        .verifyWith(getSigningKey())        // não setSigningKey()
        .build()
        .parseSignedClaims(token)           // não parseClaimsJws()
        .getPayload();                      // não getBody()
```

### Banco / Flyway

- Toda mudança de schema é migration versionada: `V1__schema_inicial.sql`, `V2__seed_catalogo.sql`, ...
- **Nunca edite uma migration já mergeada na `main`** — crie a próxima.
- `spring.jpa.hibernate.ddl-auto=validate` em todo ambiente. `update` está proibido: ele mascara divergência de schema entre as 6 máquinas.
- Catálogo (tipos de bolo, tamanhos, massas, recheios, preços, mínimos) entra por **seed em migration**, não por `data.sql` nem hardcoded em enum.
- Valores monetários: `BigDecimal` (`DECIMAL(10,2)` no banco). **Nunca `double`/`float` para dinheiro.**

### OpenAPI

- Todo controller documentado com `@Tag`; todo endpoint com `@Operation(summary=...)` e `@ApiResponse` dos códigos relevantes.
- Esquema de segurança `bearerAuth` configurado em `OpenApiConfig` para o Swagger UI permitir testar autenticado.
- Swagger em `/swagger-ui.html`, liberado sem token (é a doc que o time usa para integrar o app).

---

## 6. Modelo de dados

Entidades centrais (campos essenciais — complete conforme a história):

- **Usuario** — `id, nome, email (único), telefone, senhaHash, perfil (enum), ativo`
  - Perfil e credencial são por conta. Contas de perfis diferentes **não compartilham dados** (mesma pessoa com dois perfis = dois registros).
  - Endereço **não** fica no usuário — é informado por pedido, no checkout.
- **Pedido** — `id, cliente (FK Usuario), status (enum), dataEntrega, horario, entrega (bool), endereco, formaPagamento (enum), valorItens, valorComplemento, valorTotal, sinal, observacaoOrcamento, linkPagamento, sinalMarcadoPelaCliente (bool), observacoes, criadoEm`
- **ItemPedido** — `id, pedido (FK), produtoTipo (enum), resumo (String), valor, decoracaoPersonalizada (bool), observacaoDecoracao, fotoReferenciaPath, configuracaoJson`
  - `resumo` é a descrição legível já montada (ex.: `"Chantininho tradicional M · Massa: Chocolate · Recheio: Brigadeiro preto + Maracujá"`) — é o que a confeiteira lê na Agenda.
  - `configuracaoJson` guarda a seleção estruturada, para reprocessamento e auditoria de preço.
- **Notificacao** — `id, destinatarioPerfil (enum), destinatarioUsuario (FK, nullable), mensagem, lida (bool), criadoEm`
- **FotoPortfolio** — `id, categoria (enum), caminhoArquivo, criadoEm`
- **Catálogo** (tabelas de referência, populadas por seed): `tipo_bolo`, `tamanho_bolo`, `preco_bolo`, `massa`, `recheio`, `produto`, `produto_opcao`.

### Upload de imagem

MVP: arquivo salvo em disco (`${app.upload.dir}`), banco guarda só o caminho; servido por endpoint próprio. Limite 5 MB, aceita `image/jpeg` e `image/png`. **Não** grave base64 no banco (o protótipo usa base64 só porque roda sem servidor).

---

## 7. Máquina de estados do pedido

É o coração do sistema. Implemente como enum `StatusPedido` + validação de transição no `PedidoService`. **Transição inválida lança exceção**, nunca é ignorada em silêncio.

```
                          ┌──────────────────────────────┐
  cliente envia           │                              │
     pedido        ┌──────▼──────┐                       │
  ─────────────────►  EM_ANALISE │                       │
                   └──────┬──────┘                       │
                          │                              │
       confeiteira conclui análise                       │
           ┌──────────────┴───────────────┐              │
           │                              │              │
  sem complemento              com complemento (> 0)     │
           │                              │              │
           │                   ┌──────────▼───────────┐  │
           │                   │ AGUARDANDO_APROVACAO │  │
           │                   └──────┬───────────┬───┘  │
           │                          │           │      │
           │              cliente aprova    cliente desiste
           │                          │           │      │
           └──────────┬───────────────┘           └──────► CANCELADO
                      │
             ┌────────▼────────┐
             │ AGUARDANDO_SINAL│ ◄─── cliente toca "Já paguei o sinal"
             └────────┬────────┘      (só marca flag, NÃO muda status)
                      │
      confeiteira confirma recebimento
                      │
             ┌────────▼────────┐
             │   CONFIRMADO    │  ← só aqui o pedido ENTRA NA AGENDA
             └────────┬────────┘
                      │
      confeiteira marca como entregue
       (só a partir da data do pedido)
                      │
             ┌────────▼────────┐
             │    ENTREGUE     │
             └─────────────────┘
```

**Fluxo de execução do "confirmar análise"** (o ponto mais confundido do sistema):

1. `POST /api/pedidos/{id}/analise` chega com `valorComplemento` (opcional), `observacao` (opcional) e `linkPagamento` (opcional).
2. Service carrega o pedido e valida que o status é `EM_ANALISE` — senão, `409`.
3. Se `formaPagamento == CARTAO` e veio `linkPagamento`, grava no pedido.
4. Se `valorComplemento` é nulo ou zero → status vai direto para `AGUARDANDO_SINAL`; notifica CLIENTE ("pedido analisado, pode pagar o sinal").
5. Se `valorComplemento > 0` → grava complemento + observação, recalcula `valorTotal = valorItens + valorComplemento` e `sinal`, status vai para `AGUARDANDO_APROVACAO`; notifica CLIENTE ("complemento de orçamento enviado").
6. Retorna `PedidoResponse` com o novo status.

Regras associadas:
- **Cancelado é terminal.** Pedido cancelado não exibe nenhum botão de ação para ninguém e some das filas ativas da Agenda.
- **Só `CONFIRMADO` e `ENTREGUE` aparecem no calendário.**
- `sinalMarcadoPelaCliente` é flag informativa para a confeiteira — não avança status sozinho.
- Botão "Marcar como entregue" só habilita quando `dataEntrega <= hoje`. Antes disso, o app mostra a partir de quando ficará disponível.

---

## 8. Regras de negócio e tabelas

Valores conferidos no protótipo. Servem de **seed**; a aplicação lê do banco.

### 8.1 Pedido — regras gerais

| Regra | Valor |
|---|---|
| Antecedência mínima | **5 dias** a partir de hoje |
| Horários | 07:00 às 18:00, de 30 em 30 min (23 slots) |
| Sinal | **50% do total**, arredondado **para cima** em centavos: `ceil(total * 0.5 * 100) / 100` |
| Saldo restante | Combinado fora do app, via WhatsApp |
| Taxa de entrega | **Não** calculada no app — entra como complemento de orçamento |
| Formas de pagamento | Pix ou Cartão (sem transferência bancária) |
| Chave Pix | `docesyncantos@gmail.com` |
| Cancelamento com menos de 3 dias | Sinal não é devolvido (aviso no checkout) |
| Data por pedido | **Uma só data por pedido.** Duas datas = dois pedidos |

### 8.2 Monte seu Bolo

**Tipos** (preço base por tamanho, R$):

| Tipo | Camadas massa | PP | P | M | G | GG |
|---|---|---|---|---|---|---|
| Chantininho tradicional | 3 | 100 | 160 | 210 | 270 | 320 |
| Chantininho baixo | 2 | 80 | 130 | 180 | 230 | 280 |
| Naked cake | 3 | 70 | 120 | 190 | 230 | 270 |

**Tamanhos:** PP 12 cm (6–8 fatias) · P 16 cm (10–12) · M 21 cm (20–25) · G 25 cm (30–35) · GG 30 cm (40–45).

**Massas:** Branca +0 · Chocolate +7 · Mista +0 · Red Velvet +8 · Maracujá +8.

**Recheios:** Brigadeiro branco, Beijinho, Brigadeiro preto → +0 · Limão +6 · Paçoca, Maracujá, Ninho → +8 · Caramelo salgado +10 · Nozes +12 · Doce de leite +15 · Geleia de morango +18 *(slot extra)* · Cream cheese +18.

**Regras de limite:**
- Limite de recheios normais = `camadas - 1` (tradicional/naked → 2; baixo → 1).
- **Modo restrito** quando tipo é *Chantininho baixo* **ou** tamanho é *PP*: máximo **1 recheio** e massa **Mista indisponível**.
- **Geleia de morango não conta no limite** — entra sempre como sabor extra.
- Ao mudar tipo/tamanho para o modo restrito, o excedente é podado automaticamente (mantém o 1º recheio não-geleia + geleia, se houver) e massa Mista vira Branca.
- Massa Mista na pré-visualização: camadas externas brancas, camada do meio chocolate.

**Ajuste por tamanho** — só sobre recheios com acréscimo > 0:

| Tamanho | Multiplicador do acréscimo |
|---|---|
| PP, P, M | 1x |
| G | **1,5x** |
| GG | **2x** |

**Fluxo do cálculo de preço** (`PrecificacaoService.calcularBolo`):

1. `precoBase = preco_bolo[tipo][tamanho]`
2. `extraMassa = massa.acrescimo` (sem ajuste por tamanho)
3. Para cada recheio: `if (acrescimo <= 0) 0 else acrescimo * multiplicador(tamanho)`
4. `valor = precoBase + extraMassa + soma(extrasRecheio)`
5. Decoração personalizada **não entra aqui** — vira complemento na análise.

### 8.3 Outras linhas

| Produto | Regras |
|---|---|
| **Bolo de Andar** | 2 andares, 3 camadas de massa + 2 de recheio **cada**. Aprox. 55 pessoas **R$520** · aprox. 80 pessoas **R$690**. Massa e recheios escolhidos por andar (até 2 + geleia extra). **Sem** ajuste G/GG nos recheios. Decorável. |
| **Bolo Fake** (cenográfico) | Sem sabor/recheio. P 15 cm **R$50** · M 20 cm **R$80** · G 25 cm **R$120** · GG 30 cm **R$150**. Decorável. |
| **Kit Festa** | Kit 01 — bolo PP + 10 brigadeiros + 4 cupcakes → **R$145**. Kit 02 — bolo P + 20 brigadeiros + 6 cupcakes → **R$245**. Bolo do kit é sempre 3 camadas / 2 recheios, **independente do tamanho**. Sem ajuste G/GG. Decorável. |
| **Caseirinhos** | Cobertura fixa por sabor. P/G: Queijo 35/65 · Chocolate 45/75 · Coco 50/80 · Nozes 60/90 · Maracujá 50/80 · Churros 55/85 · Tapioca 55/85 · Red Velvet 70/100. |
| **Brigadeiros** | Mín. **20 un/sabor**. Branco 1,60 · Preto 1,80 · Beijinho 1,80 · Casadinho 1,90 · Churros/Paçoca/Ferrero/Ninho c/ Nutella/Prestígio 2,20 · Nozes 2,70. |
| **Bem-Casados** | Mín. **25 un/recheio**. Brigadeiro preto 5,50 · Nozes 6,50 · Doce de leite 6,50. |
| **Brownies** | Mín. **9 un/recheio**. Chocolate 8,50 · Coco 8,50 · Doce de leite 9,50 · Nozes 9,50. |

O mínimo é **por sabor/recheio selecionado**, não pelo somatório: 10 de um + 15 de outro **não** satisfaz um mínimo de 25. Quantidade abaixo do mínimo → item destacado e ação bloqueada.

### 8.4 Decoração

- **Simples** (inclusa): espatulado liso, texturado ou wave; cor branca ou clara; topo com nome de até 10 letras. Texto informativo, sem opções clicáveis.
- **Personalizada**: papelaria 3D, flores naturais etc. → **valor a orçar**, nunca precificado no app.
- Foto de referência e observação são **opcionais e não excludentes** — pode mandar as duas, uma, ou nenhuma.
- A foto/observação aparece para a confeiteira na análise e para a cliente em "Meus Pedidos".

### 8.5 Perfis e navegação

| Perfil | Abas visíveis | Aba inicial |
|---|---|---|
| Cliente | Perfil, Cardápio, Fotos, Pedidos | Cardápio |
| Confeiteira | Perfil, Fotos, Agenda | Agenda |
| Admin | Perfil, Cardápio, Fotos, Pedidos, Agenda | Cardápio |

- Cadastro público cria **somente** perfil Cliente. Confeiteira e Admin não têm autocadastro.
- Só Confeiteira e Admin adicionam/removem foto do portfólio. Cliente nunca vê esses controles.
- Botão "Falar com a confeiteira no WhatsApp" (`wa.me`) aparece **só para Cliente**, na tela Minha conta.
- Ao sair da conta, o carrinho em andamento é descartado.
- Admin enxerga **todas** as notificações, de qualquer destinatário.

### 8.6 Galeria

8 categorias: Chantininho · Naked Cake · Bolo de Andar · Caseirinhos · Brigadeiros · Bem-Casados · Brownies · Kit Festa.
Categoria vazia para Cliente → mensagem "ainda não há fotos nesta categoria".

### 8.7 Notificações

| Evento | Destinatário |
|---|---|
| Novo pedido enviado | Confeiteira |
| Cliente aprovou orçamento | Confeiteira |
| Cliente sinalizou pagamento do sinal | Confeiteira |
| Cliente desistiu do pedido | Confeiteira |
| Pedido analisado | Cliente |
| Complemento de orçamento enviado | Cliente |
| Sinal confirmado | Cliente |
| Data/horário alterados pela confeiteira | Cliente |
| Pedido marcado como entregue | Cliente |

Abrir o sininho marca **todas** como lidas e zera o contador.

---

## 9. Contrato REST

Base: `/api`. Autenticação: `Authorization: Bearer <token>`.

```
POST   /auth/cadastro                     público   → cria CLIENTE
POST   /auth/login                        público   → { token, perfil, nome }
POST   /auth/recuperar-senha              público   → sempre 200 (não revela se e-mail existe)
POST   /auth/redefinir-senha              público   → token de redefinição expirável

GET    /perfil                            autenticado
PUT    /perfil                            autenticado

GET    /catalogo                          autenticado → catálogo completo com preços e regras
POST   /pedidos/simular                   CLIENTE    → valida config e devolve valor calculado

GET    /pedidos                           CLIENTE    → só os próprios
POST   /pedidos                           CLIENTE    → cria em EM_ANALISE
POST   /pedidos/{id}/aprovar-orcamento    CLIENTE
POST   /pedidos/{id}/desistir             CLIENTE
POST   /pedidos/{id}/marcar-sinal-pago    CLIENTE

GET    /agenda/analise                    CONFEITEIRA → fila EM_ANALISE
GET    /agenda/aguardando-orcamento       CONFEITEIRA
GET    /agenda/aguardando-sinal           CONFEITEIRA
GET    /agenda/calendario?mes=&ano=       CONFEITEIRA → contagem por dia
GET    /agenda/dia/{data}                 CONFEITEIRA → pedidos confirmados do dia
POST   /pedidos/{id}/analise              CONFEITEIRA → conclui análise / envia orçamento
PUT    /pedidos/{id}/data-horario         CONFEITEIRA
POST   /pedidos/{id}/confirmar-sinal      CONFEITEIRA
POST   /pedidos/{id}/marcar-entregue      CONFEITEIRA

GET    /fotos?categoria=                  autenticado
POST   /fotos                             CONFEITEIRA/ADMIN  (multipart)
DELETE /fotos/{id}                        CONFEITEIRA/ADMIN

GET    /notificacoes                      autenticado
POST   /notificacoes/marcar-lidas         autenticado
```

Datas em ISO-8601 (`2026-12-14`), horário `HH:mm`, valores como número decimal (`268.50`) — **formatação em `R$ 268,50` é responsabilidade do app**.

Ao criar/alterar endpoint, atualize esta seção no mesmo PR.

---

## 10. App Android

### Padrão por feature

Cada feature = `Fragment` + `ViewModel` + (opcional) `Adapter`, em `ui/<feature>/`.

- Fragment: só liga view ↔ ViewModel. Sem `Retrofit`, sem regra, sem `if` de negócio.
- ViewModel: expõe `LiveData<UiState<T>>` com `Loading | Success | Error`. Chama Repository.
- Repository: chama `ApiService`, converte erro HTTP em mensagem.
- `SessionManager`: guarda token e perfil em `EncryptedSharedPreferences`. Único lugar que lê/escreve token.
- `AuthInterceptor`: injeta o header `Authorization` em toda chamada. Ninguém monta esse header na mão.

### Regras de UI

- Toda tela trata os três estados: carregando, sucesso, erro. Lista vazia tem empty state com texto (não tela em branco).
- Mensagem de erro exibida é a `mensagem` do backend. Não invente texto genérico "Erro ao processar".
- Menu inferior montado a partir do perfil retornado no login — ver tabela 8.5.
- Nenhuma string hardcoded em layout: tudo em `strings.xml` (pt-BR).
- Valores monetários formatados com `NumberFormat.getCurrencyInstance(new Locale("pt","BR"))`.

---

## 11. Testes e Definition of Done

Mínimo por PR:

- **Backend**: teste unitário de service para toda regra com ramificação — precificação (limites de recheio, modo restrito, ajuste G/GG, mínimos por unidade), transição de status, permissão por perfil. `MockMvc` para o endpoint principal do card.
- **Android**: teste manual roteirizado nos 3 perfis quando a feature cruza permissões.

Um card só vai para **Quality** quando:

- [ ] Todos os critérios de aceite do card passam, um a um
- [ ] Regras de negócio do card implementadas **no backend**, não só no app
- [ ] Testes novos passando e suíte inteira verde (`mvn test`)
- [ ] Endpoints novos documentados no Swagger e na seção 9 deste arquivo
- [ ] Nenhum secret, IP, senha ou token no diff
- [ ] Sem `System.out.println`, sem `TODO` órfão, sem código comentado
- [ ] PR descreve o que testar e o card está linkado

---

## 12. Git

**Branch protegida: `main`.** Ninguém commita direto. Merge só via Pull Request aprovado por **Vinícius**.

```
main                    ← sempre estável, sempre roda
 └── feat/<epico>-<card-curto>
```

Nomes de branch: `feat/auth-realizar-login`, `feat/bolo-selecionar-recheio`, `fix/carrinho-total-nao-recalcula`.

Commits em português, imperativo, um assunto por commit:

```
feat(pedido): calcular sinal de 50% arredondado para cima
fix(bolo): impedir massa mista em tamanho PP
docs(api): documentar POST /pedidos/analise
test(precificacao): cobrir ajuste de recheio em GG
```

Prefixos: `feat` · `fix` · `refactor` · `test` · `docs` · `chore`.

Antes de abrir PR: `git pull --rebase origin main` e resolva conflito na sua branch. **Não** mande PR com conflito para o revisor resolver.

PR pequeno. Se passou de ~400 linhas alteradas, provavelmente são dois cards.

---

## 13. Épicos e responsáveis

Divisão preliminar da reunião de 13/09 — **o Trello é a fonte de verdade**, confirme lá antes de assumir.

| # | Épico | Responsável |
|---|---|---|
| 1 | Autenticação e Perfis de Acesso | Matheus |
| 2 | Perfil do Usuário | Kamilly |
| 3 | Monte seu Bolo | Yane |
| 4 | Outras Linhas de Produto | George |
| 5 | Carrinho e Checkout | Leandro |
| 6 | Análise e Orçamento (Confeiteira) | Vinícius |
| 7 | Aprovação de Orçamento e Sinal (Cliente) | Leandro |
| 8 | Agenda da Loja | George |
| 9 | Notificações | Matheus |
| 10 | Galeria de Fotos (Portfólio) | Kamilly |

**Dependências** — respeite a ordem, senão você trabalha em cima de base inexistente:

```
1 Autenticação → 2 Perfil
              → 3 Monte seu Bolo / 4 Outras Linhas → 5 Carrinho e Checkout
                                                   → 6 Análise → 7 Aprovação e Sinal → 8 Agenda
9 Notificações e 10 Galeria: transversais, dependem só de 1
```

Terminou seu épico? Pegue card livre no Trello antes de entrar no épico de outra pessoa.

---

## 14. Pendências e divergências conhecidas

Registre aqui tudo que não está decidido. **Não resolva por conta própria.**

| # | Assunto | Situação |
|---|---|---|
| 1 | Posição da aba Perfil (menu inferior vs. ícone no header, ao lado do sininho) | Discutido na reunião, não fechado |
| 2 | Imagens nos cards do cardápio (carrossel) | Matheus propôs, Yane preferiu manter só na aba Fotos — **fora do MVP** |
| 3 | "Recuperar senha" existe no Trello, não existe no protótipo | Implementar conforme card |
| 4 | Botão WhatsApp — card sem épico atribuído | Definir dono |
| 5 | Cancelamento < 3 dias sem devolução do sinal | Está no protótipo, não está em card do Trello |
| 6 | Cartão exibe "+taxa" no protótipo, sem regra de cálculo definida | Perguntar à Yane |
| 7 | Contadores de unidade começam em 0 ou no mínimo | Yane prefere 0 (contador) — confirmar UX final |

---

## 15. Como trabalhar neste repositório (agentes de IA)

1. **Leia o card do Trello inteiro** — descrição, regras de negócio e todos os critérios de aceite — antes de codar.
2. **Confira o protótipo** para número e fluxo de tela.
3. Implemente **backend primeiro**, depois o app. Regra que só existe no app não existe.
4. Faça o mínimo que satisfaz o card. Refatoração alheia ao card vai em PR separado.
5. Rode `mvn test` antes de commitar.
6. Ao terminar, liste na descrição do PR **cada critério de aceite e como foi atendido**.
7. Na dúvida sobre regra de negócio: **pergunte**. Não invente, não infira preço, não "melhore" regra da confeiteira.
