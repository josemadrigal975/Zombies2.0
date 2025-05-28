
package Server;

import Mapas.cargarMapas;
import Personajes.Jugador;
import Personajes.Zombie;
import Modelos.Mensaje;
import Modelos.TipoMensaje;
import Modelos.ActualizacionEstadoJuegoDTO;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;


public class Servidor {
    private final int PORT = 8084;
    ServerSocket serverSocket;
    public PantallaServidor pantalla;
    
    List<ThreadServidor> clientesAceptados = Collections.synchronizedList(new ArrayList<>());
    Map<String, Jugador> jugadores = Collections.synchronizedMap(new HashMap<>());
    List<Zombie> zombies = Collections.synchronizedList(new ArrayList<>());
    ConexionThreads conexionsThread;
    private List<String> nombresJugadores = Collections.synchronizedList(new ArrayList<>()); // Sincronizado
    private List<String> nombresEnEspera = Collections.synchronizedList(new ArrayList<>()); // Sincronizado
    private char[][] mapaBase;
    private Timer gameLoopTimer;
    private final int GAME_TICK_MS = 150; 
    private AtomicInteger zombieIdCounter = new AtomicInteger(0); // Para IDs únicos de zombies
    private int nivelActual = 1;
    private int MAX_NIVELES = 10;
    private long inicioPartidaMillis;
    private long tiempoActualPartida;



