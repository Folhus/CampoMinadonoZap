import { exec } from 'child_process';
import { promisify } from 'util';
import { readFile } from 'fs/promises';

const jsonfile = require('jsonfile');
const file = '/tmp/data.json';
const writeFile = require('jsonfile').writeFile;

export class CampoMinadoH {
    private javaPath: string;
    private baseImagePath: string;
    private pontePath: string;
    private games: Map<string, string> = new Map(); // Mapa para guardar jogos por chat/grupo

    constructor() {
        this.javaPath = 'C:/Users/wilto/Desktop/Programa/Projeto/CampoMinadonoZap/CampoMinado/bin';
        this.baseImagePath = 'C:/Users/wilto/Desktop/Programa/Projetos/CampoMinadonoZap/CampoMinado/midia';
        this.pontePath = 'C:/Users/wilto/Desktop/Programa/Projetos/CampoMinadonoZap/CampoMinado/ponte.json'
    }

    private getImagePath(chatId: string): string {
        const safeId = chatId.replace(/[^a-zA-Z0-9]/g, '_');
        return `${this.baseImagePath}/campo.png`;
    }

    async novoJogo(chatId: string): Promise<{ success: boolean; message: string; imagePath?: string }> {
        try {
            const imagePath = this.getImagePath(chatId);
            const obj = { 'comando': ['novo']};
            jsonfile.writeFile(this.pontePath, obj);

            // Aguarda um momento para garantir que a imagem foi gerada
            await new Promise(resolve => setTimeout(resolve, 1000));

            // Guarda o jogo atual para este chat
            this.games.set(chatId, imagePath);

            return {
                success: true,
                message: "NOVO JOGO INICIADO!\n\nCampo: 14x14\nMinas: 28\nUse: !jogar A1\nBandeira: !bandeira A1",
                imagePath: imagePath
            };
        } catch (error: any) {
            console.error('Erro na ponte', error);
            return {
                success: false,
                message: `Erro ao criar novo jogo: ${error.message}`
            };
        }
    }

    async fazerJogada(chatId: string, jogada: string): Promise<{ success: boolean; message: string; imagePath?: string }> {
        // Separa múltiplas coordenadas por espaço e/ou vírgula
        console.log(jogada);
        const coordenadas = jogada.trim().split(/[\s,]+/).filter(c => c.length > 0);
        const comando: string[] = ['jogar'];
        console.log(coordenadas);

        let i = 1;
        let temp = "";
        for (const coord of coordenadas) {
            const match = coord.match(/^([A-N])(\d+)$/i);
            if (!match) {
                return {
                    success: false,
                    message: "Formato invalido! Use letra (A-N) e numero (1-14)\nExemplo: A1, B3, M14"
                };
            }
            while(i < coord.length){
                if(i%2 == 0){
                    temp = coord[i-1].toUpperCase() + coord[i];
                    comando.push(temp);
                    console.log(temp);
                } 
                i++;
                console.log(match[i]);
            }
        }

        // Verifica se existe um jogo ativo para este chat
        const imagePath = this.games.get(chatId);
        if (!imagePath) {
            return {
                success: false,
                message: "Nenhum jogo ativo! Use !novo para iniciar um jogo."
            };
        }

        try {
            const obj = { 'comando': comando };
            jsonfile.writeFileSync(this.pontePath, obj)

            await new Promise(resolve => setTimeout(resolve, 1000));

            if (jsonfile.readFile(this.pontePath).Comando == "GAMEOVER") {
                // Remove o jogo quando termina
                this.games.delete(chatId);
                return {
                    success: true,
                    message: "GAME OVER! Voce atingiu uma mina!\nUse !novo para jogar novamente",
                    imagePath: imagePath
                };
            } else if (jsonfile.readFile(this.pontePath).Comando == "VITORIA") {
                // Remove o jogo quando vence
                this.games.delete(chatId);
                return {
                    success: true,
                    message: "PARABENS! Voce venceu o Campo Minado!\nUse !novo para jogar novamente",
                    imagePath: imagePath
                };
            } else if (jsonfile.readFile(this.pontePath).Comando == "JOGADA_REALIZADA") {
                return {
                    success: true,
                    message: `Jogadas realizadas!\nContinue jogando...`,
                    imagePath: imagePath
                };
            } else if (jsonfile.readFile(this.pontePath).Comando == "JOGADA_INVALIDA") {
                return {
                    success: false,
                    message: "Jogada invalida!\nCelula ja revelada ou com bandeira."
                };
            } else {
                return {
                    success: true,
                    message: `Jogadas realizadas!`,
                    imagePath: imagePath
                };
            }
        } catch (error: any) {
            return {
                success: false,
                message: `Erro ao processar jogada: ${error.message}`
            };
        }
    }


