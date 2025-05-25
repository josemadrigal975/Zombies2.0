/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Server;

import Modelos.Mensaje;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 *
 * @author jos_m
 */
public class ThreadServidor extends Thread{
    public Socket socket;
    public ObjectOutputStream salida;
    private ObjectInputStream entrada;
    private DataInputStream entradaDatos;
    public String nombre;
    private Servidor server;
    private boolean isRunning = true;

    public ThreadServidor(Socket socket, Servidor server) {
        this.socket = socket;
        this.server = server;
        try {
            salida = new ObjectOutputStream(socket.getOutputStream());
            entrada = new ObjectInputStream(socket.getInputStream());
            entradaDatos = new DataInputStream(socket.getInputStream());
        } catch (IOException ex) {}
    }

    @Override
    public void run() {
        try {
            nombre = entradaDatos.readUTF();
            server.pantalla.write("Recibido nombre: " + nombre);
            server.registrarJugador(nombre);
            //server.agregarJugadorEnEspera(nombre);
        } catch (IOException ex) {
            return;
        }

        while (isRunning) {
            try {
                Mensaje mensaje = (Mensaje) entrada.readObject();
                server.pantalla.write("Recibido: " + mensaje);

                switch (mensaje.getTipo()) {
                    case PUBLICO -> server.broadcoast(mensaje);
                    case PRIVADO -> server.privateMessage(mensaje);
                    case DISPARO -> server.broadcoast(mensaje);
                    case MOVER -> server.broadcoast(mensaje);
                    default -> server.pantalla.write("Tipo de mensaje no manejado: " + mensaje);
                }
            } catch (IOException | ClassNotFoundException ex) {
                server.pantalla.write("Cliente desconectado inesperadamente.");
                isRunning = false;
                server.eliminarCliente(this);
            }
        }
    }
    
    
}
