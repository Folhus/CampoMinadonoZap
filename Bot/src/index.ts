import 'dotenv/config';
import makeWASocket, {
  Browsers,
  useMultiFileAuthState,
  DisconnectReason,
  WAMessage,
  fetchLatestWaWebVersion,
} from "baileys";
import qrcode from "qrcode-terminal";
import { Boom } from "@hapi/boom";
import { logger } from "./utils/logger";
import { FormattedMessage, getMessage } from "./utils/message";
import MessageHandler from "./handlers/message";


const CONNECTION_TYPE = process.env.CONNECTION_TYPE || "QR";
const PHONE_NUMBER = process.env.PHONE_NUMBER || "";
const USE_LASTEST_VERSION = true;

let retryCount = 0;
const MAX_RETRIES = parseInt(process.env.MAX_RETRIES || "5");
const RETRY_INTERVAL = parseInt(process.env.RETRY_INTERVAL || "5000");

const startBot = async () => {
  try {
    await initWASocket();
  } catch (error) {
    logger.error('Erro fatal ao iniciar o bot: ' + (error instanceof Error ? error.message : String(error)));
    if (retryCount < MAX_RETRIES) {
      retryCount++;
      logger.info(`Tentando reiniciar o bot... Tentativa ${retryCount}/${MAX_RETRIES}`);
      setTimeout(startBot, RETRY_INTERVAL);
    } else {
      logger.error('Número máximo de tentativas excedido. O bot será encerrado.');
      process.exit(1);
    }
  }
};

export const initWASocket = async (): Promise<void> => {
  try {
    const { state, saveCreds } = await useMultiFileAuthState("auth");

    const { version, isLatest } = await fetchLatestWaWebVersion({});

    if (USE_LASTEST_VERSION) {
      logger.info(
        `Versão atual do WaWeb: ${version.join(".")} | ${isLatest ? "Versão mais recente" : "Está desatualizado"
        }`
      );
    }

    // @ts-ignore
    const sock = makeWASocket({
      auth: state,
      browser:
        // @ts-ignore
        CONNECTION_TYPE === "NUMBER"
          ? Browsers.ubuntu("Chrome")
          : Browsers.appropriate("Desktop"),
      printQRInTerminal: false,
      version: USE_LASTEST_VERSION ? version : undefined,
      defaultQueryTimeoutMs: 60000, // 60 segundos
      getMessage: async (key) => {
        return { conversation: 'retry' }
      },
      retryRequestDelayMs: 1000,    // Delay entre tentativas
    });

  // @ts-ignore
  if (CONNECTION_TYPE === "NUMBER" && !sock.authState.creds.registered) {
    try {
      const code = await sock.requestPairingCode(PHONE_NUMBER);
      logger.info(`Código de Pareamento: ${code}`);
    } catch (error) {
      logger.error("Erro ao obter o código.");
    }
  }

  sock.ev.on(
    "connection.update",
    async ({ connection, lastDisconnect, qr }: any) => {
      logger.info(
        `Socket Connection Update: ${connection || ""} ${lastDisconnect ? JSON.stringify(lastDisconnect.error) : ""}`
      );

      switch (connection) {
        case "close":
          const statusCode = (lastDisconnect?.error as Boom)?.output?.statusCode;
          
          if (statusCode === DisconnectReason.loggedOut) {
            // Permite reinicializar para mostrar QR code novamente
            retryCount = 0;
            setTimeout(() => initWASocket(), RETRY_INTERVAL);
          } else if (statusCode === DisconnectReason.connectionClosed) {
            if (retryCount < MAX_RETRIES) {
              retryCount++;
              setTimeout(() => initWASocket(), RETRY_INTERVAL);
            }
          } else if (statusCode === DisconnectReason.connectionLost) {
            if (retryCount < MAX_RETRIES) {
              retryCount++;
              setTimeout(() => initWASocket(), RETRY_INTERVAL);
            }
          } else if (statusCode === DisconnectReason.connectionReplaced) {
            retryCount = MAX_RETRIES;
          } else if (statusCode === DisconnectReason.timedOut) {
            if (retryCount < MAX_RETRIES) {
              retryCount++;
              setTimeout(() => initWASocket(), RETRY_INTERVAL);
            }
          } else {
            if (retryCount < MAX_RETRIES) {
              retryCount++;
              setTimeout(() => initWASocket(), RETRY_INTERVAL);
            }
          }
          break;
          
        case "open":
          logger.info("Bot Conectado com sucesso!");
          retryCount = 0; // Reset contador de tentativas
          break;
          
        case "connecting":
          logger.info("Conectando ao WhatsApp...");
          break;
      }

      if (qr !== undefined && CONNECTION_TYPE === "QR") {
        logger.info("Novo codigo QR gerado");
        qrcode.generate(qr, { small: true });
      }
    }
  );

  // Gerenciamento de mensagens
  sock.ev.on("messages.upsert", async ({ messages, type }: { messages: WAMessage[], type: string }) => {
    for (const message of messages) {
      // Ignora mensagens sem remoteJid
      if (!message?.key?.remoteJid) continue;

      // Ignora mensagens de status/broadcast
      if (message.key.remoteJid === "status@broadcast") continue;

      // Ignora comunidades
      if (message.key.remoteJid.endsWith("@g.us") && message.key.remoteJid.includes("--")) continue;

      if (message.key.remoteJid.endsWith("@newsletter")) continue;

      // Processa apenas mensagens que tenham conteúdo relevante
      const formattedMessage: FormattedMessage | undefined = getMessage(message);
      if (!formattedMessage || !formattedMessage.content) continue;

      await MessageHandler(sock, formattedMessage);
    }
});

  // Salvar as credenciais de autenticação
  sock.ev.on("creds.update", saveCreds);

  // Registrar manipulador para erros não tratados
  process.on('unhandledRejection', (error) => {
    logger.error('Erro não tratado (unhandledRejection): ' + (error instanceof Error ? error.message : String(error)));
  });

  process.on('uncaughtException', (error) => {
    logger.error('Erro não tratado (uncaughtException): ' + error.message);
  });
  
  } catch (error) {
    logger.error('Erro ao inicializar o socket: ' + (error instanceof Error ? error.message : String(error)));
    throw error;
  }
};

// Iniciar o bot
startBot();



//npm run start:dev