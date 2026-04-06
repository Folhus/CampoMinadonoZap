import java.io.*;
import com.google.gson.Gson;
import java.util.Map;

public class Jogo {

    private static Jogo game;

    private Jogo() {
        // Carregar configurações e jogos salvos ao inicializar
        ConfigEnv.getInstance();
        GerenciadorJogos.getInstance().carregarJogos();
    }

    private static Jogo gi() {
        if (game == null) {
            game = new Jogo();
        }

        return game;
    }

    public Dados dados = new Dados();
    public Response response = new Response();

    private boolean checarAcao() {
        if (Jogo.gi().dados.comando.id.isEmpty() || Jogo.gi().dados.comando==null) {
            return false;
        } else {
            return true;
        }
    }

    private void limparResponse (String status) {
        try {
            Gson gson = new Gson();
            Response dadosLimpo = new Response();
            dadosLimpo.lido = true;
            dadosLimpo.status = status;
            
            FileWriter escritor = new FileWriter("response.json");
            gson.toJson(dadosLimpo, escritor);
            escritor.close();
        } catch (IOException e) {
            System.err.println("Erro ao limpar response.json: " + e.getMessage());
        }
    }

    private void lerJson() {
        Gson gson = new Gson();
        try {
            BufferedReader leitor = new BufferedReader(new FileReader("request.json"));
            Jogo.gi().dados = gson.fromJson(leitor, Dados.class);
            leitor.close();
        } catch (IOException e) {
            System.err.println("Erro ao ler request.json: " + e.getMessage());
        }
    }

    private void limparJson() {
        try {
            Gson gson = new Gson();
            Dados dadosLimpo = new Dados();
            dadosLimpo.lido = true;
            dadosLimpo.jogo = "";
            dadosLimpo.comando = new Comando();
            dadosLimpo.comando.id = "";
            dadosLimpo.comando.coordenadas = new java.util.ArrayList<>();
            
            FileWriter escritor = new FileWriter("request.json");
            gson.toJson(dadosLimpo, escritor);
            escritor.close();
        } catch (IOException e) {
            System.err.println("Erro ao limpar request.json: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        do {
            try {
                Jogo.gi().lerJson();
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } while (Jogo.gi().checarAcao() == false);

        System.out.println("Uso: java Jogo " + Jogo.gi().dados.jogo);



        try {
            switch (Jogo.gi().dados.comando.id) {
                case "novo":
                    Jogo.gi().limparJson();
                    CampoMinado jogo = new CampoMinado();
                    String chatId = Jogo.gi().dados.jogo;
                    jogo.setChatId(chatId);

                    if (GerenciadorJogos.getInstance().existeJogo(chatId) == true) {
                        GerenciadorJogos.getInstance().removerJogo(chatId);
                    }

                    GerenciadorJogos.getInstance().adicionarJogo(chatId, jogo);
                    salvarJogo(jogo, "jogo_salvo.ser");
                    System.out.println("Jogo criado para chat: " + chatId);
                    System.out.println("Jogo criado! Use !jogar A1 para começar.");
                    break;

                case "jogar":
                    Jogo.gi().limparJson();
                    String chatIdJogar = Jogo.gi().dados.jogo;
                    for (String coordenada : Jogo.gi().dados.comando.coordenadas) {
                        coordenada = coordenada.toUpperCase();
                        int L = coordenada.charAt(0) - 'A';
                        int C = Integer.parseInt(coordenada.substring(1)) - 1;

                        CampoMinado jogoCarregado = GerenciadorJogos.getInstance().obterJogo(chatIdJogar);
                        if (jogoCarregado != null) {
                            boolean sucesso = jogoCarregado.revelarCelula(L, C);
                            if (sucesso) {
                                GerenciadorJogos.getInstance().adicionarJogo(chatIdJogar, jogoCarregado);
                                salvarJogo(jogoCarregado, "jogo_salvo.ser");
                                if (jogoCarregado.isGameOver()) {
                                    Jogo.gi().limparResponse("GAMEOVER");
                                } else if (jogoCarregado.isVitoria()) {
                                    Jogo.gi().limparResponse("VITORIA");
                                } else {
                                    Jogo.gi().limparResponse("JOGADA_REALIZADA");
                                }
                            } else {
                                Jogo.gi().limparResponse("JOGADA_INVALIDA");
                            }
                        } else {
                            Jogo.gi().limparResponse("JOGO_INEXISTENTE");
                        }
                    }
                    break;

                case "bandeira":
                    Jogo.gi().limparJson();
                    String chatIdBandeira = Jogo.gi().dados.jogo;
                    for (String coord : Jogo.gi().dados.comando.coordenadas) {
                        String coordBandeira = coord.toUpperCase();
                        int LB = coordBandeira.charAt(0) - 'A';
                        int CB = Integer.parseInt(coordBandeira.substring(1)) - 1;

                        CampoMinado jogoBandeira = GerenciadorJogos.getInstance().obterJogo(chatIdBandeira);
                        if (jogoBandeira != null) {
                            boolean sucesso = jogoBandeira.toggleBandeira(LB, CB);
                            GerenciadorJogos.getInstance().adicionarJogo(chatIdBandeira, jogoBandeira);
                            salvarJogo(jogoBandeira, "jogo_salvo.ser");
                            if (!sucesso) {
                                Jogo.gi().limparResponse("BANDEIRA_INVALIDA");
                            }else{
                                Jogo.gi().limparResponse("BANDEIRA_ALTERADA");
                            }
                        } else {
                            Jogo.gi().limparResponse("JOGO_INEXISTENTE");
                        }
                    }
                    break;

                case "status":
                    Jogo.gi().limparJson();
                    String chatIdStatus = Jogo.gi().dados.jogo;
                    CampoMinado jogoStatus = GerenciadorJogos.getInstance().obterJogo(chatIdStatus);
                    if (jogoStatus != null) {
                        if (jogoStatus.isGameOver()) {
                            Jogo.gi().limparResponse("STATUS_GAMER_OVER");
                        } else if (jogoStatus.isVitoria()) {
                            Jogo.gi().limparResponse("STATUS_VITORIA");
                        } else {
                            Jogo.gi().limparResponse("STATUS_EM_ANDAMENTO");
                        }
                    }
                    break;

            }
        } catch (Exception e) {
            System.out.println("ERRO: " + e.getMessage());
        }

        try {
            String chatIdGerador = Jogo.gi().dados.jogo;
            CampoMinado jogoImagem = GerenciadorJogos.getInstance().obterJogo(chatIdGerador);
            if (jogoImagem != null) {
                jogoImagem.setChatId(chatIdGerador);
                String caminhoImagem = jogoImagem.getCaminhoImagem();
                File imagem = new File(caminhoImagem);
                if (imagem.exists()) {
                    imagem.delete();
                }
                jogoImagem.gerarImagem();
                System.out.println("Imagem gerada para chat: " + chatIdGerador);
            }
        } catch (Exception e) {
            System.err.println("Erro ao gerar imagem: " + e.getMessage());
        }

        Jogo.gi().main(null);
    }

    private static void salvarJogo(CampoMinado jogo, String arquivo) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(arquivo))) {
            out.writeObject(jogo);
        } catch (IOException e) {
            System.err.println("Erro ao salvar jogo: " + e.getMessage());
        }
    }
}