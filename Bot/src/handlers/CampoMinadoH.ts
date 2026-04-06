import 'dotenv/config';
import { readFile } from 'fs/promises';
import { writeFileSync, existsSync, statSync } from 'fs';

const jsonfile = require('jsonfile');
const file = '/tmp/data.json';
const writeFile = require('jsonfile').writeFile;

export class CampoMinadoH {
    private baseImagePath: string;
    private requestPath: string;
    private responsePath: string;

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
        const obj = {
            lido: false,
            jogo: chatId,
            comando: {
                id: comando,
                coordenadas: coords
            }
        };
        // Reescreve request.json com o objeto obj
        writeFileSync(this.requestPath, JSON.stringify(obj, null, 4));

        // Aguarda um momento para garantir que a imagem foi gerada
        new Promise(resolve => setTimeout(resolve, 1000));
    }

    private async lerResponse() {
        let lido = false;
        let status = "";

        // Aguarda até 2 segundos pela resposta
        for (let i = 0; i < 20; i++) {
            await new Promise(resolve => setTimeout(resolve, 100));
            try {
                const data = await jsonfile.readFile(this.responsePath);
                lido = data.lido;
                status = data.status;
                if (lido) break;
            } catch (error) {
                continue;
            }
        }

        if (lido) {
            this.writeResponse();
            return status;
        } else {
            return "SEM_RESPOSTA";
        }
    }

    private writeResponse() {
        const obj = {
            lido: false,
            status: ""
        };

        writeFileSync(this.responsePath, JSON.stringify(obj, null, 2));
    }

    async novoJogo(chatId: string): Promise<{ success: boolean; message: string; imagePath?: string }> {
        try {
            const imagePath = this.getImagePath(chatId);

            this.writeRequest(chatId, "novo", [""]);

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
        console.log("jogada recebida:", JSON.stringify(jogada));
        const coordenadas = jogada.trim().split(/[\s,]+/).filter(c => c.length > 0);
        console.log("coordenadas split:", coordenadas);
        const comando: string[] = [];

        for (const coord of coordenadas) {
            const match = coord.match(/^([A-N])(\d+)$/i);
            if (!match) {
                return {
                    success: false,
                    message: "Formato invalido! Use letra (A-N) e numero (1-14)\nExemplo: A1, B3, M14"
                };
            }
            comando.push(match[1].toUpperCase() + match[2]);
        }


        try {
            const imagePath = this.getImagePath(chatId);

            this.writeRequest(chatId, "jogar", comando);

            const status = await this.lerResponse();

            if (status == "GAMEOVER") {
                return {
                    success: true,
                    message: "GAME OVER! Voce atingiu uma mina!\nUse !novo para jogar novamente",
                    imagePath: imagePath
                };
            } else if (status == "VITORIA") {
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
            } else if (status == "JOGO_INEXISTENTE") {
                return {
                    success: false,
                    message: "Nenhum jogo ativo! Use !novo para iniciar um jogo."
                };
            } else if (status == "SEM_RESPOSTA") {
                return {
                    success: false,
                    message: "Campo minado inativo/em manutenção."
                }
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
        const comando: string[] = [];

        for (const coord of coordenadas) {
            const match = coord.match(/^([A-N])(\d+)$/i);
            if (!match) {
                return {
                    success: false,
                    message: "Formato invalido! Use letra (A-N) e numero (1-14)\nExemplo: A1, B3, M14"
                };
            }
            comando.push(match[1].toUpperCase() + match[2]);
        }


        try {
            const imagePath = this.getImagePath(chatId);

            this.writeRequest(chatId, "bandeira", comando);

            const status = await this.lerResponse();

            if (status == "BANDEIRA_ALTERADA") {
                return {
                    success: true,
                    message: `Bandeira colocada/removida!`,
                    imagePath: imagePath
                };
            } else if (status == "JOGO_INEXISTENTE") {
                return {
                    success: false,
                    message: "Nenhum jogo ativo! Use !novo para iniciar um jogo."
                };
            } else if (status == "SEM_RESPOSTA") {
                return {
                    success: false,
                    message: "Campo minado inativo/em manutenção."
                }
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

    async verStatus(chatId: string): Promise<{ success: boolean; message: string; imagePath?: string }> {
        try {
            const imagePath = this.getImagePath(chatId);

            this.writeRequest(chatId, "status", [""]);

            const status = await this.lerResponse();

            if (status == "STATUS_GAME_OVER") {
                return {
                    success: true,
                    message: "GAME OVER\nUse !novo para jogar novamente",
                    imagePath: imagePath
                };
            } else if (status == "STATUS_VITORIA") {
                return {
                    success: true,
                    message: "VITORIA!\nUse !novo para jogar novamente",
                    imagePath: imagePath
                };
            } else if (status == "STATUS_EM_ANDAMENTO") {
                return {
                    success: true,
                    message: "JOGO EM ANDAMENTO\nContinue jogando!",
                    imagePath: imagePath
                };
            } else if (status == "SEM_RESPOSTA") {
                return {
                    success: false,
                    message: "Campo minado inativo/em manutenção."
                }
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

    async getImageBuffer(imagePath: string): Promise<Buffer | null> {
        try {
            // Aguarda um pouco para garantir que o arquivo foi completamente escrito
            await new Promise(resolve => setTimeout(resolve, 500));
            
            // Verifica se o arquivo existe e é válido
            if (!existsSync(imagePath)) {
                console.error(`Arquivo de imagem não encontrado: ${imagePath}`);
                return null;
            }
            
            const stat = statSync(imagePath);
            if (stat.size === 0) {
                console.error(`Arquivo de imagem vazio: ${imagePath}`);
                return null;
            }
            
            return await readFile(imagePath);
        } catch (error) {
            console.error('Erro ao ler arquivo de imagem:', error);
            return null;
        }
    }

}