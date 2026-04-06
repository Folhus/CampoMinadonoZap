import java.io.*;
import java.util.*;

public class GerenciadorJogos {

    private static GerenciadorJogos instancia;
    private Map<String, CampoMinado> jogosAtivos;
    private static final String ARQUIVO_JOGOS = "jogos_ativos.dat";

    private GerenciadorJogos() {
        this.jogosAtivos = new HashMap<>();
    }

    /**
     * Retorna a instância singleton do gerenciador
     */
    public static GerenciadorJogos getInstance() {
        if (instancia == null) {
            instancia = new GerenciadorJogos();
        }
        return instancia;
    }

    /**
     * Adiciona um novo jogo ativo
     * @param chatId ID do chat do WhatsApp
     * @param jogo Instância do jogo
     */
    public void adicionarJogo(String chatId, CampoMinado jogo) {
        if (chatId == null || chatId.isEmpty()) {
            throw new IllegalArgumentException("ChatId não pode ser nulo ou vazio");
        }
        jogosAtivos.put(chatId, jogo);
        salvarJogos();
    }

    /**
     * Remove um jogo quando é finalizado
     * @param chatId ID do chat do WhatsApp
     */
    public void removerJogo(String chatId) {
        if (jogosAtivos.containsKey(chatId)) {
            jogosAtivos.remove(chatId);
            salvarJogos();
        }
    }

    /**
     * Obtém um jogo ativo
     * @param chatId ID do chat do WhatsApp
     * @return O jogo ou null se não existir
     */
    public CampoMinado obterJogo(String chatId) {
        return jogosAtivos.get(chatId);
    }

    /**
     * Verifica se existe um jogo ativo para um chat
     * @param chatId ID do chat do WhatsApp
     * @return true se existe um jogo ativo
     */
    public boolean existeJogo(String chatId) {
        return jogosAtivos.containsKey(chatId);
    }

    /**
     * Retorna todos os jogos ativos
     * @return Map com todos os jogos
     */
    public Map<String, CampoMinado> obterTodosJogos() {
        return new HashMap<>(jogosAtivos);
    }

    /**
     * Retorna a quantidade de jogos ativos
     * @return Número de jogos em andamento
     */
    public int totalJogosAtivos() {
        return jogosAtivos.size();
    }

    /**
     * Limpa todos os jogos
     */
    public void limparTodos() {
        jogosAtivos.clear();
        salvarJogos();
    }

    /**
     * Salva todos os jogos em um arquivo
     */
    public void salvarJogos() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(ARQUIVO_JOGOS))) {
            out.writeObject(jogosAtivos);
            System.out.println("Jogos salvos com sucesso. Total: " + jogosAtivos.size());
        } catch (IOException e) {
            System.err.println("Erro ao salvar jogos: " + e.getMessage());
        }
    }

    /**
     * Carrega todos os jogos de um arquivo
     */
    @SuppressWarnings("unchecked")
    public void carregarJogos() {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(ARQUIVO_JOGOS))) {
            jogosAtivos = (Map<String, CampoMinado>) in.readObject();
            System.out.println("Jogos carregados com sucesso. Total: " + jogosAtivos.size());
        } catch (FileNotFoundException e) {
            System.out.println("Arquivo de jogos não encontrado. Inicializando com mapa vazio.");
            jogosAtivos = new HashMap<>();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erro ao carregar jogos: " + e.getMessage());
            jogosAtivos = new HashMap<>();
        }
    }

    /**
     * Retorna informações em JSON sobre todos os jogos ativos
     * @return JSON com informações dos jogos
     */
    public String obterInfoJogosJSON() {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"totalJogos\": ").append(jogosAtivos.size()).append(",\n");
        json.append("  \"jogos\": [\n");

        List<String> chats = new ArrayList<>(jogosAtivos.keySet());
        for (int i = 0; i < chats.size(); i++) {
            String chatId = chats.get(i);
            CampoMinado jogo = jogosAtivos.get(chatId);
            json.append("    {\n");
            json.append("      \"chatId\": \"").append(chatId).append("\",\n");
            json.append("      \"gameOver\": ").append(jogo.isGameOver()).append(",\n");
            json.append("      \"vitoria\": ").append(jogo.isVitoria()).append("\n");
            json.append("    }");
            if (i < chats.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("  ]\n");
        json.append("}\n");
        return json.toString();
    }
}
