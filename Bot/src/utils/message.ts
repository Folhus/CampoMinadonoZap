import { proto, WAMessage, WASocket } from "baileys";
import { logger } from "./logger";

export type FormattedMessage = {
  key: proto.IMessageKey;
  messageTimestamp: Number | Long | null;
  pushName: string | null;
  content: string | null;
};

/**
 * @param message
 * @returns a message vindo do Baileys para algo mais amigável.
 */
export const getMessage = (message: WAMessage) => {
  try {
    const textContent =
      message.message?.conversation ||
      message.message?.extendedTextMessage?.text ||
      '';

    const cleanedContent = textContent.trim().replace(/\s+/g, ' ').toLowerCase();

    let ts: Number | Long | null = null;
    if (typeof message.messageTimestamp === 'number' || typeof message.messageTimestamp === 'object') {
      ts = message.messageTimestamp as Number | Long;
    }

    return {
      key: message.key,
      messageTimestamp: ts,
      pushName: message.pushName ?? null,
      content: cleanedContent || null,
    };
  } catch {
    return undefined;
  }
};


export const sendImage = async (
  sock: WASocket,
  jid: string,
  imagePath: string,
  caption?: string
): Promise<any> => {
  try {
    return await sock.sendMessage(jid, {
      image: { url: imagePath },
      caption: caption
    });
  } catch (error) {
    logger.error('Erro ao enviar imagem:',);
  }
};