    public Servidor(PantallaServidor pantalla) {
        this.pantalla = pantalla;

        this.mapaBase = cargarMapas.cargarMapaDesdeArchivo("mapa1.txt");
        if (this.mapaBase == null || this.mapaBase.length == 0) {
            pantalla.write("ERROR CRÍTICO: No se pudo cargar el mapa. El servidor no puede funcionar.");
            this.mapaBase = new char[][]{{'X'}}; 
        }

        connect();
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
        
        synchronized (clientesAceptados) {
            Iterator<ThreadServidor> iterator = clientesAceptados.iterator();
            while (iterator.hasNext()) {
                ThreadServidor cliente = iterator.next();
                try {
                    if (cliente.socket != null && !cliente.socket.isClosed() && cliente.salida != null) {
                        if (mensaje.getTipo() == TipoMensaje.ACTUALIZAR_ESTADO_JUEGO ||
                            mensaje.getTipo() == TipoMensaje.INICIALIZAR) {
                            cliente.salida.reset(); 
                        }
                        cliente.salida.writeObject(mensaje);
                        cliente.salida.flush(); 
                    } else {
                        
                    }
                } catch (IOException ex) {
                    pantalla.write("Error enviando mensaje a " + (cliente.nombre != null ? cliente.nombre : "cliente desconocido") + ": " + ex.getMessage() + " (Tipo: " + mensaje.getTipo() +")");
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

    private synchronized void inicializarZombies() {
        zombies.clear(); 
        zombieIdCounter.set(0); 
        if (mapaBase == null) {
            pantalla.write("Error: mapaBase es nulo, no se pueden inicializar zombies.");
            return;
        }

        for (int fila = 0; fila < mapaBase.length; fila++) {
            for (int col = 0; col < mapaBase[0].length; col++) {
                if (mapaBase[fila][col] == 'Z') { 
                    String id = "zombie_" + zombieIdCounter.getAndIncrement();
                    zombies.add(new Zombie(id, col, fila, 100, 3)); 
                    
                }
            }
        }
        pantalla.write(zombies.size() + " zombies inicializados en total.");
    }


    public synchronized void iniciarJuego() {
        pantalla.write("Iniciando juego con jugadores en espera: " + nombresEnEspera);
        
        char[][] mapaTemporal = new char[mapaBase.length][];
        for(int i=0; i<mapaBase.length; ++i) mapaTemporal[i] = mapaBase[i].clone();

        synchronized (nombresEnEspera) { 
            Iterator<String> iter = nombresEnEspera.iterator();
            while (iter.hasNext()) {
                String nombre = iter.next();
                boolean asignado = false;
                for (int fila = 0; fila < mapaTemporal.length; fila++) {
                    for (int col = 0; col < mapaTemporal[0].length; col++) {
                        if (mapaTemporal[fila][col] == 'P') { 
                            Jugador nuevo = new Jugador(nombre, col, fila);
                            jugadores.put(nombre, nuevo);
                            mapaTemporal[fila][col] = '.'; 
                            pantalla.write("Jugador " + nombre + " asignado a (" + col + ", " + fila + ")");
                            asignado = true;
                            break;
                        }
                    }
                    if (asignado) break;
                }
                if (!asignado) {
                    pantalla.write("ADVERTENCIA: No se encontró posición inicial 'P' para " + nombre);
                    Jugador nuevo = new Jugador(nombre, 1, 1); 
                    jugadores.put(nombre, nuevo);
                    pantalla.write("Jugador " + nombre + " asignado a fallback (1,1)");
                }
            }
            nombresEnEspera.clear();
            String listaNombres = String.join(",", jugadores.keySet());
            Mensaje actualizar = new Mensaje("SERVIDOR", listaNombres, "TODOS", TipoMensaje.ACTUALIZAR_JUGADORES);

            for (ThreadServidor cliente : clientesAceptados) {
                try {
                    cliente.salida.writeObject(actualizar);
                    cliente.salida.flush();
                } catch (IOException e) {
                    System.err.println("Error al enviar lista de jugadores: " + e.getMessage());
                }
            }
        }

        inicializarZombies();
        inicioPartidaMillis = System.currentTimeMillis();
        tiempoActualPartida = 0;


        if (gameLoopTimer != null) {
            gameLoopTimer.cancel(); 
        }
        gameLoopTimer = new Timer("GameLoop");
        gameLoopTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                actualizarLogicaJuego();
            }
        }, 0, GAME_TICK_MS);

        pantalla.write("Bucle de juego iniciado.");
       Mensaje mensajeInicio = new Mensaje("SERVIDOR", "INICIAR_JUEGO", "TODOS", TipoMensaje.CONTROL);
        for (ThreadServidor cliente : clientesAceptados) {
            try {
                cliente.salida.writeObject(mensajeInicio);
                cliente.salida.flush();
            } catch (IOException e) {
                System.err.println("Error al enviar mensaje a cliente " + cliente.nombre + ": " + e.getMessage());
            }
        }
    }

    private synchronized void actualizarLogicaJuego() {
        tiempoActualPartida = System.currentTimeMillis() - inicioPartidaMillis;
        List<Jugador> jugadoresActualesLista; 
        synchronized(jugadores) {
            jugadoresActualesLista = new ArrayList<>(jugadores.values());
        }

        synchronized (zombies) {
            Iterator<Zombie> zombieIterator = zombies.iterator();
            while(zombieIterator.hasNext()){
                Zombie z = zombieIterator.next();
                if (z.getVidas() > 0) {
                    z.actualizar(jugadoresActualesLista, mapaBase);
                }
            }
        }
        enviarActualizacionEstadoJuego();
        
        verificarEstadoJuego();

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
            if (!nombresJugadores.contains(nombre)) {
                nombresJugadores.add(nombre);
            }
            pantalla.write("Jugador '" + nombre + "' registrado para lobby. Total nombres: " + nombresJugadores.size());
            agregarJugadorEnEspera(nombre);
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
        nombresEnEspera.remove(cliente.nombre); 

        if (clienteRemovido || jugadorRemovido != null || nombreRemovido) {
            pantalla.write("Cliente desconectado y eliminado: " + cliente.nombre);
            enviarActualizacionListaJugadores(); 

            if (!jugadores.isEmpty() && gameLoopTimer != null) {
                enviarActualizacionEstadoJuego(); 
            } else if (jugadores.isEmpty() && gameLoopTimer != null) {
                gameLoopTimer.cancel();
                gameLoopTimer = null;
                zombies.clear(); 
                pantalla.write("No hay jugadores. Game loop detenido y zombies limpiados.");
            }
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
        // pantalla.write("Enviada actualización de lista de jugadores: " + copiaNombres.size() + " nombres.");
    }

    public void enviarActualizacionEstadoJuego() {
        List<Jugador> listaJugadoresCopia;
        List<Zombie> listaZombiesCopia;

        synchronized (jugadores) {
            listaJugadoresCopia = new ArrayList<>();
            for(Jugador j : jugadores.values()){
                listaJugadoresCopia.add(new Jugador(j)); 
            }
        }
        synchronized (zombies) {
            listaZombiesCopia = new ArrayList<>();
            for(Zombie z : zombies){
                 if (z.getVidas() > 0) { 
                    listaZombiesCopia.add(new Zombie(z)); 
                 }
            }
        }

        ActualizacionEstadoJuegoDTO dto = new ActualizacionEstadoJuegoDTO(listaJugadoresCopia, listaZombiesCopia);
        Mensaje updateMsg = new Mensaje("Servidor", dto, "ALL", TipoMensaje.ACTUALIZAR_ESTADO_JUEGO);
        broadcoast(updateMsg);
        
    }

    public synchronized void procesarMovimiento(Mensaje mensajeMovimiento) {
        String nombreJugador = mensajeMovimiento.getEnviador();
        String direccion = (String) mensajeMovimiento.getContenido();
        pantalla.write("Servidor.procesarMovimiento para " + nombreJugador + " direccion " + direccion); 

        Jugador jugador = jugadores.get(nombreJugador);
        if (jugador == null) {
            pantalla.write("Error: Jugador " + nombreJugador + " no encontrado para mover.");
            return;
        }
        if(!jugador.isVivo()){
            pantalla.write("Jugador " + nombreJugador + " está muerto, no puede moverse.");
            return;
        }

        int oldX = jugador.getX();
        int oldY = jugador.getY();

        int newX = oldX;
        int newY = oldY;

        switch (direccion.toUpperCase()) {
            case "UP":    newY--; break;
            case "DOWN":  newY++; break;
            case "LEFT":  newX--; break;
            case "RIGHT": newX++; break;
            default:
                pantalla.write("Dirección de movimiento no válida: " + direccion);
                return;
        }

        if (isValidMove(newX, newY)) {
            jugador.setX(newX);
            jugador.setY(newY);
            pantalla.write("Jugador " + nombreJugador + " movido de (" + oldX + "," + oldY + ") a (" + newX + ", " + newY + ")"); // << DEBUG LOG
        } else {
            pantalla.write("Movimiento inválido para " + nombreJugador + " de (" + oldX + "," + oldY + ") a (" + newX + ", " + newY + ")"); // << DEBUG LOG
        }
    }

    private boolean isValidMove(int x, int y) {
        if (mapaBase == null || mapaBase.length == 0 || mapaBase[0].length == 0) {
            pantalla.write("Error: Mapa base no cargado o vacío. No se puede validar movimiento.");
            return false; 
        }
        if (y < 0 || y >= mapaBase.length || x < 0 || x >= mapaBase[0].length) {
            return false; 
        }
        if (mapaBase[y][x] == 'X') {
            return false; 
        }
        return true; 
    }

    public List<ThreadServidor> getClientesAceptados() {
        return clientesAceptados; 
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }
    
    public Map<String, Jugador> getJugadores() {
        return jugadores;
    }

    public List<Zombie> getZombies() {
        return zombies;
    }

    public List<String> getNombresEnEspera() {
        return nombresEnEspera;
    }

    public void setNivelActual(int nivel) {
        this.nivelActual = nivel;
    }


    public synchronized void detenerServidor() {
        pantalla.write("Deteniendo el servidor...");
        if (gameLoopTimer != null) {
            gameLoopTimer.cancel();
            gameLoopTimer = null;
            pantalla.write("Game loop detenido.");
        }
        if (conexionsThread != null) {
            conexionsThread.detener(); 
            try {
                conexionsThread.join(1000); 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                 pantalla.write("Interrupción esperando al hilo de conexiones.");
            }
            pantalla.write("Hilo de conexiones detenido.");
        }

        synchronized (clientesAceptados) {
            for (ThreadServidor cliente : new ArrayList<>(clientesAceptados)) { 
                try {
                    if (cliente.socket != null && !cliente.socket.isClosed()) {
                       
                        // Mensaje msgCierre = new Mensaje("Servidor", "El servidor se está cerrando.", cliente.nombre, TipoMensaje.FINALIZAR_JUEGO);
                        // cliente.salida.writeObject(msgCierre);
                        // cliente.salida.flush();
                        cliente.socket.close();
                    }
                } catch (IOException e) {
                    pantalla.write("Error cerrando socket de cliente " + (cliente.nombre != null ? cliente.nombre : "desconocido") + ": " + e.getMessage());
                }
            }
            clientesAceptados.clear();
        }
        
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                pantalla.write("ServerSocket cerrado.");
            } catch (IOException e) {
                pantalla.write("Error cerrando ServerSocket: " + e.getMessage());
            }
        }
        pantalla.write("Servidor detenido.");
    }
    
