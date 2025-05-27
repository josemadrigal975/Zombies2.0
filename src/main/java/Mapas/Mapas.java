package Mapas;

import Personajes.Jugador;
import Personajes.Zombie;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.util.ArrayList;
import java.util.Collections; 
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

public class Mapas extends JPanel {
    private char[][] mapa;
    private Image imgMuro, imgPiso, imgJugador, imgZombie, imgSalida;
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
    }

    private Image cargarImagen(String ruta) {
        java.net.URL url = getClass().getResource(ruta);
        if (url == null) {
            System.err.println("MAPAS: ❌ Imagen no encontrada: " + ruta);
            return null; 
        }
        return new ImageIcon(url).getImage();
    }
    
    public void setJugadores(List<Jugador> jugadores) {
     
        this.jugadores = (jugadores != null) ? new ArrayList<>(jugadores) : new ArrayList<>();
        // System.out.println("MAPAS.setJugadores: Lista actualizada con " + this.jugadores.size() + " jugadores.");
        // if(!this.jugadores.isEmpty()){
        //    Jugador j = this.jugadores.get(0); // Suponiendo que hay al menos uno para loguear
        //    System.out.println("MAPAS.setJugadores: Primer jugador en lista interna: " + j.getNombre() + 
        //                       " en (" + j.getX() + "," + j.getY() + "), Salud: " + j.getSalud());
        // }
    }

    public void setZombies(List<Zombie> zombies) {
        this.zombies = (zombies != null) ? new ArrayList<>(zombies) : new ArrayList<>();
        // System.out.println("MAPAS.setZombies: Lista actualizada con " + this.zombies.size() + " zombies.");
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // System.out.println("MAPAS.paintComponent INICIO - Jugadores: " + 
        //                   (this.jugadores != null ? this.jugadores.size() : "null") + 
        //                   ", Zombies: " + (this.zombies != null ? this.zombies.size() : "null"));

        if (mapa == null || mapa.length == 0) {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Color.RED);
            g.drawString("Error: Mapa no cargado.", 50, 50);
           
            return;
        }

       
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
                    // System.out.println("MAPAS: Dibujando Zombie ID: " + z.getId() + " en (" + z.getX() + "," + z.getY() + ")");
                    g.drawImage(imgZombie, z.getX() * 40, z.getY() * 40, 40, 40, this);
                }
            }
        }
        
        // 3. Dibujar Jugadores
        
        synchronized (this.jugadores) {
            for (Jugador j : this.jugadores) {
                if (j.isVivo() && imgJugador != null) { 
                    // System.out.println("MAPAS: Dibujando Jugador: " + j.getNombre() + " en (" + j.getX() + "," + j.getY() + ") Salud: " + j.getSalud());
                    g.drawImage(imgJugador, j.getX() * 40, j.getY() * 40, 40, 40, this);
                    
                    g.setColor(Color.WHITE);
                    g.setFont(new Font("Arial", Font.BOLD, 11));
                    int nombreWidth = g.getFontMetrics().stringWidth(j.getNombre());
                    g.drawString(j.getNombre(), j.getX() * 40 + (40 - nombreWidth) / 2, j.getY() * 40 + 12); 

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
            }
        }
        // System.out.println("MAPAS.paintComponent FIN");
    }
}