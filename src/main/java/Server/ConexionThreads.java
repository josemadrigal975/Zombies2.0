/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Server;

import java.io.IOException;
import java.net.Socket;

/**
 *
 * @author jos_m
 */
public class ConexionThreads extends Thread {
    private boolean isRunning = true;
    Servidor server;

    public ConexionThreads(Servidor server) {
        this.server = server;
    }

    public void run() {
        while (isRunning) {
            try {
                server.pantalla.write("Esperando cliente...");
                Socket socket = server.server.accept();
                ThreadServidor ts = new ThreadServidor(socket, server);
                ts.start();
                server.clientesAceptados.add(ts);
                server.pantalla.write("Cliente conectado.");
            } catch (IOException ex) {
                server.pantalla.write("Error al aceptar cliente: " + ex.getMessage());
            }
        }
    }
}
