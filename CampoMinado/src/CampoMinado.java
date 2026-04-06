import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.awt.Font;

public class CampoMinado implements Serializable {
    private static final long serialVersionUID = 1L;
    // variáveis do construtor
    int TamanhoQuadrado = 60, LinhasCampo = 14, ColunasCampo = 14;
    int AlturaCampo = LinhasCampo * TamanhoQuadrado;
    int LarguraCampo = ColunasCampo * TamanhoQuadrado;
    int TotalMinas = 28;

    // matriz do campo minado
    private CelulaEstado[][] celulas = new CelulaEstado[LinhasCampo][ColunasCampo];
    private ArrayList<Point> minas;
    private boolean gameOver = false;
    private boolean vitoria = false;

    // identificação do jogo
    private String chatId;
    
    // caminho para a imagem
    private String caminhoImagem;

    // Classe para guardar o estado de cada célula
    private static class CelulaEstado implements Serializable {
        private static final long serialVersionUID = 1L;
        boolean temMina = false;
        boolean revelado = false;
        boolean bandeira = false;
        int minasVizinhas = 0;
    }

    // construtor
    public CampoMinado() {
        inicializarCaminhoImagem();
        inicializarCelulas();
        setMinas();
        calcularMinasVizinhas();
    }

    // Construtor para carregar jogo existente
    public CampoMinado(boolean[][] revelado, boolean[][] bandeiras, ArrayList<Point> minas) {
        this.minas = minas;
        inicializarCaminhoImagem();
        inicializarCelulas();

        // Restaurar estados
        for (int L = 0; L < LinhasCampo; L++) {
            for (int C = 0; C < ColunasCampo; C++) {
                celulas[L][C].revelado = revelado[L][C];
                celulas[L][C].bandeira = bandeiras[L][C];
                // Marcar minas
                for (Point mina : minas) {
                    if (mina.x == L && mina.y == C) {
                        celulas[L][C].temMina = true;
                    }
                }
            }
        }
        calcularMinasVizinhas();
        verificarVitoria();
    }

    private void inicializarCaminhoImagem() {
        String caminhoBase = ConfigEnv.getInstance().obter("BASE_IMAGE_PATH", 
            "midia");
        String nomeArquivo = (chatId != null && !chatId.isEmpty()) ? 
            chatId + ".png" : "campo.png";
        this.caminhoImagem = caminhoBase + File.separator + nomeArquivo;
    }

    private void inicializarCelulas() {
        for (int L = 0; L < LinhasCampo; L++) {
            for (int C = 0; C < ColunasCampo; C++) {
                celulas[L][C] = new CelulaEstado();
            }
        }
    }

    // coloca minas
    void setMinas() {
        minas = new ArrayList<Point>();
        Random aqui = new Random();

        for (int i = TotalMinas; i > 0; i--) {
            int L = aqui.nextInt(LinhasCampo);
            int C = aqui.nextInt(ColunasCampo);

            // Garante que não coloque mina na mesma posição
            Point novaMina = new Point(L, C);
            if (!minas.contains(novaMina)) {
                minas.add(novaMina);
                celulas[L][C].temMina = true;
            } else {
                i++; // Tenta novamente
            }
        }
    }

    void calcularMinasVizinhas() {
        for (int L = 0; L < LinhasCampo; L++) {
            for (int C = 0; C < ColunasCampo; C++) {
                if (!celulas[L][C].temMina) {
                    celulas[L][C].minasVizinhas = contarMinasVizinhas(L, C);
                }
            }
        }
    }

    int contarMinasVizinhas(int L, int C) {
        int count = 0;
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                int vizinhoL = L + i;
                int vizinhoC = C + j;
                if (vizinhoL >= 0 && vizinhoL < LinhasCampo &&
                        vizinhoC >= 0 && vizinhoC < ColunasCampo) {
                    if (celulas[vizinhoL][vizinhoC].temMina) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    // Método para revelar célula
    public boolean revelarCelula(int L, int C) {
        if (gameOver || celulas[L][C].revelado || celulas[L][C].bandeira) {
            return false;
        }

        celulas[L][C].revelado = true;

        if (celulas[L][C].temMina) {
            gameOver = true;
            revelarMinas();
        } else if (celulas[L][C].minasVizinhas == 0) {
            revelarVizinhas(L, C);
        }

        verificarVitoria();
        return true;
    }

    private void revelarVizinhas(int L, int C) {
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                int vizinhoL = L + i;
                int vizinhoC = C + j;
                if (vizinhoL >= 0 && vizinhoL < LinhasCampo &&
                        vizinhoC >= 0 && vizinhoC < ColunasCampo) {
                    if (!celulas[vizinhoL][vizinhoC].revelado &&
                            !celulas[vizinhoL][vizinhoC].bandeira) {
                        celulas[vizinhoL][vizinhoC].revelado = true;
                        if (celulas[vizinhoL][vizinhoC].minasVizinhas == 0) {
                            revelarVizinhas(vizinhoL, vizinhoC);
                        }
                    }
                }
            }
        }
    }

    // Método para colocar/remover bandeira
    public boolean toggleBandeira(int L, int C) {
        if (gameOver || celulas[L][C].revelado) {
            return false;
        }
        celulas[L][C].bandeira = !celulas[L][C].bandeira;
        return true;
    }

    // revelar todas as minas após uma ser clicada
    void revelarMinas() {
        for (Point mina : minas) {
            celulas[mina.x][mina.y].revelado = true;
        }
    }

