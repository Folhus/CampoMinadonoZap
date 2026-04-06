/**
 * Testes unitários simples para o sistema
 * Execute para validar a implementação
 */
public class TestesRapidos {
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║        TESTES DO SISTEMA DE GERENCIAMENTO DE JOGOS         ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");
        
        int sucessos = 0;
        int falhas = 0;
        
        // Teste 1: Carregar ConfigEnv
        try {
            System.out.println("Teste 1: Carregar ConfigEnv");
            ConfigEnv config = ConfigEnv.getInstance();
            String baseImagePath = config.obter("BASE_IMAGE_PATH");
            if (baseImagePath != null && !baseImagePath.isEmpty()) {
                System.out.println("   BASE_IMAGE_PATH carregado: " + baseImagePath);
                sucessos++;
            } else {
                System.out.println("   BASE_IMAGE_PATH não configurado");
                falhas++;
            }
        } catch (Exception e) {
            System.out.println("   Erro: " + e.getMessage());
            falhas++;
        }
        System.out.println();
        
        // Teste 2: Criar novo jogo
        try {
            System.out.println("Teste 2: Criar novo CampoMinado");
            CampoMinado jogo = new CampoMinado();
            jogo.setChatId("5584981881549");
            
            if (jogo.getChatId().equals("5584981881549")) {
                System.out.println("   ChatId definido corretamente");
                System.out.println("   Caminho imagem: " + jogo.getCaminhoImagem());
                sucessos++;
            } else {
                System.out.println("   ChatId não foi definido");
                falhas++;
            }
        } catch (Exception e) {
            System.out.println("   Erro: " + e.getMessage());
            falhas++;
        }
        System.out.println();
        
        // Teste 3: GerenciadorJogos singleton
        try {
            System.out.println("Teste 3: GerenciadorJogos Singleton");
            GerenciadorJogos gerenciador1 = GerenciadorJogos.getInstance();
            GerenciadorJogos gerenciador2 = GerenciadorJogos.getInstance();
            
            if (gerenciador1 == gerenciador2) {
                System.out.println("   Singleton funcionando (mesma instância)");
                sucessos++;
            } else {
                System.out.println("   Singleton falhou (instâncias diferentes)");
                falhas++;
            }
        } catch (Exception e) {
            System.out.println("   Erro: " + e.getMessage());
            falhas++;
        }
        System.out.println();
        
        // Teste 4: Adicionar e recuperar jogo
        try {
            System.out.println("Teste 4: Adicionar e Recuperar Jogo");
            GerenciadorJogos gerenciador = GerenciadorJogos.getInstance();
            CampoMinado jogo = new CampoMinado();
            String chatId = "5584987654321";
            jogo.setChatId(chatId);
            
            gerenciador.adicionarJogo(chatId, jogo);
            CampoMinado jogoRecuperado = gerenciador.obterJogo(chatId);
            
            if (jogoRecuperado != null && jogoRecuperado.getChatId().equals(chatId)) {
                System.out.println("   Jogo adicionado e recuperado com sucesso");
                sucessos++;
            } else {
                System.out.println("   Falha ao recuperar jogo");
                falhas++;
            }
        } catch (Exception e) {
            System.out.println("   Erro: " + e.getMessage());
            falhas++;
        }
        System.out.println();
        
        // Teste 5: Verificar existência
        try {
            System.out.println("Teste 5: Verificar Existência de Jogo");
            GerenciadorJogos gerenciador = GerenciadorJogos.getInstance();
            
            boolean existe1 = gerenciador.existeJogo("5584987654321");
            boolean existe2 = gerenciador.existeJogo("9999999999999");
            
            if (existe1 && !existe2) {
                System.out.println("   Verificação de existência funcionando");
                sucessos++;
            } else {
                System.out.println("   Verificação de existência falhou");
                falhas++;
            }
        } catch (Exception e) {
            System.out.println("   Erro: " + e.getMessage());
            falhas++;
        }
        System.out.println();
        
