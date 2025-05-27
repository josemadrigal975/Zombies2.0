
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Server;

import Mapas.cargarMapas;
import Personajes.Jugador;
import Modelos.Mensaje;
import Modelos.TipoMensaje;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator;


/**
 *
 * @author jos_m
 */
public class Servidor {
    private final int PORT = 8084;
    ServerSocket serverSocket; // Renombrado para evitar confusión con la clase Servidor
    public PantallaServidor pantalla;
    // Sincronizar el acceso a clientesAceptados y jugadores
    List<ThreadServidor> clientesAceptados = Collections.synchronizedList(new ArrayList<>());
    Map<String, Jugador> jugadores = Collections.synchronizedMap(new HashMap<>());
    ConexionThreads conexionsThread;
    private List<String> nombresJugadores = Collections.synchronizedList(new ArrayList<>()); // Sincronizado
    private List<String> nombresEnEspera = Collections.synchronizedList(new ArrayList<>()); // Sincronizado
    private char[][] mapaBase; // Mapa cargado una vez


    public Servidor(PantallaServidor pantalla) {
        this.pantalla = pantalla;
        // Cargar el mapa una vez al inicio
        this.mapaBase = cargarMapas.cargarMapaDesdeArchivo("mapa1.txt");
        if (this.mapaBase == null || this.mapaBase.length == 0) {
            pantalla.write("ERROR CRÍTICO: No se pudo cargar el mapa. El servidor no puede funcionar.");
           
            this.mapaBase = new char[][]{{'X'}}; // Un mapa mínimo para evitar NullPointerExceptions
        }

        connect(); // Intenta abrir el ServerSocket
        if (serverSocket != null && !serverSocket.isClosed()) {
            conexionsThread = new ConexionThreads(this);
            conexionsThread.start();
        } else {
            pantalla.write("El servidor no pudo iniciarse. El hilo de conexiones no comenzará.");
        }
    }

    public void connect() {
        try {
            serverSocket = new ServerSocket(PORT);
            pantalla.write("Servidor funcionando en puerto " + PORT);
        } catch (IOException ex) {
            pantalla.write("Error iniciando servidor: " + ex.getMessage());
            
        }
    }

    public synchronized void broadcoast(Mensaje mensaje) {
        pantalla.write("Enviando a todos (" + clientesAceptados.size() + " clientes): " + mensaje);
        
        synchronized (clientesAceptados) {
            Iterator<ThreadServidor> iterator = clientesAceptados.iterator();
            while (iterator.hasNext()) {
                ThreadServidor cliente = iterator.next();
                try {
                    if (cliente.socket != null && !cliente.socket.isClosed() && cliente.salida != null) {
                      
                        if (mensaje.getTipo() == TipoMensaje.ACTUALIZAR_POSICIONES ||
                            mensaje.getTipo() == TipoMensaje.INICIALIZAR) {
                            cliente.salida.reset(); 
                        }
                        cliente.salida.writeObject(mensaje);
                        cliente.salida.flush(); 
                    } else {
                        pantalla.write("ADVERTENCIA: Intentando enviar a cliente nulo o con socket cerrado: " + cliente.nombre);
                    }
                } catch (IOException ex) {
                    pantalla.write("Error enviando mensaje a " + cliente.nombre + ": " + ex.getMessage() + ". Marcando para eliminar.");
                    
                }
            }
        }
    }

    public synchronized void agregarJugadorEnEspera(String nombre) {
        if (!nombresEnEspera.contains(nombre)) {
            nombresEnEspera.add(nombre);
        }
        if (!nombresJugadores.contains(nombre)) {
           nombresJugadores.add(nombre);
        }
        pantalla.write("Jugador en espera: " + nombre);
        enviarActualizacionListaJugadores();
    }

    public synchronized void iniciarJuego() {
        pantalla.write("Iniciando juego con jugadores en espera: " + nombresEnEspera);
        char[][] mapaTemporal = new char[mapaBase.length][];
        for(int i=0; i<mapaBase.length; ++i) mapaTemporal[i] = mapaBase[i].clone();

        synchronized (nombresEnEspera) { // Sincronizar acceso a nombresEnEspera
            Iterator<String> iter = nombresEnEspera.iterator();
            while (iter.hasNext()) {
                String nombre = iter.next();
                boolean asignado = false;
                for (int fila = 0; fila < mapaTemporal.length; fila++) {
                    for (int col = 0; col < mapaTemporal[0].length; col++) {
                        if (mapaTemporal[fila][col] == 'P') { // 'P' para posición inicial de jugador en el archivo de mapa
                            Jugador nuevo = new Jugador(nombre, col, fila);
                            jugadores.put(nombre, nuevo);
                            mapaTemporal[fila][col] = '.'; // Marcar la posición como ocupada en el mapa temporal
                            pantalla.write("Jugador " + nombre + " asignado a (" + col + ", " + fila + ")");
                            asignado = true;
                            break;
                        }
                    }
                    if (asignado) break;
                }
                if (!asignado) {
                    pantalla.write("ADVERTENCIA: No se encontró posición inicial para " + nombre);
                    //  asignarlo a una posición por defecto si no hay 'P's disponibles
                    Jugador nuevo = new Jugador(nombre, 1, 1); // Fallback
                    jugadores.put(nombre, nuevo);
                    pantalla.write("Jugador " + nombre + " asignado a fallback (1,1)");
                }
            }
            nombresEnEspera.clear();
        }


        List<Jugador> listaJugadoresActivos = new ArrayList<>(jugadores.values());
        Mensaje mensaje = new Mensaje("Servidor", listaJugadoresActivos, "ALL", TipoMensaje.INICIALIZAR);
        broadcoast(mensaje);
        pantalla.write("Mensaje INICIALIZAR enviado con " + listaJugadoresActivos.size() + " jugadores.");
    }


