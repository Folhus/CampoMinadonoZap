# Campo Minado no WhatsApp

Bot de WhatsApp que permite jogar Campo Minado diretamente em conversas e grupos, com geração de imagem do tabuleiro em tempo real.

---

## Estrutura do Projeto

```
CampoMinadonoZap/
├── CampoMinado/               # Backend Java (lógica do jogo)
│   ├── src/
│   │   ├── CampoMinado.java   # Lógica principal do Campo Minado
│   │   ├── Jogo.java          # Loop principal e ponte JSON
│   │   └── Dados.java         # Modelo de dados para comunicação
│   ├── midia/
│   │   └── campo.png          # Imagem gerada do tabuleiro
│   ├── fontes/
│   │   └── static/
│   │       └── NotoEmoji-Bold.ttf
│   └── ponte.json             # Arquivo de comunicação entre Java e Node
│
└── BotWhatsApp/               # Frontend Node.js (bot WhatsApp)
    ├── src/
    │   ├── index.ts            # Inicialização do bot (Baileys)
    │   ├── handlers/
    │   │   ├── message.ts      # Roteador de comandos
    │   │   └── CampoMinadoH.ts # Handler do jogo (ponte Java ↔ Node)
    │   └── utils/
    │       ├── logger.ts       # Logger com pino
    │       └── message.ts      # Formatação de mensagens WhatsApp
    └── auth/                   # Credenciais de sessão (gerado em runtime)
```

---

## Arquitetura

O projeto usa uma arquitetura de **duas camadas** que se comunicam via arquivo JSON:

```
WhatsApp → Baileys (Node.js) → ponte.json → Java (Jogo.java) → ponte.json → Node.js → WhatsApp
```

1. O bot Node.js recebe o comando do usuário via WhatsApp
2. Escreve o comando em `ponte.json`
3. O processo Java lê o arquivo, executa a jogada e atualiza `ponte.json` com o resultado
4. O Java também gera/atualiza a imagem `campo.png` do tabuleiro
5. O Node.js lê o resultado, carrega a imagem e responde ao usuário

---

## Comandos disponíveis

| Comando | Descrição |
|---|---|
| `!novo` | Inicia um novo jogo (campo 14×14, 28 minas) |
| `!jogar A1` | Revela a célula A1 |
| `!jogar A1 B3 C5` | Revela múltiplas células de uma vez |
| `!bandeira A1` | Coloca ou remove bandeira em A1 |
| `!status` | Mostra o status atual do jogo |
| `!ajuda` | Lista todos os comandos |

### Coordenadas

- **Linhas:** `A` até `N` (14 linhas)
- **Colunas:** `1` até `14`
- Exemplos válidos: `A1`, `B5`, `M14`, `N9`

---

## Pré-requisitos

- **Java 11+**
- **Node.js 18+**
- **npm**
- Biblioteca [Gson](https://github.com/google/gson) para Java (via JAR no classpath)
- Fonte `NotoEmoji-Bold.ttf` na pasta `fontes/static/`

---

## Instalação e Execução

### 1. Clonar o repositório

```bash
git clone https://github.com/seu-usuario/CampoMinadonoZap.git
cd CampoMinadonoZap
```

### 2. Configurar e compilar o Java

```bash
cd CampoMinado
# Certifique-se de que o gson.jar está no classpath
javac -cp ".;gson.jar" src/*.java -d bin
```

### 3. Iniciar o processo Java

```bash
java -cp "bin;gson.jar" Jogo
```

> O processo Java ficará aguardando comandos via `ponte.json`.

### 4. Configurar o bot Node.js

```bash
cd ../BotWhatsApp
npm install
```

Antes de iniciar, ajuste os caminhos absolutos em `src/handlers/CampoMinadoH.ts`:

```typescript
this.javaPath = 'CAMINHO/PARA/CampoMinado/bin';
this.baseImagePath = 'CAMINHO/PARA/CampoMinado/midia';
this.pontePath = 'CAMINHO/PARA/CampoMinado/ponte.json';
```

E em `src/CampoMinado.java`, ajuste o caminho de saída da imagem:

```java
File outputDir = new File("CAMINHO/PARA/CampoMinado/midia");
```

### 5. Iniciar o bot

```bash
npm run start:dev
```

Na primeira execução, um QR code será exibido no terminal. Escaneie com o WhatsApp para autenticar.

---

## Configuração

As principais configurações ficam em `src/index.ts`:

```typescript
const CONNECTION_TYPE = "QR";        // "QR" ou "NUMBER"
const PHONE_NUMBER = "5500000000000"; // usado apenas se CONNECTION_TYPE = "NUMBER"
const USE_LASTEST_VERSION = true;     // usa a versão mais recente do WhatsApp Web
const MAX_RETRIES = 5;                // tentativas de reconexão
const RETRY_INTERVAL = 5000;          // intervalo entre tentativas (ms)
```

---

## Dependências Node.js

| Pacote | Uso |
|---|---|
| `baileys` | Conexão com WhatsApp Web |
| `qrcode-terminal` | Exibe QR code no terminal |
| `@hapi/boom` | Tratamento de erros de conexão |
| `pino` + `pino-pretty` | Logger formatado |
| `jsonfile` | Leitura/escrita da ponte JSON |

---

## Problemas conhecidos

- **Caminhos hardcoded:** Os caminhos absolutos no `CampoMinadoH.ts` e `CampoMinado.java` precisam ser ajustados manualmente para cada ambiente. Considere mover para variáveis de ambiente (`.env`).
- **Lógica de coordenadas múltiplas:** O parsing de múltiplas coordenadas em `fazerJogada()` possui uma lógica de índice que pode ignorar algumas coordenadas — revisar o loop `while(i < coord.length)`.
- **Leitura do JSON de resposta:** Em alguns handlers, a leitura do `ponte.json` usa `.Comando` (maiúsculo) em vez de `.comando`, causando comparações que nunca batem. Padronizar a chave resolve o problema.
- **Sem suporte a múltiplos jogos simultâneos:** O Java mantém apenas um arquivo `jogo_salvo.ser` e `campo.png`, então múltiplos grupos usando o bot ao mesmo tempo podem conflitar.

---

## Melhorias futuras

- [X] Usar variáveis de ambiente (`dotenv`) para os caminhos
- [ ] Suporte a múltiplos jogos simultâneos por grupo/chat
- [ ] Dificuldades configuráveis (campo e número de minas)
- [ ] Placar e histórico de partidas
- [ ] Substituir a ponte via arquivo por comunicação via stdin/stdout ou socket

---

## Licença

MIT
