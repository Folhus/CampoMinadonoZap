import { CampoMinadoH } from './CampoMinadoH';
import { FormattedMessage } from '../utils/message';
import { proto } from 'baileys';
import { readFile } from 'fs/promises';
import { existsSync, statSync } from 'fs';
import 'dotenv/config';
const jsonfile = require('jsonfile');

const campoMinadoH = new CampoMinadoH();

/**
 * Health check para o request path
 * Verifica se o arquivo é acessível e pode ser lido/escrito
 * @returns true se o path está saudável, false caso contrário
 */
function isRequestPathHealthy(): boolean {
    try {
        const requestPath = process.env.REQUEST_PATH;
        
        // Validação básica: caminho não pode estar vazio
        if (!requestPath || requestPath.trim() === '') {
            console.error('REQUEST_PATH vazio ou não definido em .env');
            return false;
        }

        // Validação 1: Arquivo existe?
        if (!existsSync(requestPath)) {
            console.error(`REQUEST_PATH não encontrado: ${requestPath}`);
            return false;
        }

        // Validação 2: É um arquivo (não diretório)?
        const stat = statSync(requestPath);
        if (!stat.isFile()) {
            console.error(`REQUEST_PATH não é um arquivo válido: ${requestPath}`);
            return false;
        }

        const lido = jsonfile.readFile(requestPath).lido;
        for (let i = 0; lido==false && i <= 20; i++) {
            new Promise(resolve => setTimeout(resolve, 100));
        }
        if (lido==false) {
            console.error('CampoMinado desligado ou em manutenção');
            return false;
        }

        // Validação 3: Tenta ler o arquivo (teste de leitura)
        try {
            const data = jsonfile.readFileSync(requestPath);
            if (!data) {
                console.warn('REQUEST_PATH existe mas está vazio ou inválido');
                return false;
            }
        } catch (readError: any) {
            console.error(`Erro ao ler REQUEST_PATH: ${readError.message}`);
            return false;
        }

        // Todos os testes passaram
        console.log(`REQUEST_PATH está saudável: ${requestPath}`);
        return true;
    } catch (error: any) {
        console.error(`Erro crítico no health check: ${error.message}`);
        return false;
    }
}

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

    // Health check: verifica se o requestPath está acessível antes de processar
    if (!isRequestPathHealthy()) {
        console.error('Sistema de ponte indisponível - ignorando comando');
        await sock.sendMessage(remoteJid, {
            text: "Sistema indisponível no momento. Tente novamente mais tarde."
        });
        return;
    }

    await handleCampoMinado(sock, remoteJid, content);
};


// Tudo relacionado ao campoMinado
async function handleCampoMinado(sock: any, remoteJid: string, content: string) {

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
}

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
    const args = content.split(' ').slice(1).join(' ');

    if (args.length < 2) {
        await sock.sendMessage(remoteJid, {
            text: "Use: !jogar <coordenada>\nExemplo: !jogar A1"
        });
        return;
    }

    const jogada = args.toUpperCase();
    const result = await campoMinadoH.fazerJogada(remoteJid, jogada);

    await sendGameResponse(sock, remoteJid, result);
}

async function handleBandeira(sock: any, remoteJid: string, content: string) {
    const args = content.split(' ').slice(1).join(' ');

    if (args.length < 2) {
        await sock.sendMessage(remoteJid, {
            text: "Use: !bandeira <coordenada>\nExemplo: !bandeira A1"
        });
        return;
    }

    const jogada = args.toUpperCase();
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