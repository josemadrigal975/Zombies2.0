/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Mapas;

import Modelos.Jugador;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

/**
 *
 * @author jos_m
 */



public class Mapas extends JPanel {
    private char[][] mapa;
    private Image imgMuro, imgPiso, imgZombie, imgJugador, imgSalida;
    private List<Jugador> jugadores = new ArrayList<>();    

    public Mapas(char[][] mapa) {
        this.mapa = mapa;
        cargarImagenes();

        if (mapa.length > 0 && mapa[0].length > 0) {
            setPreferredSize(new Dimension(mapa[0].length * 40, mapa.length * 40));
        } else {
            System.err.println("⚠️ El mapa está vacío.");
            setPreferredSize(new Dimension(400, 400)); // fallback
        }
    }

    private void cargarImagenes() {
        imgMuro = cargarImagen("/Imagenes/muroo.jpg");
        imgPiso = cargarImagen("/Imagenes/piso.jpeg");
        imgZombie = cargarImagen("/Imagenes/zombie.jpg");
        imgJugador = cargarImagen("/Imagenes/jugador.jpg");
        imgSalida = cargarImagen("/Imagenes/salida.jpeg");
    }

    private Image cargarImagen(String ruta) {
        java.net.URL url = getClass().getResource(ruta);
        if (url == null) {
            System.err.println("❌ Imagen no encontrada: " + ruta);
            return null;
        }
        return new ImageIcon(url).getImage();
    }
    
    public void setJugadores(List<Jugador> jugadores) {
        this.jugadores = jugadores;
        System.out.println("🧍 Dibujando " + jugadores.size() + " jugadores.");
        for (Jugador j : jugadores) {
            System.out.println("→ " + j.nombre + " en (" + j.x + "," + j.y + ")");
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (mapa == null) return;

        for (int fila = 0; fila < mapa.length; fila++) {
            for (int col = 0; col < mapa[0].length; col++) {
                char c = mapa[fila][col];
                Image img = switch (c) {
                    case 'X' -> imgMuro;
                    case '.' -> imgPiso;
                    case 'Z' -> imgZombie;
                    case 'P' -> imgJugador;
                    case 'S' -> imgSalida;
                    default -> imgPiso;
                };
                if (img != null) {
                    g.drawImage(img, col * 40, fila * 40, 40, 40, this);
                } else {
                    g.setColor(java.awt.Color.GRAY);
                    g.fillRect(col * 40, fila * 40, 40, 40);
                }
            }
        }
        for (Jugador j : jugadores) {
        g.drawImage(imgJugador, j.x * 40, j.y * 40, 40, 40, this);
        g.setColor(java.awt.Color.WHITE);
        g.drawString(j.nombre, j.x * 40 + 5, j.y * 40 - 5);
        }
    }
}
