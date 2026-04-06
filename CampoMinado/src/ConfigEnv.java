import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConfigEnv {
    
    private static ConfigEnv instancia;
    private Map<String, String> variaveis;
    private static final String ARQUIVO_ENV = ".env";

    private ConfigEnv() {
        variaveis = new HashMap<>();
        carregarEnv();
    }

     //Retorna a instância singleton do gerenciador de configurações
    public static ConfigEnv getInstance() {
        if (instancia == null) {
            instancia = new ConfigEnv();
        }
        return instancia;
    }

    // Carrega as variáveis do arquivo .env
    private void carregarEnv() {
        try (BufferedReader leitor = new BufferedReader(new FileReader(ARQUIVO_ENV))) {
            String linha;
            while ((linha = leitor.readLine()) != null) {
                linha = linha.trim();
                // Ignorar linhas vazias e comentários
                if (linha.isEmpty() || linha.startsWith("#")) {
                    continue;
                }
                
                // Dividir por = para pegar chave e valor
                int indice = linha.indexOf('=');
                if (indice > 0) {
                    String chave = linha.substring(0, indice).trim();
                    String valor = linha.substring(indice + 1).trim();
                    variaveis.put(chave, valor);
                }
            }
            System.out.println("Arquivo .env carregado com sucesso. Total de variáveis: " + variaveis.size());
        } catch (IOException e) {
            System.err.println("Erro ao carregar arquivo .env: " + e.getMessage());
        }
    }

    /**
     * Obtém o valor de uma variável de ambiente
     * @param chave Nome da variável
     * @return Valor da variável ou null se não existir
     */
    public String obter(String chave) {
        return variaveis.get(chave);
    }

    /**
     * Obtém o valor de uma variável com valor padrão
     * @param chave Nome da variável
     * @param padrao Valor padrão se não existir
     * @return Valor da variável ou o padrão
     */
    public String obter(String chave, String padrao) {
        return variaveis.getOrDefault(chave, padrao);
    }

    /**
     * Verifica se uma variável existe
     * @param chave Nome da variável
     * @return true se existe
     */
    public boolean existe(String chave) {
        return variaveis.containsKey(chave);
    }

    /**
     * Retorna todas as variáveis carregadas
     * @return Mapa com todas as variáveis
     */
    public Map<String, String> obterTodas() {
        return new HashMap<>(variaveis);
    }
}