    public synchronized void privateMessage(Mensaje mensaje) {
        synchronized (clientesAceptados) {
            for (ThreadServidor cliente : clientesAceptados) {
                try {
                    if (mensaje.getReceptor().equals(cliente.nombre)) {
                        if (cliente.socket != null && !cliente.socket.isClosed() && cliente.salida != null) {
                           
                            cliente.salida.writeObject(mensaje);
                            cliente.salida.flush();
                            pantalla.write("Mensaje privado enviado a " + cliente.nombre);
                        } else {
                             pantalla.write("ADVERTENCIA: Intentando enviar msg privado a cliente nulo o con socket cerrado: " + cliente.nombre);
                        }
                        break;
                    }
                } catch (IOException ex) {
                    pantalla.write("Error mensaje privado a " + cliente.nombre + ": " + ex.getMessage());
                }
            }
        }
    }

    public synchronized void registrarJugador(String nombre) {
       
        if (!jugadores.containsKey(nombre)) {
            
            jugadores.put(nombre, new Jugador(nombre, 0, 0));
            if (!nombresJugadores.contains(nombre)) {
                nombresJugadores.add(nombre);
            }
            pantalla.write("Jugador '" + nombre + "' registrado. Total nombres: " + nombresJugadores.size());
            agregarJugadorEnEspera(nombre); // Lo añade a la lista de espera y actualiza a todos
        } else {
            pantalla.write("Jugador '" + nombre + "' ya estaba registrado.");
        }
    }

    public synchronized void eliminarCliente(ThreadServidor cliente) {
        if (cliente == null || cliente.nombre == null) {
            pantalla.write("Intento de eliminar cliente nulo o sin nombre.");
          
            clientesAceptados.removeIf(c -> c == null || c.nombre == null);
            return;
        }

        boolean clienteRemovido = clientesAceptados.remove(cliente);
        Jugador jugadorRemovido = jugadores.remove(cliente.nombre);
        boolean nombreRemovido = nombresJugadores.remove(cliente.nombre);
        nombresEnEspera.remove(cliente.nombre); // También de la lista de espera

        if (clienteRemovido || jugadorRemovido != null || nombreRemovido) {
            pantalla.write("Cliente desconectado y eliminado: " + cliente.nombre);
            enviarActualizacionListaJugadores();
            enviarActualizacionPosiciones(); // Notificar a los demás que un jugador se fue
        } else {
            pantalla.write("Intento de eliminar cliente " + cliente.nombre + " que no estaba en las listas.");
        }
    }

   
    public void enviarActualizacionListaJugadores(){
       
        List<String> copiaNombres;
        synchronized (nombresJugadores) {
            copiaNombres = new ArrayList<>(nombresJugadores);
        }
        String listaComoString = String.join(",", copiaNombres);
        Mensaje updateMsg = new Mensaje("Servidor", listaComoString, "ALL", TipoMensaje.ACTUALIZAR_JUGADORES);
        broadcoast(updateMsg);
        pantalla.write("Enviada actualización de lista de jugadores: " + copiaNombres.size() + " nombres.");
    }

    // Para las posiciones de los jugadores en el juego
    public void enviarActualizacionPosiciones() {
        List<Jugador> listaJugadoresActuales;
        synchronized (jugadores) { // Sincronizar el acceso al mapa de jugadores
            listaJugadoresActuales = new ArrayList<>(jugadores.values());
        }
        Mensaje updatePosMsg = new Mensaje("Servidor", listaJugadoresActuales, "ALL", TipoMensaje.ACTUALIZAR_POSICIONES);
        broadcoast(updatePosMsg);
        pantalla.write("Enviada actualización de posiciones: " + listaJugadoresActuales.size() + " jugadores.");
    }

    // << NUEVO: Método para procesar el movimiento
    public synchronized void procesarMovimiento(Mensaje mensajeMovimiento) {
        String nombreJugador = mensajeMovimiento.getEnviador();
        String direccion = (String) mensajeMovimiento.getContenido();

        Jugador jugador = jugadores.get(nombreJugador);
        if (jugador == null) {
            pantalla.write("Error: Jugador " + nombreJugador + " no encontrado para mover.");
            return;
        }

        int newX = jugador.getX();
        int newY = jugador.getY();

        switch (direccion) {
            case "UP":    newY--; break;
            case "DOWN":  newY++; break;
            case "LEFT":  newX--; break;
            case "RIGHT": newX++; break;
        }

        // Validar movimiento (colisiones con el mapa)
        if (isValidMove(newX, newY)) {
            jugador.setX(newX);
            jugador.setY(newY);
            pantalla.write("Jugador " + nombreJugador + " movido a (" + newX + ", " + newY + ")");
            enviarActualizacionPosiciones(); // Enviar las nuevas posiciones a todos los clientes
        } else {
            pantalla.write("Movimiento inválido para " + nombreJugador + " a (" + newX + ", " + newY + ")");
         
        }
    }

   
    private boolean isValidMove(int x, int y) {
        if (mapaBase == null || mapaBase.length == 0 || mapaBase[0].length == 0) {
            pantalla.write("Error: Mapa base no cargado o vacío. No se puede validar movimiento.");
            return false; // No se puede mover si no hay mapa
        }
        // Verificar límites del mapa
        if (y < 0 || y >= mapaBase.length || x < 0 || x >= mapaBase[0].length) {
            return false; // Fuera de los límites
        }
        // Verificar si es una pared 'X'
        if (mapaBase[y][x] == 'X') {
            return false; // Colisión con una pared
        }
        
        return true; // Movimiento válido
    }

    public List<ThreadServidor> getClientesAceptados() {
        return clientesAceptados; // Devuelve la lista sincronizada
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }
}