    void verificarVitoria() {
        int celulasReveladas = 0;
        for (int L = 0; L < LinhasCampo; L++) {
            for (int C = 0; C < ColunasCampo; C++) {
                if (celulas[L][C].revelado && !celulas[L][C].temMina) {
                    celulasReveladas++;
                }
            }
        }

        if (celulasReveladas == (LinhasCampo * ColunasCampo - minas.size())) {
            vitoria = true;
        }
    }

    // GERA A IMAGEM DO CAMPO
    public void gerarImagem() {
        BufferedImage image = new BufferedImage(LarguraCampo, AlturaCampo, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        Font emoji = null;
        try {
            try {
                emoji = Font.createFont(Font.TRUETYPE_FONT, new FileInputStream("fontes/static/NotoEmoji-Bold.ttf"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FontFormatException e) {
            e.printStackTrace();
        }

        // Configurar qualidade
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Fundo azul
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, LarguraCampo, AlturaCampo);

        // Desenhar células
        for (int L = 0; L < LinhasCampo; L++) {
            for (int C = 0; C < ColunasCampo; C++) {
                int x = C * TamanhoQuadrado;
                int y = L * TamanhoQuadrado;

                CelulaEstado celula = celulas[L][C];

                // Cor de fundo da célula
                if (celula.revelado) {
                    if (celula.temMina) {
                        g.setColor(Color.RED); // Mina revelada
                    } else {
                        g.setColor(Color.WHITE); // Célula revelada
                    }
                } else {
                    g.setColor(Color.GRAY); // Célula não revelada
                }

                g.fillRect(x, y, TamanhoQuadrado, TamanhoQuadrado);
                g.setColor(Color.BLACK);
                g.drawRect(x, y, TamanhoQuadrado, TamanhoQuadrado);

                // Desenhar conteúdo da célula
                if (celula.revelado) {
                    if (celula.temMina) {
                        // Desenhar mina
                        g.setColor(Color.BLACK);
                        Font emojiT = emoji.deriveFont(Font.BOLD, 30);
                        g.setFont(emojiT);
                        FontMetrics fm = g.getFontMetrics();
                        int textWidth = fm.stringWidth("💣");
                        int textHeight = fm.getHeight();
                        g.drawString("💣", x + (TamanhoQuadrado - textWidth) / 2,
                                y + (TamanhoQuadrado + textHeight / 2) / 2);
                    } else if (celula.minasVizinhas > 0) {
                        // Desenhar número
                        g.setColor(getColorForNumber(celula.minasVizinhas));
                        g.setFont(new Font("Arial", Font.BOLD, 20));
                        String text = String.valueOf(celula.minasVizinhas);
                        FontMetrics fm = g.getFontMetrics();
                        int textWidth = fm.stringWidth(text);
                        int textHeight = fm.getHeight();
                        g.drawString(text, x + (TamanhoQuadrado - textWidth) / 2,
                                y + (TamanhoQuadrado + textHeight / 2) / 2);
                    }
                } else if (celula.bandeira) {
                    // Desenhar bandeira
                    g.setColor(Color.RED);
                    Font emojiT = emoji.deriveFont(Font.BOLD, 30);
                    g.setFont(emojiT);
                    FontMetrics fm = g.getFontMetrics();
                        int textWidth = fm.stringWidth("🚩");
                        int textHeight = fm.getHeight();
                    g.drawString("🚩", x + (TamanhoQuadrado - textWidth) / 2,
                                y + (TamanhoQuadrado + textHeight / 2) / 2);
                } else {
                    // Coordenadas para referência
                    g.setColor(Color.DARK_GRAY);
                    g.setFont(new Font("Arial", Font.PLAIN, 20));
                    String coord = "" + (char) ('A' + L) + (C + 1);
                    FontMetrics fm = g.getFontMetrics();
                    int textWidth = fm.stringWidth(coord);
                    int textHeight = fm.getHeight();
                    g.drawString(coord, x + (TamanhoQuadrado - textWidth) / 2,
                            y + (TamanhoQuadrado + textHeight / 2) / 2);
                }
            }
        }

        try {
            File outputDir = new File(new File(caminhoImagem).getParent());
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            File output = new File(caminhoImagem);
            ImageIO.write(image, "png", output);
            System.out.println("Imagem salva em: " + output.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Erro ao salvar a imagem: " + e.getMessage());
        }
    }

    private Color getColorForNumber(int number) {
        switch (number) {
            case 1:
                return Color.BLUE;
            case 2:
                return Color.GREEN;
            case 3:
                return Color.RED;
            case 4:
                return Color.MAGENTA;
            case 5:
                return Color.ORANGE;
            case 6:
                return Color.CYAN;
            case 7:
                return Color.BLACK;
            case 8:
                return Color.DARK_GRAY;
            default:
                return Color.BLACK;
        }
    }

    // Getters para salvar/recuperar estado
    public boolean[][] getRevelado() {
        boolean[][] revelado = new boolean[LinhasCampo][ColunasCampo];
        for (int L = 0; L < LinhasCampo; L++) {
            for (int C = 0; C < ColunasCampo; C++) {
                revelado[L][C] = celulas[L][C].revelado;
            }
        }
        return revelado;
    }

    public boolean[][] getBandeiras() {
        boolean[][] bandeiras = new boolean[LinhasCampo][ColunasCampo];
        for (int L = 0; L < LinhasCampo; L++) {
            for (int C = 0; C < ColunasCampo; C++) {
                bandeiras[L][C] = celulas[L][C].bandeira;
            }
        }
        return bandeiras;
    }

    public ArrayList<Point> getMinas() {
        return minas;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public boolean isVitoria() {
        return vitoria;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
        inicializarCaminhoImagem();
    }

    public String getCaminhoImagem() {
        return caminhoImagem;
    }
}