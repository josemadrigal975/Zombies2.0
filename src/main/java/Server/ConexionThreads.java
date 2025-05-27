/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Server;

import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket; // Importar ServerSocket

/**
 *
 * @author jos_m
 */
public class ConexionThreads extends Thread {
    private volatile boolean isRunning = true; // Hacerlo volatile
    Servidor server;

    public ConexionThreads(Servidor server) {
        this.server = server;
    }

    public void run() {
        ServerSocket serverSocket = server.getServerSocket(); // Obtener el ServerSocket del servidor principal
        if (serverSocket == null || serverSocket.isClosed()) {
            server.pantalla.write("Hilo de Conexiones: ServerSocket no está disponible o cerrado. El hilo no puede ejecutarse.");
            return;
        }

        while (isRunning) {
            try {
                server.pantalla.write("Esperando cliente...");
                Socket socket = serverSocket.accept(); // Usar el ServerSocket del servidor
                server.pantalla.write("Cliente intentando conectar desde: " + socket.getInetAddress().getHostAddress());
                
                ThreadServidor ts = new ThreadServidor(socket, server);
                ts.start();
                // Agregar el cliente a la lista DESPUÉS de que el ThreadServidor haya inicializado sus streams
                // y leído el nombre, o manejarlo dentro del ThreadServidor.
                // Por ahora, lo dejamos así, pero la adición real a clientesAceptados
                // debería ser después de que el cliente se identifique.
                // server.clientesAceptados.add(ts); // Esto se hace ahora en registrarJugador/ThreadServidor

                // Se ha modificado ThreadServidor para que registre al jugador (y por ende se añada a la lista de clientes
                // a través de registrarJugador que llama a enviarActualizacionListaJugadores que usa clientesAceptados)
                // solo DESPUÉS de recibir el nombre.
                // La adición directa de 'ts' a 'clientesAceptados' aquí se puede hacer,
                // pero el 'nombre' del ThreadServidor será null hasta que se lea.
                 server.getClientesAceptados().add(ts); // Añadir a la lista general de threads
                 server.pantalla.write("Cliente conectado y thread iniciado. IP: " + socket.getInetAddress().getHostAddress() + ". Total threads: " + server.getClientesAceptados().size());


            } catch (java.net.SocketException se) {
                if (isRunning) { // Si isRunning es falso, es una parada controlada.
                    server.pantalla.write("SocketException en ConexionThreads (puede ser normal si el servidor se está deteniendo): " + se.getMessage());
                } else {
                    server.pantalla.write("ConexionThreads detenido.");
                }
                isRunning = false; // Detener el bucle en caso de SocketException no controlada
            } 
            catch (IOException ex) {
                if (isRunning) {
                    server.pantalla.write("Error al aceptar cliente: " + ex.getMessage());
                    // Considerar una pausa o lógica de reintento si es un error transitorio.
                }
            }
        }
        server.pantalla.write("Hilo ConexionThreads finalizado.");
    }

    public void detener() {
        isRunning = false;
        try {
            // Cerrar el ServerSocket para desbloquear el accept()
            if (server.getServerSocket() != null && !server.getServerSocket().isClosed()) {
                server.getServerSocket().close();
            }
        } catch (IOException e) {
            server.pantalla.write("Error al cerrar ServerSocket para detener ConexionThreads: " + e.getMessage());
        }
        this.interrupt(); // Interrumpir el hilo si está en alguna operación bloqueante
    }
}