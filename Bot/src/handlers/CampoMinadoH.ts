import 'dotenv/config';
import { readFile } from 'fs/promises';

const jsonfile = require('jsonfile');
const file = '/tmp/data.json';
const writeFile = require('jsonfile').writeFile;

export class CampoMinadoH {
    private baseImagePath: string;
    private requestPath: string;
    private responsePath: string;
    private games: Map<string, string> = new Map(); // Mapa para guardar jogos por chat/grupo

    constructor() {
        this.baseImagePath = process.env.BASE_IMAGE_PATH || '';
        this.requestPath = process.env.REQUEST_PATH || '';
        this.responsePath = process.env.RESPONSE_PATH || '';
    }

    private getImagePath(chatId: string): string {
        return `${this.baseImagePath}/${chatId}.png`;
    }

    private writeRequest(chatId: string, comando: string, coords: string[]) {
        // Cria objeto com os elementos já preenchidos
        const obj = `{
                "lido": false,
                "jogo": ${chatId},
                "comando": {
                    "id": ${comando},
                    "coordenadas": ${coords}
                }
            }`;
        // Reescreve request.json com o objeto obj
        jsonfile.writeFileSync(this.requestPath, obj)

        // Aguarda um momento para garantir que a imagem foi gerada
        new Promise(resolve => setTimeout(resolve, 1000));
    }

    async novoJogo(chatId: string): Promise<{ success: boolean; message: string; imagePath?: string }> {
        try {
            const imagePath = this.getImagePath(chatId);

            this.writeRequest(chatId, "novo", [""]);

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
        const comando: string[] = [''];
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
            while (i < coord.length) {
                if (i % 2 == 0) {
                    temp = coord[i - 1].toUpperCase() + coord[i];
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
            this.writeRequest(chatId, "jogar", comando);

            const status = jsonfile.readFile(this.responsePath).status;

            if (status == "GAMEOVER") {
                // Remove o jogo quando termina
                this.games.delete(chatId);
                return {
                    success: true,
                    message: "GAME OVER! Voce atingiu uma mina!\nUse !novo para jogar novamente",
                    imagePath: imagePath
                };
            } else if (status == "VITORIA") {
                // Remove o jogo quando vence
                this.games.delete(chatId);
                return {
                    success: true,
                    message: "PARABENS! Voce venceu o Campo Minado!\nUse !novo para jogar novamente",
                    imagePath: imagePath
                };
            } else if (status == "JOGADA_REALIZADA") {
                return {
                    success: true,
                    message: `Jogadas realizadas!\nContinue jogando...`,
                    imagePath: imagePath
                };
            } else if (status == "JOGADA_INVALIDA") {
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
        console.log(jogada);
        const coordenadas = jogada.trim().split(/[\s,]+/).filter(c => c.length > 0);
        const comando: string[] = [''];
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
            while (i < coord.length) {
                if (i % 2 == 0) {
                    temp = coord[i - 1].toUpperCase() + coord[i];
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
            this.writeRequest(chatId, "bandeira", comando);

            if (jsonfile.readFile(this.responsePath).status == "BANDEIRA_ALTERADA") {
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

            this.writeRequest(chatId, "status", [""]);

            const status = jsonfile.readFile(this.responsePath).status;

            if (status == "STATUS_GAME_OVER") {
                return {
                    success: true,
                    message: "GAME OVER\nUse !novo para jogar novamente",
                    imagePath
                };
            } else if (status == "STATUS_VITORIA") {
                return {
                    success: true,
                    message: "VITORIA!\nUse !novo para jogar novamente",
                    imagePath
                };
            } else if (status == "STATUS_EM_ANDAMENTO") {
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