/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Server;

import Modelos.Mensaje;
import Modelos.TipoMensaje; 
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
            salida.flush(); // Enviar la cabecera del stream inmediatamente.
            entrada = new ObjectInputStream(socket.getInputStream());
            entradaDatos = new DataInputStream(socket.getInputStream()); // Se usa solo para el nombre inicial
        } catch (IOException ex) {
             System.err.println("Error al crear streams para " + (nombre != null ? nombre : "nuevo cliente") + ": " + ex.getMessage());
             isRunning = false; 
        }
    }

    @Override
    public void run() {
        try {
            nombre = entradaDatos.readUTF(); // Leer nombre primero
            server.pantalla.write("Recibido nombre: " + nombre);
            server.registrarJugador(nombre); 
          
        } catch (IOException ex) {
            server.pantalla.write("Error al leer nombre o registrar jugador: " + ex.getMessage());
            isRunning = false; // No se puede continuar
        }

        while (isRunning) {
            try {
                Mensaje mensaje = (Mensaje) entrada.readObject();
                server.pantalla.write("Recibido de " + nombre + ": " + mensaje);

                switch (mensaje.getTipo()) {
                    case PUBLICO:
                        server.broadcoast(mensaje);
                        break;
                    case PRIVADO:
                        server.privateMessage(mensaje);
                        break;
                    case DISPARO: 
                        server.broadcoast(mensaje); 
                        break;
                    case MOVER: 
                        server.procesarMovimiento(mensaje);
                        break;
                    default:
                        server.pantalla.write("Tipo de mensaje no manejado de " + nombre + ": " + mensaje.getTipo());
                }
            } catch (IOException | ClassNotFoundException ex) {
                server.pantalla.write("Cliente " + nombre + " desconectado inesperadamente: " + ex.getMessage());
                isRunning = false;
              
            }
        }
        
        // Limpieza cuando el bucle termina (por error o desconexión)
        server.eliminarCliente(this);
        try {
            if (salida != null) salida.close();
            if (entrada != null) entrada.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ex) {
            server.pantalla.write("Error al cerrar recursos para " + nombre + ": " + ex.getMessage());
        }
        server.pantalla.write("Thread para " + nombre + " finalizado.");
    }
}