    // Este método se usa para ver si hay aún jugadores vivos o muertos
    private synchronized void verificarEstadoJuego() {
         boolean algunoMurio = false;
        boolean todosEnSalida = true;

        for (Jugador j : jugadores.values()) {
            if (!j.isVivo()) {
                algunoMurio = true; 
            } else {
                
                if (mapaBase[j.getY()][j.getX()] != 'S') {
                    todosEnSalida = false;
                    j.setNotificadoLlegada(false); // Se permite volver a notificar si se movió
                } else {
                    if (!j.isNotificadoLlegada()) {
                        long tiempoSegundos = tiempoActualPartida / 1000;
                        j.setLlegoMeta(true);
                        j.setTiempoEscape(tiempoSegundos);
                        j.setNotificadoLlegada(true);

                        pantalla.write("⏱️ Tiempo registrado para " + j.getNombre() + ": " + tiempoSegundos + "s");

                        Mensaje msg = new Mensaje(
                            "Servidor", "¡Llegaste a la salida!", j.getNombre(), TipoMensaje.LLEGO_META
                        );
                        privateMessage(msg);
                    }
                }
            }
        }

        if (algunoMurio) {
            pantalla.write("Un jugador ha muerto. Reiniciando juego...");
            reiniciarJuego();
        } else if (todosEnSalida) {
            pantalla.write("Todos los jugadores vivos llegaron a la salida. Avanzando de nivel...");
            avanzarDeNivel();
        }
    }
    
