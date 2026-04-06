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

    private boolean checarAcao() {
        if (Jogo.gi().dados.comando.id.isEmpty() || Jogo.gi().dados.comando==null) {
            return false;
        } else {
            return true;
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
            FileWriter escritor = new FileWriter("request.json");
            escritor.write("{lido: true,\r\n + jogo: chatId,\r\n + comando: {\r\n + id: %,\r\n + coordenadas: coords");
            escritor.close();
        } catch (IOException e) {
            System.err.println("Erro ao limpar request.json: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        do {
            try {
                Jogo.gi().lerJson();
                Thread.sleep(500);
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
                                    GerenciadorJogos.getInstance().removerJogo(chatIdJogar);
                                    FileWriter escritor = new FileWriter("ponte.json");
                                    escritor.write("{\n\"comando\": [\n\"GAMEOVER\"\n]\n}");
                                    escritor.close();
                                } else if (jogoCarregado.isVitoria()) {
                                    GerenciadorJogos.getInstance().removerJogo(chatIdJogar);
                                    FileWriter escritor = new FileWriter("ponte.json");
                                    escritor.write("{\n\"comando\": [\n\"VITORIA\"\n]\n}");
                                    escritor.close();
                                } else {
                                    FileWriter escritor = new FileWriter("ponte.json");
                                    escritor.write("{\n\"comando\": [\n\"JOGADA_REALIZADA\"\n]\n}");
                                    escritor.close();
                                }
                            } else {
                                FileWriter escritor = new FileWriter("ponte.json");
                                escritor.write("{\n\"comando\": [\n\"JOGADA_INVALIDA\"\n]\n}");
                                escritor.close();
                            }
                        } else {
                            System.out.println("Crie um jogo primeiro");
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
                            FileWriter escritor = new FileWriter("ponte.json");
                            escritor.write(sucesso ? "{\n\"comando\": [\n\"BANDEIRA_ALTERADA\"\n]\n}"
                                    : "{\n\"comando\": [\n\"BANDEIRA_INVALIDA\"\n]\n}");
                            escritor.close();
                        } else {
                            System.out.println("Crie um jogo primeiro");
                        }
                    }
                    break;

                case "status":
                    Jogo.gi().limparJson();
                    String chatIdStatus = Jogo.gi().dados.jogo;
                    CampoMinado jogoStatus = GerenciadorJogos.getInstance().obterJogo(chatIdStatus);
                    if (jogoStatus != null) {
                        if (jogoStatus.isGameOver()) {
                            FileWriter escritor = new FileWriter("response.json");
                                escritor.write("{\n\"comando\": [\n\"STATUS_GAMER_OVER\"\n]\n}");
                                escritor.close();
                        } else if (jogoStatus.isVitoria()) {
                            FileWriter escritor = new FileWriter("response.json");
                                escritor.write("{\n\"comando\": [\n\"STATUS_VITORIA\"\n]\n}");
                                escritor.close();
                        } else {
                            FileWriter escritor = new FileWriter("response.json");
                                escritor.write("{\n\"comando\": [\n\"STATUS_EM_ANDAMENTO\"\n]\n}");
                                escritor.close();
                        }
                    } else {
                        FileWriter escritor = new FileWriter("response.json");
                                escritor.write("{\n\"comando\": [\n\"STATUS_SEM_JOGO\"\n]\n}");
                                escritor.close();
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