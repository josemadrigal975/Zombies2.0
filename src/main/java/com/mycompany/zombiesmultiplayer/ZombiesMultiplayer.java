/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.zombiesmultiplayer;

import Mapas.cargarMapas;

/**
 *
 * @author jos_m
 */
public class ZombiesMultiplayer {

    public static void main(String[] args) {
       // 1. Iniciar servidor
        Server.PantallaServidor servidorUI = new Server.PantallaServidor();
        servidorUI.setVisible(true);

        // 2. Esperar un momento para asegurar que el socket esté listo
        try {
            Thread.sleep(1000); // 1 segundo (opcional)
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        


        // 3. Iniciar cliente
        javax.swing.SwingUtilities.invokeLater(() -> {
            Cliente.Pantalla clienteUI = new Cliente.Pantalla();
            clienteUI.setVisible(true);
        });
    }
}
