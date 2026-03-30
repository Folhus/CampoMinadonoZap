import java.io.*;
import com.google.gson.Gson;

public class Jogo {

    private static Jogo game;

    private Jogo() {
    }

    private static Jogo gi() {
        if (game == null) {
            game = new Jogo();
        }

        return game;
    }

    public Dados dados = new Dados();

    private boolean checarAcao() {
        if (Jogo.gi().dados.comando.isEmpty() || Jogo.gi().dados.comando==null) {
            return false;
        } else {
            return true;
        }
    }

    private void lerJson() {
        Gson gson = new Gson();
        try {
            BufferedReader leitor = new BufferedReader(new FileReader("ponte.json"));
            Jogo.gi().dados = gson.fromJson(leitor, Dados.class);
            leitor.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void limparJson() {
        try {
            FileWriter escritor = new FileWriter("ponte.json");
            escritor.write("{\n\"comando\": [\n\n]\n}");
            escritor.close();
        } catch (IOException e) {
            e.printStackTrace();
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

        System.out.println("Uso: java Jogo " + Jogo.gi().dados.comando);

        try {
            switch (Jogo.gi().dados.comando.get(0)) {
                case "novo":
                    Jogo.gi().limparJson();
                    CampoMinado jogo = new CampoMinado();
                    salvarJogo(jogo, "jogo_salvo.ser");
                    System.out.println("Jogo criado! Use !jogar A1 para começar.");
                    break;

                case "jogar":
                    Jogo.gi().limparJson();
                    for (int i = 1; i < Jogo.gi().dados.comando.size(); i++) {
                        String coordenada = Jogo.gi().dados.comando.get(i).toUpperCase();
                        int L = coordenada.charAt(0) - 'A';
                        int C = Integer.parseInt(coordenada.substring(1)) - 1;

                        CampoMinado jogoCarregado = carregarJogo("jogo_salvo.ser");
                        if (jogoCarregado != null) {
                            boolean sucesso = jogoCarregado.revelarCelula(L, C);
                            if (sucesso) {
                                salvarJogo(jogoCarregado, "jogo_salvo.ser");
                                if (jogoCarregado.isGameOver()) {
                                    FileWriter escritor = new FileWriter("ponte.json");
                                    escritor.write("{\n\"comando\": [\n\"GAMEOVER\"\n]\n}");
                                    escritor.close();
                                } else if (jogoCarregado.isVitoria()) {
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
                    for (int i = 1; i < Jogo.gi().dados.comando.size(); i++) {
                        String coordBandeira = Jogo.gi().dados.comando.get(i).toUpperCase();
                        int LB = coordBandeira.charAt(0) - 'A';
                        int CB = Integer.parseInt(coordBandeira.substring(1)) - 1;

                        CampoMinado jogoBandeira = carregarJogo("jogo_salvo.ser");
                        if (jogoBandeira != null) {
                            boolean sucesso = jogoBandeira.toggleBandeira(LB, CB);
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
                    CampoMinado jogoStatus = carregarJogo("jogo_salvo.ser");
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
            CampoMinado jogoImagem = carregarJogo("jogo_salvo.ser");
            if (jogoImagem != null) {
                File imagem = new File(
                        "C:/Users/wilto/Desktop/Programa/Projetos/CampoMinadonoZap/CampoMinado/midia/campo.png");
                if (imagem.exists()) {
                    imagem.delete();
                }
                jogoImagem.gerarImagem();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Jogo.gi().main(null);
    }

    private static void salvarJogo(CampoMinado jogo, String arquivo) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(arquivo))) {
            out.writeObject(jogo);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static CampoMinado carregarJogo(String arquivo) {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(arquivo))) {
            return (CampoMinado) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }
}