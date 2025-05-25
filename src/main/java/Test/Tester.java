/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Test;

import Mapas.Mapas;
import Mapas.cargarMapas;
import java.awt.Color;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 *
 * @author jos_m
 */
public class Tester {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Crear ventana
            JFrame frame = new JFrame("Vista de prueba del mapa");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // Cargar el mapa
            char[][] mapa = cargarMapas.cargarMapaDesdeArchivo("mapa1.txt");
            Mapas panelMapa = new Mapas(mapa);

            // Opcional: borde visible para debug
            panelMapa.setBorder(BorderFactory.createLineBorder(Color.RED, 2));

            // Agregar panel
            frame.getContentPane().add(panelMapa);
            frame.pack(); // se ajusta al tamaño preferido del mapa
            frame.setLocationRelativeTo(null); // centrado
            frame.setVisible(true);
        });
    }
}
