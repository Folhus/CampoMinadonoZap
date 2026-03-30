import { CampoMinadoH } from './CampoMinadoH';
import { FormattedMessage } from '../utils/message';
import { proto } from 'baileys';

const campoMinadoH = new CampoMinadoH();

const MessageHandler = async (sock: any, message: FormattedMessage): Promise<void> => {
    const content = message.content?.toLowerCase() || '';
    const remoteJid = message.key.remoteJid;
    const isGroup = remoteJid?.endsWith('@g.us');

    console.log('MENSAGEM RECEBIDA:', {
        from: remoteJid,
        content: content,
        pushName: message.pushName,
        isGroup: isGroup
    });

    if (!content || !remoteJid) {
        console.log('Mensagem sem conteúdo ou remoteJid');
        return;
    }

    // Se não for um comando do jogo, ignora a mensagem
    if (!content.startsWith('!')) return;

    try {
        // Comandos do Campo Minado
        if (content.startsWith('!novo') || content.startsWith('!campominado')) {
            await handleNovoJogo(sock, remoteJid);
        }
        else if (content.startsWith('!jogar')) {
            await handleJogar(sock, remoteJid, content);
        }
        else if (content.startsWith('!bandeira')) {
            await handleBandeira(sock, remoteJid, content);
        }
        else if (content.startsWith('!status')) {
            await handleStatus(sock, remoteJid);
        }
        else if (content.startsWith('!ajuda') || content.startsWith('!help')) {
            await handleAjuda(sock, remoteJid);
        }
        else if (content.startsWith('!')) {
            // Comando não reconhecido
            await sock.sendMessage(remoteJid, {
                text: "Comando nao reconhecido! Use !ajuda para ver os comandos disponiveis."
            });
        }
    } catch (error) {
        console.error('Erro no handler de mensagem:', error);
        await sock.sendMessage(remoteJid, {
            text: "Ocorreu um erro ao processar seu comando."
        });
    }
};

async function handleNovoJogo(sock: any, remoteJid: string) {
    const result = await campoMinadoH.novoJogo(remoteJid);

    if (result.success && result.imagePath) {
        try {
            const imageBuffer = await campoMinadoH.getImageBuffer(result.imagePath);
            if (imageBuffer) {
                await sock.sendMessage(remoteJid, {
                    image: imageBuffer,
                    caption: result.message
                });
            } else {
                await sock.sendMessage(remoteJid, {
                    text: result.message + "\nErro: Imagem não encontrada"
                });
            }
        } catch (error) {
            console.error('Erro ao enviar imagem:', error);
            await sock.sendMessage(remoteJid, {
                text: result.message + "\nErro ao carregar imagem"
            });
        }
    } else {
        await sock.sendMessage(remoteJid, {
            text: result.message
        });
    }
}

async function handleJogar(sock: any, remoteJid: string, content: string) {
    const args = content.split(' ');

    if (args.length < 2) {
        await sock.sendMessage(remoteJid, {
            text: "Use: !jogar <coordenada>\nExemplo: !jogar A1"
        });
        return;
    }

    const jogada = args[1].toUpperCase();
    const result = await campoMinadoH.fazerJogada(remoteJid, jogada);

    await sendGameResponse(sock, remoteJid, result);
}

async function handleBandeira(sock: any, remoteJid: string, content: string) {
    const args = content.split(' ');

    if (args.length < 2) {
        await sock.sendMessage(remoteJid, {
            text: "Use: !bandeira <coordenada>\nExemplo: !bandeira A1"
        });
        return;
    }

    const jogada = args[1].toUpperCase();
    const result = await campoMinadoH.toggleBandeira(remoteJid, jogada);

    await sendGameResponse(sock, remoteJid, result);
}

async function handleStatus(sock: any, remoteJid: string) {
    const result = await campoMinadoH.verStatus(remoteJid);
    await sendGameResponse(sock, remoteJid, result);
}

async function handleAjuda(sock: any, remoteJid: string) {
    const helpText = `COMANDOS CAMPO MINADO

!novo - Inicia um novo jogo (14x14, 14 minas)
!jogar A1, B2 - Revela a celula A1 e B2
!bandeira A1, B2 - Coloca/remove bandeira em A1 e B2
!status - Mostra status atual do jogo
!ajuda - Mostra esta mensagem

COORDENADAS:
Linhas: A ate N (14 linhas)
Colunas: 1 ate 14
Exemplos: A1, B5, M14

COMO JOGAR:
Use !jogar para revelar celulas
Use !bandeira para marcar minas
Evite as 28 minas escondidas!`;

    await sock.sendMessage(remoteJid, {
        text: helpText
    });
}

async function sendGameResponse(sock: any, remoteJid: string, result: any) {
    if (result.success && result.imagePath) {
        try {
            const imageBuffer = await campoMinadoH.getImageBuffer(result.imagePath);
            if (imageBuffer) {
                await sock.sendMessage(remoteJid, {
                    image: imageBuffer,
                    caption: result.message
                });
            } else {
                await sock.sendMessage(remoteJid, {
                    text: result.message + "\nErro: Imagem não encontrada"
                });
            }
        } catch (error) {
            console.error('Erro ao carregar imagem:', error);
            await sock.sendMessage(remoteJid, {
                text: result.message + "\nErro ao carregar imagem"
            });
        }
    } else {
        await sock.sendMessage(remoteJid, {
            text: result.message
        });
    }
}

export default MessageHandler;