    async toggleBandeira(chatId: string, jogada: string): Promise<{ success: boolean; message: string; imagePath?: string }> {
        const match = jogada.match(/^([A-Z])(\d+)$/i);
        if (!match) {
            return {
                success: false,
                message: "Formato invalido! Use letra (A-N) e numero (1-14)\nExemplo: A1, B3, M14"
            };
        }

        const linha = match[1].toUpperCase();
        const coluna = match[2];

        if (linha < 'A' || linha > 'N' || parseInt(coluna) < 1 || parseInt(coluna) > 14) {
            return {
                success: false,
                message: "Coordenada fora do campo!\nLinhas: A ate N\nColunas: 1 ate 14"
            };
        }

        try {
            // Verifica se existe um jogo ativo para este chat
            const imagePath = this.games.get(chatId);
            if (!imagePath) {
                return {
                    success: false,
                    message: "Nenhum jogo ativo! Use !novo para iniciar um jogo."
                };
            }

            const obj = { 'Comando': ['jogar', match] };
            jsonfile.writeFileSync(this.pontePath, obj);

            if (jsonfile.readFile(this.pontePath).Comando == "BANDEIRA_ALTERADA") {
                return {
                    success: true,
                    message: `Bandeira colocada/removida!`,
                    imagePath
                };
            } else {
                return {
                    success: false,
                    message: "Nao foi possível colocar bandeira!\nCelula ja revelada."
                };
            }
        } catch (error: any) {
            return {
                success: false,
                message: `Erro ao processar bandeira: ${error.message}`
            };
        }
    }

    async getImageBuffer(imagePath: string): Promise<Buffer | null> {
        try {
            return await readFile(imagePath);
        } catch (error) {
            console.error('Erro ao ler arquivo de imagem:', error);
            return null;
        }
    }

    async verStatus(chatId: string): Promise<{ success: boolean; message: string; imagePath?: string }> {
        try {
            // Verifica se existe um jogo ativo para este chat
            const imagePath = this.games.get(chatId);
            if (!imagePath) {
                return {
                    success: false,
                    message: "Nenhum jogo ativo! Use !novo para iniciar um jogo."
                };
            }

            const obj = { 'Comando': ['status'] };
            jsonfile.writeFile(this.pontePath, obj);

            await new Promise(resolve => setTimeout(resolve, 1000));
            if (jsonfile.readFile(this.pontePath).Comando == "STATUS_GAME_OVER") {
                return {
                    success: true,
                    message: "GAME OVER\nUse !novo para jogar novamente",
                    imagePath
                };
            } else if (jsonfile.readFile(this.pontePath).Comando == "STATUS_VITORIA") {
                return {
                    success: true,
                    message: "VITORIA!\nUse !novo para jogar novamente",
                    imagePath
                };
            } else if (jsonfile.readFile(this.pontePath).Comando == "STATUS_EM_ANDAMENTO") {
                return {
                    success: true,
                    message: "JOGO EM ANDAMENTO\nContinue jogando!",
                    imagePath
                };
            } else {
                return {
                    success: false,
                    message: "Nenhum jogo em andamento!\nUse !novo para comecar."
                };
            }
        } catch (error: any) {
            return {
                success: false,
                message: `Erro ao ver status: ${error.message}`
            };
        }
    }

}