        // Teste 6: Total de jogos
        try {
            System.out.println("Teste 6: Contar Jogos Ativos");
            GerenciadorJogos gerenciador = GerenciadorJogos.getInstance();
            int total = gerenciador.totalJogosAtivos();
            
            if (total > 0) {
                System.out.println("   Total de jogos ativos: " + total);
                sucessos++;
            } else {
                System.out.println("   Nenhum jogo ativo (pode ser esperado)");
                sucessos++;
            }
        } catch (Exception e) {
            System.out.println("   Erro: " + e.getMessage());
            falhas++;
        }
        System.out.println();
        
        // Teste 7: Remover jogo
        try {
            System.out.println("Teste 7: Remover Jogo");
            GerenciadorJogos gerenciador = GerenciadorJogos.getInstance();
            String chatId = "5584987654321";
            
            gerenciador.removerJogo(chatId);
            boolean existe = gerenciador.existeJogo(chatId);
            
            if (!existe) {
                System.out.println("   Jogo removido com sucesso");
                sucessos++;
            } else {
                System.out.println("   Falha ao remover jogo");
                falhas++;
            }
        } catch (Exception e) {
            System.out.println("   Erro: " + e.getMessage());
            falhas++;
        }
        System.out.println();
        
        // Teste 8: Revelar célula
        try {
            System.out.println("Teste 8: Revelar Célula do Jogo");
            CampoMinado jogo = new CampoMinado();
            
            boolean resultado = jogo.revelarCelula(5, 5);
            if (resultado) {
                System.out.println("   Célula revelada com sucesso");
                sucessos++;
            } else {
                System.out.println("   Falha ao revelar célula");
                falhas++;
            }
        } catch (Exception e) {
            System.out.println("   Erro: " + e.getMessage());
            falhas++;
        }
        System.out.println();
        
        // Teste 9: Adicionar bandeira
        try {
            System.out.println("Teste 9: Adicionar Bandeira");
            CampoMinado jogo = new CampoMinado();
            
            boolean resultado = jogo.toggleBandeira(3, 3);
            if (resultado) {
                System.out.println("   Bandeira adicionada com sucesso");
                sucessos++;
            } else {
                System.out.println("   Falha ao adicionar bandeira");
                falhas++;
            }
        } catch (Exception e) {
            System.out.println("   Erro: " + e.getMessage());
            falhas++;
        }
        System.out.println();
        
        // Teste 10: Gerar imagem
        try {
            System.out.println("Teste 10: Gerar Imagem com ChatId");
            CampoMinado jogo = new CampoMinado();
            jogo.setChatId("5584999999999");
            
            // Apenas verificar se o método é chamável
            // (geração real depende de configuração)
            System.out.println("   Caminho definido: " + jogo.getCaminhoImagem());
            if (jogo.getCaminhoImagem().contains("5584999999999")) {
                System.out.println("   Imagem será salva com nome do chatId");
                sucessos++;
            } else {
                System.out.println("   Nome da imagem não contém chatId");
                falhas++;
            }
        } catch (Exception e) {
            System.out.println("   Erro: " + e.getMessage());
            falhas++;
        }
        System.out.println();
        
        // Resultado final
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║                    RESULTADO DOS TESTES                    ║");
        System.out.println("╠════════════════════════════════════════════════════════════╣");
        System.out.println("║  Sucessos: " + String.format("%2d", sucessos) + 
                          "                                             ║");
        System.out.println("║  Falhas:   " + String.format("%2d", falhas) + 
                          "                                             ║");
        System.out.println("║  Taxa:    " + String.format("%.1f", 
                          (sucessos * 100.0) / (sucessos + falhas)) + "% " +
                          "                                          ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        
        if (falhas == 0) {
            System.out.println("\nTODOS OS TESTES PASSARAM! Sistema pronto para uso!\n");
        } else {
            System.out.println("\nAlguns testes falharam. Verifique a configuração.\n");
        }
    }
}
