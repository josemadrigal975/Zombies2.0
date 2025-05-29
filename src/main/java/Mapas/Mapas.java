package Mapas;

import Personajes.Jugador;
import Personajes.Zombie;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.util.ArrayList;
// import java.util.Collections; // No se usa directamente aquí
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

public class Mapas extends JPanel {
    private char[][] mapa;
    private Image imgMuro, imgPiso, imgJugador, imgZombie, imgSalida, imgFrancotirador; // NUEVO imgFrancotirador
    private List<Jugador> jugadores = new ArrayList<>();    
    private List<Zombie> zombies = new ArrayList<>();

    public Mapas(char[][] mapa) {
        this.mapa = mapa;
        cargarImagenes();

        if (mapa != null && mapa.length > 0 && mapa[0].length > 0) {
            setPreferredSize(new Dimension(mapa[0].length * 40, mapa.length * 40));
        } else {
            System.err.println("MAPAS: ⚠️ El mapa está vacío o es nulo al construir Mapas.");
            setPreferredSize(new Dimension(400, 400)); 
        }
        this.setDoubleBuffered(true); 
    }

    private void cargarImagenes() {
        imgMuro = cargarImagen("/Imagenes/muroo.jpg");
        imgPiso = cargarImagen("/Imagenes/piso.jpeg");
        imgZombie = cargarImagen("/Imagenes/zombie.png");
        imgJugador = cargarImagen("/Imagenes/jugadora.jpg");
        imgSalida = cargarImagen("/Imagenes/salida.jpeg");
        imgFrancotirador = cargarImagen("/Imagenes/francotirador.jpg"); // NUEVO: Asegúrate de tener esta imagen
        if (imgFrancotirador == null) {
            System.err.println("MAPAS: ❌ Imagen de francotirador no encontrada, usando imagen de jugador normal como fallback.");
            imgFrancotirador = imgJugador; // Fallback si no se carga
        }
    }

    private Image cargarImagen(String ruta) {
        java.net.URL url = getClass().getResource(ruta);
        if (url == null) {
            System.err.println("MAPAS: ❌ Imagen no encontrada en classpath: " + ruta);
            return null; 
        }
        return new ImageIcon(url).getImage();
    }
    
    public void setJugadores(List<Jugador> jugadores) {
        this.jugadores = (jugadores != null) ? new ArrayList<>(jugadores) : new ArrayList<>();
    }

    public void setZombies(List<Zombie> zombies) {
        this.zombies = (zombies != null) ? new ArrayList<>(zombies) : new ArrayList<>();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (mapa == null || mapa.length == 0) {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Color.RED);
            g.drawString("Error: Mapa no cargado.", 50, 50);
            return;
        }

        // 1. Dibujar el mapa base
        for (int fila = 0; fila < mapa.length; fila++) {
            for (int col = 0; col < mapa[0].length; col++) {
                char c = mapa[fila][col];
                Image imgToDraw = null;
                switch (c) {
                    case 'X': imgToDraw = imgMuro; break;
                    case '.': imgToDraw = imgPiso; break;
                    case 'P': imgToDraw = imgPiso; break; 
                    case 'Z': imgToDraw = imgPiso; break; 
                    case 'S': imgToDraw = imgSalida; break;
                    default:  imgToDraw = imgPiso; break;
                }

                if (imgToDraw != null) {
                    g.drawImage(imgToDraw, col * 40, fila * 40, 40, 40, this);
                } else { 
                    g.setColor(Color.MAGENTA); 
                    g.fillRect(col * 40, fila * 40, 40, 40);
                    g.setColor(Color.BLACK);
                    g.drawString("?", col * 40 + 15, fila * 40 + 25);
                }
            }
        }
        
        // 2. Dibujar Zombies
        synchronized (this.zombies) { 
            for (Zombie z : this.zombies) {
                if (z.getVidas() > 0 && imgZombie != null) {
                    g.drawImage(imgZombie, z.getX() * 40, z.getY() * 40, 40, 40, this);
                }
            }
        }
        
        // 3. Dibujar Jugadores
        synchronized (this.jugadores) {
            for (Jugador j : this.jugadores) {
                Image imgParaJugador = imgJugador; 
                if (j.esFrancotirador()) {
                    imgParaJugador = imgFrancotirador;
                }

                if ((j.isVivo() || j.esFrancotirador()) && imgParaJugador != null) { 
                    g.drawImage(imgParaJugador, j.getX() * 40, j.getY() * 40, 40, 40, this);
                    
                    g.setColor(Color.WHITE);
                    g.setFont(new Font("Arial", Font.BOLD, 11));
                    int nombreWidth = g.getFontMetrics().stringWidth(j.getNombre());
                    g.drawString(j.getNombre(), j.getX() * 40 + (40 - nombreWidth) / 2, j.getY() * 40 + 10); 

                    if (!j.esFrancotirador()) {
                        int saludActual = j.getSalud();
                        int maxSalud = 100; 
                        double porcentajeSalud = Math.max(0, (double) saludActual / maxSalud);
                        int anchoBarraSalud = (int) (38 * porcentajeSalud); 

                        g.setColor(Color.RED); 
                        g.fillRect(j.getX() * 40 + 1, j.getY() * 40 - 7, 38, 5); 
                        g.setColor(Color.GREEN); 
                        g.fillRect(j.getX() * 40 + 1, j.getY() * 40 - 7, anchoBarraSalud, 5);
                        g.setColor(Color.BLACK); 
                        g.drawRect(j.getX() * 40 + 1, j.getY() * 40 - 7, 38, 5);
                    }
                    else { // Es francotirador, dibujar munición debajo del nombre si se prefiere en el mapa
                        // g.setColor(Color.ORANGE);
                        // g.setFont(new Font("Arial", Font.PLAIN, 10));
                        // String municionStr = j.getMunicionFrancotirador() + "b"; // Corto para el mapa
                        // int municionWidth = g.getFontMetrics().stringWidth(municionStr);
                        // g.drawString(municionStr, j.getX() * 40 + (40 - municionWidth) / 2, j.getY() * 40 + 38);
                        // La munición ya se muestra en lblMunicionDisplay en ZonaJuego
                    }
                }
            }
        }
    }
}