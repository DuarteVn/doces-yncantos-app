-- =============================================================================
-- Doces Yncantos — V1__schema_inicial.sql
-- PostgreSQL 15+ (Supabase) · Flyway
--
-- Schema dedicado `doces`: fora do schema `public`, portanto NÃO é exposto
-- pela API REST automática do Supabase (PostgREST publica só `public` e
-- `graphql_public` por padrão). Ou seja: a anon key que vai dentro do APK
-- não alcança nenhuma destas tabelas. O único caminho até os dados é o
-- backend Spring Boot, que é onde a autorização por perfil é validada.
-- Não mova estas tabelas para `public` sem antes ligar RLS.
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS doces;

-- Blindagem explícita: se os roles do Supabase existirem, nenhum deles enxerga
-- este schema. Em Postgres local esses roles não existem — daí o guard.
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN
    EXECUTE 'REVOKE ALL ON SCHEMA doces FROM anon, authenticated';
  END IF;
END $$;

SET search_path TO doces;


-- =============================================================================
-- 0. INFRAESTRUTURA
-- =============================================================================

-- Mantém `atualizado_em` sem depender de o serviço lembrar de setar.
CREATE OR REPLACE FUNCTION doces.set_atualizado_em()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
  NEW.atualizado_em := now();
  RETURN NEW;
END $$;


