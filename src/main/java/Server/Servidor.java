package Server;

import Mapas.cargarMapas;
import Personajes.Jugador;
import Personajes.Zombie;
import Modelos.Mensaje;
import Modelos.TipoMensaje;
import Modelos.ActualizacionEstadoJuegoDTO;

import java.awt.Point; // Para coordenadas de disparo
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.Random; // Para el margen de error del francotirador
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
    private List<String> nombresJugadores = Collections.synchronizedList(new ArrayList<>());
    private List<String> nombresEnEspera = Collections.synchronizedList(new ArrayList<>());
    private char[][] mapaBase;
    private Timer gameLoopTimer;
    private final int GAME_TICK_MS = 150; 
    private AtomicInteger zombieIdCounter = new AtomicInteger(0);
    private AtomicInteger francotiradoresAsignados = new AtomicInteger(0); // NUEVO
    private final int MAX_FRANCOTIRADORES = 2; // NUEVO
    private Random random = new Random(); // NUEVO

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

    // NUEVO: Método para encontrar posición para francotirador
    private Point encontrarPosicionFrancotirador(char[][] mapaUsado) {
        List<Point> posicionesPosibles = new ArrayList<>();
        for (int fila = 1; fila < mapaBase.length - 1; fila++) { // Evitar bordes absolutos
            for (int col = 1; col < mapaBase[0].length - 1; col++) {
                if (mapaBase[fila][col] == 'X' && mapaUsado[fila][col] == 'X') { // Disponible en mapa base y no usado
                    // Podríamos añadir lógica para que no esté muy cerca de otro francotirador
                    // o que tenga "buena vista", pero por ahora cualquier 'X' no en borde sirve.
                    posicionesPosibles.add(new Point(col, fila));
                }
            }
        }
        if (posicionesPosibles.isEmpty()) {
            // Fallback si no hay 'X' internas, buscar en cualquier 'X'
            for (int fila = 0; fila < mapaBase.length; fila++) {
                for (int col = 0; col < mapaBase[0].length; col++) {
                    if (mapaBase[fila][col] == 'X' && mapaUsado[fila][col] == 'X') {
                         posicionesPosibles.add(new Point(col, fila));
                    }
                }
            }
        }
        if (!posicionesPosibles.isEmpty()) {
            Point p = posicionesPosibles.get(random.nextInt(posicionesPosibles.size()));
            mapaUsado[p.y][p.x] = 'F'; // Marcar como usado para francotirador en el mapa temporal
            return p;
        }
        return null; // No se encontró posición
    }

    public synchronized void iniciarJuego() {
        pantalla.write("Iniciando juego con jugadores en espera: " + nombresEnEspera);
        
        francotiradoresAsignados.set(0); // Resetear contador
        jugadores.clear(); // Limpiar jugadores de partidas anteriores

        char[][] mapaTemporalPosiciones = new char[mapaBase.length][];
        for(int i=0; i<mapaBase.length; ++i) mapaTemporalPosiciones[i] = mapaBase[i].clone();

        synchronized (nombresEnEspera) { 
            Iterator<String> iter = nombresEnEspera.iterator();
            while (iter.hasNext()) {
                String nombre = iter.next();
                Jugador nuevoJugador;

                // Asignar rol de francotirador si hay cupo
                if (francotiradoresAsignados.get() < MAX_FRANCOTIRADORES) {
                    Point posFrancotirador = encontrarPosicionFrancotirador(mapaTemporalPosiciones);
                    if (posFrancotirador != null) {
                        nuevoJugador = new Jugador(nombre, posFrancotirador.x, posFrancotirador.y);
                        nuevoJugador.setEsFrancotirador(true);
                        nuevoJugador.setMunicionFrancotirador(10); // Munición inicial
                        jugadores.put(nombre, nuevoJugador);
                        francotiradoresAsignados.incrementAndGet();
                        pantalla.write("Jugador " + nombre + " asignado como FRANCOTIRADOR en (" + posFrancotirador.x + ", " + posFrancotirador.y + ")");
                        continue; // Siguiente jugador
                    } else {
                        pantalla.write("ADVERTENCIA: No se encontró posición para francotirador " + nombre + ". Se asignará como jugador normal.");
                    }
                }

                // Si no es francotirador o no se pudo asignar, buscar posición 'P'
                boolean asignadoNormal = false;
                for (int fila = 0; fila < mapaTemporalPosiciones.length; fila++) {
                    for (int col = 0; col < mapaTemporalPosiciones[0].length; col++) {
                        if (mapaTemporalPosiciones[fila][col] == 'P') { 
                            nuevoJugador = new Jugador(nombre, col, fila);
                            jugadores.put(nombre, nuevoJugador);
                            mapaTemporalPosiciones[fila][col] = '.'; // Marcar como usada
                            pantalla.write("Jugador " + nombre + " asignado a (" + col + ", " + fila + ")");
                            asignadoNormal = true;
                            break;
                        }
                    }
                    if (asignadoNormal) break;
                }
                if (!asignadoNormal) {
                    pantalla.write("ADVERTENCIA: No se encontró posición inicial 'P' para " + nombre);
                    nuevoJugador = new Jugador(nombre, 1, 1); 
                    jugadores.put(nombre, nuevoJugador);
                    pantalla.write("Jugador " + nombre + " asignado a fallback (1,1)");
                }
            }
            nombresEnEspera.clear();
        }

        inicializarZombies();

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
        enviarActualizacionEstadoJuego(); // Enviar estado inicial a todos
    }

    private synchronized void actualizarLogicaJuego() {
        List<Jugador> jugadoresActualesLista; 
        synchronized(jugadores) {
            jugadoresActualesLista = new ArrayList<>(jugadores.values());
        }

        synchronized (zombies) {
            Iterator<Zombie> zombieIterator = zombies.iterator();
            while(zombieIterator.hasNext()){
                Zombie z = zombieIterator.next();
                if (z.getVidas() > 0) {
                    // Los francotiradores no son objetivos para los zombies de la misma manera
                    List<Jugador> jugadoresNoFrancotiradores = new ArrayList<>();
                    for(Jugador j : jugadoresActualesLista){
                        if(!j.esFrancotirador() && j.isVivo()){
                            jugadoresNoFrancotiradores.add(j);
                        }
                    }
                    z.actualizar(jugadoresNoFrancotiradores, mapaBase);
                } else {
                    // Opcional: remover zombie muerto de la lista aquí si no se hace en otro lado
                    // zombieIterator.remove(); 
                    // pantalla.write("Zombie " + z.getId() + " eliminado de la lista de activos.");
                }
            }
        }
        enviarActualizacionEstadoJuego();
    }
    
    // NUEVO: Procesar disparo de francotirador
    public synchronized void procesarDisparoFrancotirador(Mensaje mensaje) {
        String nombreFrancotirador = mensaje.getEnviador();
        Point objetivo = (Point) mensaje.getContenido(); // Esperamos un java.awt.Point

        Jugador francotirador = jugadores.get(nombreFrancotirador);
        if (francotirador == null || !francotirador.esFrancotirador()) {
            pantalla.write("Error: " + nombreFrancotirador + " no es un francotirador válido o no encontrado.");
            return;
        }

        if (francotirador.getMunicionFrancotirador() <= 0) {
            pantalla.write(nombreFrancotirador + " intentó disparar sin munición.");
            // Opcional: enviar mensaje al cliente "Sin munición"
            enviarMensajePrivadoFeed(nombreFrancotirador, "¡Sin munición!");
            return;
        }

        francotirador.gastarBala();
        pantalla.write(nombreFrancotirador + " disparó a (" + objetivo.x + "," + objetivo.y + "). Munición restante: " + francotirador.getMunicionFrancotirador());

        // Margen de error del 20%
        if (random.nextDouble() < 0.20) { // 20% de fallar
            pantalla.write("¡Disparo de " + nombreFrancotirador + " falló!");
            enviarMensajePrivadoFeed(nombreFrancotirador, "¡Fallaste el tiro!");
            enviarActualizacionEstadoJuego(); // Para actualizar munición
            return;
        }

        // Buscar si hay un zombie en el objetivo
        Zombie zombieAlcanzado = null;
        synchronized (zombies) {
            for (Zombie z : zombies) {
                if (z.getX() == objetivo.x && z.getY() == objetivo.y && z.getVidas() > 0) {
                    zombieAlcanzado = z;
                    break;
                }
            }
        }

        if (zombieAlcanzado != null) {
            int danoCausado = 100; // Daño suficiente para matar un zombie de un tiro
            zombieAlcanzado.tomarDano(danoCausado);
            pantalla.write("¡" + nombreFrancotirador + " acertó! Zombie " + zombieAlcanzado.getId() + " en (" + objetivo.x + "," + objetivo.y + ") eliminado.");
            enviarMensajePrivadoFeed(nombreFrancotirador, "¡Blanco eliminado en ("+objetivo.x+","+objetivo.y+")!");
            if (zombieAlcanzado.getVidas() <= 0) {
                 // Opcional: si queremos que desaparezcan inmediatamente del mapa tras morir
                 // synchronized(zombies) { zombies.remove(zombieAlcanzado); }
                 // pantalla.write("Zombie " + zombieAlcanzado.getId() + " removido de la lista activa.");
            }
        } else {
            pantalla.write(nombreFrancotirador + " acertó el disparo, pero no había un zombie en (" + objetivo.x + "," + objetivo.y + ").");
            enviarMensajePrivadoFeed(nombreFrancotirador, "Tiro acertado, ¡pero no había nada ahí!");
        }
        enviarActualizacionEstadoJuego(); // Actualizar estado del juego (zombie muerto, munición)
    }

    // Helper para enviar feedback a un jugador específico (ej. francotirador)
    private void enviarMensajePrivadoFeed(String nombreJugador, String contenido) {
        Mensaje feedMsg = new Mensaje("Servidor", contenido, nombreJugador, TipoMensaje.PRIVADO);
        privateMessage(feedMsg);
    }


    public synchronized void privateMessage(Mensaje mensaje) {
        synchronized (clientesAceptados) {
            for (ThreadServidor cliente : clientesAceptados) {
                try {
                    if (mensaje.getReceptor().equals(cliente.nombre)) {
                        if (cliente.socket != null && !cliente.socket.isClosed() && cliente.salida != null) {
                            cliente.salida.writeObject(mensaje);
                            cliente.salida.flush();
                            // No loguear mensajes de feedback de francotirador aquí para no spamear consola del servidor
                            if (!mensaje.getContenido().toString().contains("¡Fallaste el tiro!") &&
                                !mensaje.getContenido().toString().contains("¡Blanco eliminado!") &&
                                !mensaje.getContenido().toString().contains("¡Sin munición!") &&
                                !mensaje.getContenido().toString().contains("Tiro acertado, ¡pero no había nada ahí!")) {
                                pantalla.write("Mensaje privado enviado a " + cliente.nombre);
                            }
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
        if (!jugadores.containsKey(nombre)) { // Verifica si ya está en el juego (mapa de jugadores)
            // Solo añade a nombresJugadores (lista para combobox) si no está
            if (!nombresJugadores.contains(nombre)) {
                nombresJugadores.add(nombre);
            }
            pantalla.write("Jugador '" + nombre + "' registrado para lobby. Total nombres: " + nombresJugadores.size());
            agregarJugadorEnEspera(nombre); // Lo añade a la cola para la próxima partida
        } else {
            // Si el jugador ya existe en `jugadores`, podría ser una reconexión o un error.
            // Por ahora, simplemente lo logueamos. Podríamos tener lógica de reconexión aquí.
            pantalla.write("Jugador '" + nombre + "' ya estaba en la partida activa o fue registrado previamente.");
            // Podríamos querer añadirlo a nombresEnEspera igualmente si la partida ya terminó y se está formando una nueva.
            if (!nombresEnEspera.contains(nombre)) {
                 agregarJugadorEnEspera(nombre);
            } else {
                 enviarActualizacionListaJugadores(); //Asegurar que el cliente que se reconecta reciba la lista
            }
        }
    }

    public synchronized void eliminarCliente(ThreadServidor cliente) {
        if (cliente == null || cliente.nombre == null) {
            pantalla.write("Intento de eliminar cliente nulo o sin nombre.");
            // Limpiar cualquier cliente nulo o sin nombre que pudo haberse colado
            clientesAceptados.removeIf(c -> c == null || c.nombre == null);
            return;
        }

        boolean clienteRemovido = clientesAceptados.remove(cliente);
        Jugador jugadorAsociado = jugadores.get(cliente.nombre); // Obtener antes de remover
        Jugador jugadorRemovido = jugadores.remove(cliente.nombre);
        boolean nombreRemovido = nombresJugadores.remove(cliente.nombre);
        nombresEnEspera.remove(cliente.nombre); 

        if (jugadorAsociado != null && jugadorAsociado.esFrancotirador()) {
            francotiradoresAsignados.decrementAndGet();
            pantalla.write("Un francotirador (" + cliente.nombre + ") se ha desconectado. Francotiradores restantes: " + francotiradoresAsignados.get());
        }


        if (clienteRemovido || jugadorRemovido != null || nombreRemovido) {
            pantalla.write("Cliente desconectado y eliminado: " + cliente.nombre);
            enviarActualizacionListaJugadores(); 

            if (!jugadores.isEmpty() && gameLoopTimer != null) {
                // No es necesario enviar actualización de estado aquí, el game loop lo hará.
                // enviarActualizacionEstadoJuego(); 
            } else if (jugadores.isEmpty() && gameLoopTimer != null) {
                gameLoopTimer.cancel();
                gameLoopTimer = null;
                zombies.clear(); 
                francotiradoresAsignados.set(0); // Resetear al no haber jugadores
                pantalla.write("No hay jugadores. Game loop detenido, zombies limpiados y francotiradores reseteados.");
            }
        } else {
            pantalla.write("Intento de eliminar cliente " + cliente.nombre + " que no estaba en las listas principales.");
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
    }

    public void enviarActualizacionEstadoJuego() {
        List<Jugador> listaJugadoresCopia;
        List<Zombie> listaZombiesCopia;

        synchronized (jugadores) {
            listaJugadoresCopia = new ArrayList<>();
            for(Jugador j : jugadores.values()){
                // Solo enviar jugadores vivos o francotiradores (que siempre están "vivos" en su rol)
                if(j.isVivo() || j.esFrancotirador()){
                   listaJugadoresCopia.add(new Jugador(j)); 
                }
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
        // pantalla.write("Servidor.procesarMovimiento para " + nombreJugador + " direccion " + direccion); 

        Jugador jugador = jugadores.get(nombreJugador);
        if (jugador == null) {
            pantalla.write("Error: Jugador " + nombreJugador + " no encontrado para mover.");
            return;
        }
        if (jugador.esFrancotirador()){ // LOS FRANCOTIRADORES NO SE MUEVEN
            pantalla.write("Jugador " + nombreJugador + " es francotirador, no puede moverse.");
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
            // pantalla.write("Jugador " + nombreJugador + " movido de (" + oldX + "," + oldY + ") a (" + newX + ", " + newY + ")");
        } else {
            // pantalla.write("Movimiento inválido para " + nombreJugador + " de (" + oldX + "," + oldY + ") a (" + newX + ", " + newY + ")");
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
        if (mapaBase[y][x] == 'X') { // Los jugadores normales no pueden moverse a muros
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
            // pantalla.write("Hilo de conexiones detenido."); // Lo imprime el propio hilo
        }

        synchronized (clientesAceptados) {
            for (ThreadServidor cliente : new ArrayList<>(clientesAceptados)) { 
                try {
                    if (cliente.socket != null && !cliente.socket.isClosed()) {
                        cliente.socket.close();
                    }
                } catch (IOException e) {
                    pantalla.write("Error cerrando socket de cliente " + (cliente.nombre != null ? cliente.nombre : "desconocido") + ": " + e.getMessage());
                }
            }
            clientesAceptados.clear();
        }
        jugadores.clear();
        zombies.clear();
        nombresJugadores.clear();
        nombresEnEspera.clear();
        francotiradoresAsignados.set(0);
        
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
}