    public synchronized void avanzarDeNivel() {
        nivelActual++;
        if (nivelActual > MAX_NIVELES) nivelActual = 1;

        this.mapaBase = cargarMapas.cargarMapaDesdeArchivo("mapa" + nivelActual + ".txt");

        nombresEnEspera.clear();
        nombresEnEspera.addAll(jugadores.keySet());
        jugadores.clear();

        Mensaje reinicio = new Mensaje("Servidor", "mapa" + nivelActual + ".txt", "ALL", TipoMensaje.REINICIAR_JUEGO);
        broadcoast(reinicio);

        pantalla.write("Avanzando al nivel " + nivelActual + "...");

        if (!nombresEnEspera.isEmpty()) {
            iniciarJuego();
            pantalla.write("Nivel " + nivelActual + " iniciado.");
        } else {
            pantalla.write("No hay jugadores en espera. No se inició el nuevo nivel.");
        }
    }
        
    public synchronized void reiniciarJuego() {
        nivelActual = 1;
        this.mapaBase = cargarMapas.cargarMapaDesdeArchivo("mapa1.txt");

        nombresEnEspera.clear();
        nombresEnEspera.addAll(jugadores.keySet());
        jugadores.clear();

        Mensaje desbloquear = new Mensaje("SERVIDOR", "UNLOCK", "TODOS", TipoMensaje.VOLVER_LOBBY);
        broadcoast(desbloquear);

        Mensaje reinicio = new Mensaje("Servidor", "mapa1.txt", "ALL", TipoMensaje.REINICIAR_JUEGO);
        broadcoast(reinicio);

        pantalla.write("Reiniciando juego con los jugadores en espera...");

        if (!nombresEnEspera.isEmpty()) {
            iniciarJuego();
        } else {
            pantalla.write("Nadie en espera. No se reinició el juego.");
        }
    }


    
    public void enviarActualizacionTiempo() {
        long segundos = tiempoActualPartida / 1000;
        Mensaje msg = new Mensaje("Servidor", segundos, "ALL", TipoMensaje.ACTUALIZAR_TIEMPO);
        broadcoast(msg);
    }

    public synchronized void copiarMapaAlProyecto(File archivoOrigen) {
        try {
            String nombreArchivo = archivoOrigen.getName();

            File destino = new File("src/main/resources/Mapas/" + nombreArchivo);
            if (destino.exists()) {
                pantalla.write("Ya existe un mapa con ese nombre: " + nombreArchivo);
                return;
            }

            java.nio.file.Files.copy(archivoOrigen.toPath(), destino.toPath());

            MAX_NIVELES++; 
            pantalla.write("Mapa " + nombreArchivo + " copiado al proyecto como nivel " + MAX_NIVELES);

        } catch (IOException e) {
            pantalla.write("Error al copiar el mapa: " + e.getMessage());
        }
    }


    
    


}