-- Parâmetros de negócio que a confeiteira pode querer mudar sem redeploy.
-- Leia daqui, nunca hardcode no Java nem no Android.
CREATE TABLE parametro (
  chave        VARCHAR(60) PRIMARY KEY,
  valor        TEXT        NOT NULL,
  descricao    TEXT        NOT NULL,
  atualizado_em TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE parametro IS
  'Configuração de negócio (prazo mínimo, % do sinal, chave Pix, horários).';


-- =============================================================================
-- 1. CATÁLOGO — tabelas de referência, populadas em V2
-- =============================================================================

-- Tamanhos de bolo. `multiplicador_recheio` codifica a regra do ajuste G/GG:
-- o acréscimo de um recheio especial é multiplicado por este fator.
CREATE TABLE tamanho_bolo (
  codigo                VARCHAR(4)   PRIMARY KEY,
  nome                  VARCHAR(40)  NOT NULL,
  diametro_cm           SMALLINT     NOT NULL,
  fatias_min            SMALLINT     NOT NULL,
  fatias_max            SMALLINT     NOT NULL,
  multiplicador_recheio NUMERIC(3,2) NOT NULL DEFAULT 1.00,
  ordem                 SMALLINT     NOT NULL,
  CONSTRAINT ck_tamanho_multiplicador CHECK (multiplicador_recheio >= 1.00),
  CONSTRAINT ck_tamanho_fatias        CHECK (fatias_max >= fatias_min)
);

COMMENT ON COLUMN tamanho_bolo.multiplicador_recheio IS
  'Fator sobre o acréscimo do recheio: 1.00 em PP/P/M, 1.50 em G, 2.00 em GG. Não afeta recheio sem acréscimo nem o acréscimo da massa.';


-- Tipos de bolo do "Monte seu Bolo".
-- `restrito` = o tipo por si só já limita a 1 recheio e bloqueia massa mista
-- (Chantininho baixo). O tamanho PP impõe a mesma restrição — ver tipo_bolo.restrito
-- OR tamanho = 'PP' na regra de negócio.
CREATE TABLE tipo_bolo (
  codigo          VARCHAR(30) PRIMARY KEY,
  nome            VARCHAR(60) NOT NULL,
  camadas_massa   SMALLINT    NOT NULL,
  camadas_recheio SMALLINT    NOT NULL,
  restrito        BOOLEAN     NOT NULL DEFAULT FALSE,
  ativo           BOOLEAN     NOT NULL DEFAULT TRUE,
  ordem           SMALLINT    NOT NULL,
  CONSTRAINT ck_tipo_camadas CHECK (camadas_recheio = camadas_massa - 1)
);


CREATE TABLE preco_bolo (
  tipo_bolo_codigo    VARCHAR(30)  NOT NULL REFERENCES tipo_bolo(codigo)    ON DELETE CASCADE,
  tamanho_bolo_codigo VARCHAR(4)   NOT NULL REFERENCES tamanho_bolo(codigo) ON DELETE CASCADE,
  preco               NUMERIC(10,2) NOT NULL,
  PRIMARY KEY (tipo_bolo_codigo, tamanho_bolo_codigo),
  CONSTRAINT ck_preco_bolo_positivo CHECK (preco > 0)
);


CREATE TABLE massa (
  codigo              VARCHAR(30)   PRIMARY KEY,
  nome                VARCHAR(40)   NOT NULL,
  acrescimo           NUMERIC(10,2) NOT NULL DEFAULT 0,
  cor_hex             CHAR(7),
  permite_em_restrito BOOLEAN       NOT NULL DEFAULT TRUE,
  ativo               BOOLEAN       NOT NULL DEFAULT TRUE,
  ordem               SMALLINT      NOT NULL,
  CONSTRAINT ck_massa_acrescimo CHECK (acrescimo >= 0)
);

COMMENT ON COLUMN massa.permite_em_restrito IS
  'FALSE na massa Mista: indisponível em Chantininho baixo e em tamanho PP.';
COMMENT ON COLUMN massa.acrescimo IS
  'Somado uma única vez, sem multiplicador de tamanho.';


CREATE TABLE recheio (
  codigo      VARCHAR(30)   PRIMARY KEY,
  nome        VARCHAR(60)   NOT NULL,
  acrescimo   NUMERIC(10,2) NOT NULL DEFAULT 0,
  cor_hex     CHAR(7),
  slot_extra  BOOLEAN       NOT NULL DEFAULT FALSE,
  ativo       BOOLEAN       NOT NULL DEFAULT TRUE,
  ordem       SMALLINT      NOT NULL,
  CONSTRAINT ck_recheio_acrescimo CHECK (acrescimo >= 0)
);

COMMENT ON COLUMN recheio.slot_extra IS
  'TRUE na Geleia de morango: não conta no limite de recheios, entra sempre como sabor extra.';


-- Linhas de produto além do "Monte seu Bolo".
CREATE TABLE produto (
  codigo                     VARCHAR(30) PRIMARY KEY,
  nome                       VARCHAR(60) NOT NULL,
  tipo_configuracao          VARCHAR(20) NOT NULL,
  quantidade_minima          SMALLINT,
  unidade_label              VARCHAR(20),
  decoravel                  BOOLEAN     NOT NULL DEFAULT FALSE,
  tem_sabor_bolo             BOOLEAN     NOT NULL DEFAULT FALSE,
  aplica_multiplicador_tamanho BOOLEAN   NOT NULL DEFAULT FALSE,
  ativo                      BOOLEAN     NOT NULL DEFAULT TRUE,
  ordem                      SMALLINT    NOT NULL,
  CONSTRAINT ck_produto_tipo_config CHECK (
    tipo_configuracao IN ('BUILDER','ANDAR','OPCAO','OPCAO_VARIACAO','UNIDADE')
  ),
  CONSTRAINT ck_produto_minimo CHECK (
    (tipo_configuracao = 'UNIDADE' AND quantidade_minima IS NOT NULL AND quantidade_minima > 0)
    OR (tipo_configuracao <> 'UNIDADE' AND quantidade_minima IS NULL)
  )
);

COMMENT ON COLUMN produto.aplica_multiplicador_tamanho IS
  'TRUE só no Monte seu Bolo. Bolo de Andar e Kit Festa NÃO aplicam o ajuste G/GG nos recheios.';
COMMENT ON COLUMN produto.quantidade_minima IS
  'Mínimo POR sabor/recheio escolhido, nunca pelo somatório do item.';


-- Opção dentro de um produto: um tamanho de Bolo Fake, um kit, um sabor de
-- caseirinho, um sabor de brigadeiro.
CREATE TABLE produto_opcao (
  id              BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  produto_codigo  VARCHAR(30)   NOT NULL REFERENCES produto(codigo) ON DELETE CASCADE,
  codigo          VARCHAR(30)   NOT NULL,
  nome            VARCHAR(120)  NOT NULL,
  descricao       TEXT,
  preco           NUMERIC(10,2),
  ativo           BOOLEAN       NOT NULL DEFAULT TRUE,
  ordem           SMALLINT      NOT NULL,
  CONSTRAINT uq_produto_opcao UNIQUE (produto_codigo, codigo),
  CONSTRAINT ck_produto_opcao_preco CHECK (preco IS NULL OR preco > 0)
);

COMMENT ON COLUMN produto_opcao.preco IS
  'NULL quando o preço depende da variação de tamanho (Caseirinhos) — ver produto_opcao_variacao.';


-- Usada só por Caseirinhos hoje: mesmo sabor, preço por tamanho (P/G).
CREATE TABLE produto_opcao_variacao (
  id               BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  produto_opcao_id BIGINT        NOT NULL REFERENCES produto_opcao(id) ON DELETE CASCADE,
  codigo           VARCHAR(10)   NOT NULL,
  nome             VARCHAR(40)   NOT NULL,
  preco            NUMERIC(10,2) NOT NULL,
  ordem            SMALLINT      NOT NULL,
  CONSTRAINT uq_opcao_variacao UNIQUE (produto_opcao_id, codigo),
  CONSTRAINT ck_variacao_preco CHECK (preco > 0)
);


CREATE TABLE categoria_galeria (
  codigo VARCHAR(30) PRIMARY KEY,
  nome   VARCHAR(40) NOT NULL,
  ordem  SMALLINT    NOT NULL
);


-- =============================================================================
-- 2. USUÁRIOS
-- =============================================================================

-- ATENÇÃO à unicidade: é (email, perfil), não email sozinho.
-- Motivo: no login a usuária escolhe o perfil na própria tela, e a regra diz
-- que perfis não compartilham dados. A mesma pessoa pode ter conta Cliente e
-- conta Confeiteira com o mesmo e-mail — são dois registros independentes.
-- Consequência: o login exige e-mail + senha + perfil.
CREATE TABLE usuario (
  id            BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  nome          VARCHAR(120) NOT NULL,
  email         VARCHAR(160) NOT NULL,
  telefone      VARCHAR(20),
  senha_hash    VARCHAR(72)  NOT NULL,
  perfil        VARCHAR(12)  NOT NULL,
  ativo         BOOLEAN      NOT NULL DEFAULT TRUE,
  criado_em     TIMESTAMPTZ  NOT NULL DEFAULT now(),
  atualizado_em TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT uq_usuario_email_perfil UNIQUE (email, perfil),
  CONSTRAINT ck_usuario_perfil CHECK (perfil IN ('CLIENTE','CONFEITEIRA','ADMIN')),
  CONSTRAINT ck_usuario_email  CHECK (position('@' IN email) > 1)
);

COMMENT ON COLUMN usuario.senha_hash IS 'BCrypt. Nunca trafega em DTO de response.';
COMMENT ON TABLE usuario IS
  'Endereço NÃO fica aqui: é informado por pedido, no checkout.';

CREATE TRIGGER tg_usuario_atualizado_em
  BEFORE UPDATE ON usuario
  FOR EACH ROW EXECUTE FUNCTION doces.set_atualizado_em();

CREATE INDEX ix_usuario_perfil ON usuario (perfil) WHERE ativo;


CREATE TABLE token_redefinicao_senha (
  id         BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  usuario_id BIGINT      NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
  token_hash VARCHAR(64) NOT NULL UNIQUE,
  expira_em  TIMESTAMPTZ NOT NULL,
  usado_em   TIMESTAMPTZ,
  criado_em  TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON COLUMN token_redefinicao_senha.token_hash IS
  'SHA-256 do token enviado por e-mail. O token em claro nunca é persistido.';

CREATE INDEX ix_token_redefinicao_usuario ON token_redefinicao_senha (usuario_id);


-- =============================================================================
-- 3. PEDIDO
-- =============================================================================

-- valor_total e sinal são colunas GERADAS: é impossível gravar um sinal
-- divergente da regra (50% do total, arredondado para cima em centavos).
-- Em JPA, mapeie ambas com insertable=false, updatable=false e anote a
-- entidade com @DynamicInsert; recarregue a entidade após o save para ler
-- os valores calculados.
CREATE TABLE pedido (
  id                        BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  cliente_id                BIGINT        NOT NULL REFERENCES usuario(id) ON DELETE RESTRICT,
  status                    VARCHAR(24)   NOT NULL DEFAULT 'EM_ANALISE',

  data_entrega              DATE          NOT NULL,
  horario                   TIME          NOT NULL,
  entrega                   BOOLEAN       NOT NULL DEFAULT FALSE,
  endereco                  TEXT,

  forma_pagamento           VARCHAR(10)   NOT NULL,
  link_pagamento            TEXT,

  valor_itens               NUMERIC(10,2) NOT NULL,
  valor_complemento         NUMERIC(10,2) NOT NULL DEFAULT 0,
  valor_total               NUMERIC(10,2)
                              GENERATED ALWAYS AS (valor_itens + valor_complemento) STORED,
  sinal                     NUMERIC(10,2)
                              GENERATED ALWAYS AS (ceil((valor_itens + valor_complemento) * 50) / 100) STORED,

  observacao_orcamento      TEXT,
  observacoes               TEXT,
  sinal_marcado_pela_cliente BOOLEAN      NOT NULL DEFAULT FALSE,

  analisado_em              TIMESTAMPTZ,
  confirmado_em             TIMESTAMPTZ,
  entregue_em               TIMESTAMPTZ,
  cancelado_em              TIMESTAMPTZ,
  criado_em                 TIMESTAMPTZ   NOT NULL DEFAULT now(),
  atualizado_em             TIMESTAMPTZ   NOT NULL DEFAULT now(),

  CONSTRAINT ck_pedido_status CHECK (status IN (
    'EM_ANALISE', 'AGUARDANDO_APROVACAO', 'AGUARDANDO_SINAL',
    'CONFIRMADO', 'ENTREGUE', 'CANCELADO'
  )),
  CONSTRAINT ck_pedido_forma_pagamento CHECK (forma_pagamento IN ('PIX','CARTAO')),
  CONSTRAINT ck_pedido_valores CHECK (valor_itens > 0 AND valor_complemento >= 0),

  -- Entrega exige endereço; retirada não guarda endereço nenhum.
  CONSTRAINT ck_pedido_endereco CHECK (
    (entrega AND endereco IS NOT NULL AND length(btrim(endereco)) > 0)
    OR (NOT entrega AND endereco IS NULL)
  ),

  -- Horários: 07:00–18:00, de 30 em 30 minutos.
  CONSTRAINT ck_pedido_horario CHECK (
    horario BETWEEN TIME '07:00' AND TIME '18:00'
    AND EXTRACT(MINUTE FROM horario) IN (0, 30)
    AND EXTRACT(SECOND FROM horario) = 0
  ),

  -- Link de pagamento só faz sentido em cartão.
  CONSTRAINT ck_pedido_link CHECK (link_pagamento IS NULL OR forma_pagamento = 'CARTAO'),

  -- Complemento > 0 exige a observação que a cliente vai ler.
  CONSTRAINT ck_pedido_observacao_orcamento CHECK (
    valor_complemento = 0 OR observacao_orcamento IS NOT NULL
  ),

  CONSTRAINT ck_pedido_cancelado CHECK (
    (status = 'CANCELADO' AND cancelado_em IS NOT NULL)
    OR (status <> 'CANCELADO' AND cancelado_em IS NULL)
  )
);

COMMENT ON COLUMN pedido.sinal IS
  'Coluna gerada: ceil(total * 50) / 100 = 50% arredondado para cima em centavos.';
COMMENT ON COLUMN pedido.sinal_marcado_pela_cliente IS
  'Flag informativa. NÃO avança status — quem confirma é a confeiteira.';
COMMENT ON COLUMN pedido.data_entrega IS
  'Antecedência mínima de 5 dias é validada no service (CHECK não pode usar now()).';

CREATE TRIGGER tg_pedido_atualizado_em
  BEFORE UPDATE ON pedido
  FOR EACH ROW EXECUTE FUNCTION doces.set_atualizado_em();

-- "Meus pedidos", mais recentes primeiro.
CREATE INDEX ix_pedido_cliente ON pedido (cliente_id, criado_em DESC);

-- Filas da Agenda (EM_ANALISE, AGUARDANDO_APROVACAO, AGUARDANDO_SINAL).
CREATE INDEX ix_pedido_fila ON pedido (status, data_entrega)
  WHERE status IN ('EM_ANALISE','AGUARDANDO_APROVACAO','AGUARDANDO_SINAL');

-- Calendário: só pedido fechado ocupa data. Índice parcial mantém pequeno
-- o índice que a tela mais usada da confeiteira consulta.
CREATE INDEX ix_pedido_calendario ON pedido (data_entrega, horario)
  WHERE status IN ('CONFIRMADO','ENTREGUE');


-- Um item = uma linha do carrinho. `resumo` é a descrição legível que a
-- confeiteira lê na Agenda; `configuracao` é a mesma escolha em formato
-- estruturado, para auditar preço depois.
CREATE TABLE item_pedido (
  id                     BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  pedido_id              BIGINT        NOT NULL REFERENCES pedido(id) ON DELETE CASCADE,
  produto_codigo         VARCHAR(30)   NOT NULL REFERENCES produto(codigo) ON DELETE RESTRICT,
  resumo                 TEXT          NOT NULL,
  valor                  NUMERIC(10,2) NOT NULL,
  decoracao_personalizada BOOLEAN      NOT NULL DEFAULT FALSE,
  observacao_decoracao   TEXT,
  foto_referencia_path   TEXT,
  configuracao           JSONB         NOT NULL DEFAULT '{}'::jsonb,
  criado_em              TIMESTAMPTZ   NOT NULL DEFAULT now(),
  CONSTRAINT ck_item_valor CHECK (valor > 0)
);

COMMENT ON COLUMN item_pedido.resumo IS
  'Ex.: "Chantininho tradicional M · Massa: Chocolate · Recheio: Brigadeiro preto + Maracujá".';
COMMENT ON COLUMN item_pedido.foto_referencia_path IS
  'Caminho do objeto no bucket privado do Supabase Storage. Nunca base64.';
COMMENT ON COLUMN item_pedido.configuracao IS
  'Seleção estruturada: tipo, tamanho, massa, recheios, quantidades por sabor.';

CREATE INDEX ix_item_pedido_pedido ON item_pedido (pedido_id);


-- =============================================================================
-- 4. NOTIFICAÇÕES
-- =============================================================================

-- Sino dentro do app. Não é push externo (sem FCM/SMS/WhatsApp).
-- Admin enxerga todas: a consulta filtra por perfil, ou traz tudo se ADMIN.
CREATE TABLE notificacao (
  id                     BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  destinatario_perfil    VARCHAR(12)  NOT NULL,
  destinatario_usuario_id BIGINT      REFERENCES usuario(id) ON DELETE CASCADE,
  pedido_id              BIGINT       REFERENCES pedido(id)  ON DELETE CASCADE,
  evento                 VARCHAR(40)  NOT NULL,
  mensagem               TEXT         NOT NULL,
  lida                   BOOLEAN      NOT NULL DEFAULT FALSE,
  criado_em              TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT ck_notificacao_perfil CHECK (destinatario_perfil IN ('CLIENTE','CONFEITEIRA','ADMIN')),
  CONSTRAINT ck_notificacao_evento CHECK (evento IN (
    'PEDIDO_CRIADO', 'ORCAMENTO_APROVADO', 'SINAL_SINALIZADO', 'PEDIDO_CANCELADO',
    'PEDIDO_ANALISADO', 'ORCAMENTO_ENVIADO', 'SINAL_CONFIRMADO',
    'DATA_HORARIO_ALTERADO', 'PEDIDO_ENTREGUE'
  ))
);

COMMENT ON COLUMN notificacao.destinatario_usuario_id IS
  'Preenchido para eventos dirigidos a uma cliente específica. NULL = qualquer usuária do perfil.';

CREATE INDEX ix_notificacao_nao_lida ON notificacao (destinatario_perfil, criado_em DESC)
  WHERE NOT lida;
CREATE INDEX ix_notificacao_usuario ON notificacao (destinatario_usuario_id, criado_em DESC);


-- =============================================================================
-- 5. GALERIA (PORTFÓLIO)
-- =============================================================================

CREATE TABLE foto_portfolio (
  id                BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  categoria_codigo  VARCHAR(30) NOT NULL REFERENCES categoria_galeria(codigo) ON DELETE RESTRICT,
  caminho_arquivo   TEXT        NOT NULL,
  criado_por_id     BIGINT      REFERENCES usuario(id) ON DELETE SET NULL,
  criado_em         TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON COLUMN foto_portfolio.caminho_arquivo IS
  'Caminho no bucket do Supabase Storage. O backend devolve URL assinada.';

CREATE INDEX ix_foto_categoria ON foto_portfolio (categoria_codigo, criado_em DESC);


-- =============================================================================
-- 6. VIEW DE APOIO
-- =============================================================================

-- Alimenta o indicador de quantidade por dia no calendário da Agenda.
CREATE VIEW vw_agenda_calendario AS
SELECT
  data_entrega,
  count(*)                                        AS total_pedidos,
  count(*) FILTER (WHERE status = 'CONFIRMADO')   AS confirmados,
  count(*) FILTER (WHERE status = 'ENTREGUE')     AS entregues
FROM pedido
WHERE status IN ('CONFIRMADO','ENTREGUE')
GROUP BY data_entrega;
