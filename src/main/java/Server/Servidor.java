package Server;

import Mapas.cargarMapas;
import Personajes.Jugador;
import Personajes.Zombie;
import Modelos.Mensaje;
import Modelos.TipoMensaje;
import Modelos.ActualizacionEstadoJuegoDTO;

import java.awt.Point;
import java.io.File; // Para copiarMapaAlProyecto
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.Random;
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
    
    // Variables de la Versión A (Francotirador)
    private AtomicInteger francotiradoresAsignados = new AtomicInteger(0);
    private final int MAX_FRANCOTIRADORES = 2; // O el valor que desees
    private Random random = new Random();

    // Variables de la Versión B (Niveles, Tiempo)
    private int nivelActual = 1;
    private int MAX_NIVELES = 1; // Se incrementará si se añaden mapas
    private long inicioPartidaMillis;
    private long tiempoActualPartidaSegundos; // En segundos

    public Servidor(PantallaServidor pantalla) {
        this.pantalla = pantalla;
        actualizarMaxNivelesDesdeArchivos(); // Contar mapas existentes al inicio
        this.mapaBase = cargarMapas.cargarMapaDesdeArchivo("mapa" + nivelActual + ".txt");
        if (this.mapaBase == null || this.mapaBase.length == 0) {
            pantalla.write("ERROR CRÍTICO: No se pudo cargar el mapa inicial (mapa" + nivelActual + ".txt). El servidor no puede funcionar.");
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
    
    private void actualizarMaxNivelesDesdeArchivos() {
        File mapasDir = new File("src/main/resources/Mapas/");
        if (mapasDir.exists() && mapasDir.isDirectory()) {
            File[] archivosMapas = mapasDir.listFiles((dir, name) -> name.matches("mapa\\d+\\.txt"));
            if (archivosMapas != null) {
                MAX_NIVELES = Math.max(1, archivosMapas.length); // Al menos 1 nivel
                pantalla.write("Detectados " + MAX_NIVELES + " niveles de mapa.");
            }
        } else {
            pantalla.write("Directorio de mapas no encontrado, usando MAX_NIVELES=1 por defecto.");
            MAX_NIVELES = 1;
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
                    zombies.add(new Zombie(id, col, fila, 100, 3)); // Vidas, velocidad
                }
            }
        }
        pantalla.write(zombies.size() + " zombies inicializados en total para el nivel " + nivelActual + ".");
    }

    private Point encontrarPosicionFrancotirador(char[][] mapaUsado) {
        List<Point> posicionesPosibles = new ArrayList<>();
        for (int fila = 1; fila < mapaBase.length - 1; fila++) { 
            for (int col = 1; col < mapaBase[0].length - 1; col++) {
                if (mapaBase[fila][col] == 'X' && mapaUsado[fila][col] == 'X') { 
                    posicionesPosibles.add(new Point(col, fila));
                }
            }
        }
        if (posicionesPosibles.isEmpty()) {
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
            mapaUsado[p.y][p.x] = 'F'; // Marcar como usado para francotirador
            return p;
        }
        return null;
    }

    public synchronized void iniciarJuego() {
        pantalla.write("Iniciando juego (Nivel " + nivelActual + ") con jugadores en espera: " + nombresEnEspera);
        
        francotiradoresAsignados.set(0); // Resetear contador de francotiradores
        jugadores.clear(); // Limpiar jugadores de partidas anteriores si es un inicio nuevo

        char[][] mapaTemporalPosiciones = new char[mapaBase.length][];
        for(int i=0; i<mapaBase.length; ++i) mapaTemporalPosiciones[i] = mapaBase[i].clone();

        synchronized (nombresEnEspera) { 
            Iterator<String> iter = nombresEnEspera.iterator();
            while (iter.hasNext()) {
                String nombre = iter.next();
                Jugador nuevoJugador;
                boolean asignado = false;

                // Asignar rol de francotirador si hay cupo (lógica de A)
                if (francotiradoresAsignados.get() < MAX_FRANCOTIRADORES && nombresEnEspera.size() > 1) { // Al menos 2 jugadores para tener un francotirador
                    Point posFrancotirador = encontrarPosicionFrancotirador(mapaTemporalPosiciones);
                    if (posFrancotirador != null) {
                        nuevoJugador = new Jugador(nombre, posFrancotirador.x, posFrancotirador.y);
                        nuevoJugador.setEsFrancotirador(true);
                        nuevoJugador.setMunicionFrancotirador(10); 
                        jugadores.put(nombre, nuevoJugador);
                        francotiradoresAsignados.incrementAndGet();
                        pantalla.write("Jugador " + nombre + " asignado como FRANCOTIRADOR en (" + posFrancotirador.x + ", " + posFrancotirador.y + ")");
                        asignado = true;
                    } else {
                        pantalla.write("ADVERTENCIA: No se encontró posición para francotirador " + nombre + ". Se asignará como jugador normal.");
                    }
                }

                // Si no es francotirador o no se pudo asignar, buscar posición 'P' (lógica de B)
                if (!asignado) {
                    for (int fila = 0; fila < mapaTemporalPosiciones.length; fila++) {
                        for (int col = 0; col < mapaTemporalPosiciones[0].length; col++) {
                            if (mapaTemporalPosiciones[fila][col] == 'P') { 
                                nuevoJugador = new Jugador(nombre, col, fila);
                                jugadores.put(nombre, nuevoJugador);
                                mapaTemporalPosiciones[fila][col] = '.'; // Marcar como usada
                                pantalla.write("Jugador " + nombre + " (normal) asignado a (" + col + ", " + fila + ")");
                                asignado = true;
                                break;
                            }
                        }
                        if (asignado) break;
                    }
                }
                
                if (!asignado) { // Fallback si no se pudo asignar ni como francotirador ni en 'P'
                    pantalla.write("ADVERTENCIA: No se encontró posición inicial 'P' para " + nombre + " (y no fue francotirador).");
                    nuevoJugador = new Jugador(nombre, 1, 1); // Posición de fallback
                    jugadores.put(nombre, nuevoJugador);
                    pantalla.write("Jugador " + nombre + " asignado a fallback (1,1)");
                }
            }
            nombresEnEspera.clear(); // Limpiar la lista de espera ya que todos fueron procesados
        }

        inicializarZombies();
        inicioPartidaMillis = System.currentTimeMillis(); // Iniciar cronómetro del servidor
        tiempoActualPartidaSegundos = 0;

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

        pantalla.write("Bucle de juego iniciado para el Nivel " + nivelActual + ".");
        // Enviar mensaje a los clientes para que inicien su UI de juego
        Mensaje mensajeInicioCliente = new Mensaje("Servidor", "INICIAR_JUEGO", "ALL", TipoMensaje.CONTROL);
        broadcoast(mensajeInicioCliente);
        enviarActualizacionEstadoJuego(); // Enviar estado inicial
        enviarActualizacionListaJugadores(); // Enviar lista de jugadores actualizada
    }

    private synchronized void actualizarLogicaJuego() {
        // Actualizar tiempo de partida (lógica de B)
        long currentTimeMillis = System.currentTimeMillis();
        tiempoActualPartidaSegundos = (currentTimeMillis - inicioPartidaMillis) / 1000;
        enviarActualizacionTiempo(); // Enviar tiempo a los clientes

        List<Jugador> jugadoresNoFrancotiradoresVivos = new ArrayList<>();
        synchronized(jugadores) {
            for(Jugador j : jugadores.values()){
                // Solo jugadores vivos y no francotiradores son objetivos para zombies
                if(j.isVivo() && !j.esFrancotirador() && !j.isLlegoMeta()){
                   jugadoresNoFrancotiradoresVivos.add(j); 
                }
            }
        }

        synchronized (zombies) {
            Iterator<Zombie> zombieIterator = zombies.iterator();
            while(zombieIterator.hasNext()){
                Zombie z = zombieIterator.next();
                if (z.getVidas() > 0) {
                    z.actualizar(jugadoresNoFrancotiradoresVivos, mapaBase);
                }
            }
        }
        enviarActualizacionEstadoJuego(); // Esto actualiza posiciones, salud, etc.
        verificarEstadoJuego(); // Verificar si alguien murió o todos llegaron a la meta
    }
    
    public synchronized void procesarDisparoFrancotirador(Mensaje mensaje) {
        String nombreFrancotirador = mensaje.getEnviador();
        Point objetivo = (Point) mensaje.getContenido();
        Jugador francotirador = jugadores.get(nombreFrancotirador);

        if (francotirador == null || !francotirador.esFrancotirador()) {
            pantalla.write("Error: " + nombreFrancotirador + " no es un francotirador válido o no encontrado.");
            return;
        }
        if (francotirador.getMunicionFrancotirador() <= 0) {
            enviarMensajePrivadoFeed(nombreFrancotirador, "¡Sin munición!");
            return;
        }
        francotirador.gastarBala();
        pantalla.write(nombreFrancotirador + " disparó a (" + objetivo.x + "," + objetivo.y + "). Munición restante: " + francotirador.getMunicionFrancotirador());

        if (random.nextDouble() < 0.20) { // 20% de fallar (lógica de A)
            pantalla.write("¡Disparo de " + nombreFrancotirador + " falló!");
            enviarMensajePrivadoFeed(nombreFrancotirador, "¡Fallaste el tiro!");
            enviarActualizacionEstadoJuego();
            return;
        }

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
            zombieAlcanzado.tomarDano(100); // Daño suficiente para matar
            pantalla.write("¡" + nombreFrancotirador + " acertó! Zombie " + zombieAlcanzado.getId() + " eliminado.");
            enviarMensajePrivadoFeed(nombreFrancotirador, "¡Blanco eliminado en ("+objetivo.x+","+objetivo.y+")!");
        } else {
            enviarMensajePrivadoFeed(nombreFrancotirador, "Tiro acertado, ¡pero no había nada ahí!");
        }
        enviarActualizacionEstadoJuego();
    }

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
                            if (!mensaje.getContenido().toString().contains("¡Fallaste el tiro!") && // Evitar spam de logs
                                !mensaje.getContenido().toString().contains("¡Blanco eliminado!") &&
                                !mensaje.getContenido().toString().contains("¡Sin munición!") &&
                                !mensaje.getContenido().toString().contains("Tiro acertado, ¡pero no había nada ahí!")) {
                                pantalla.write("Mensaje privado enviado a " + cliente.nombre);
                            }
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
            pantalla.write("Jugador '" + nombre + "' ya estaba en la partida activa o fue registrado previamente. Añadiendo a espera si es necesario.");
             if (!nombresEnEspera.contains(nombre)) {
                 agregarJugadorEnEspera(nombre);
            } else {
                 enviarActualizacionListaJugadores();
            }
        }
         // Actualizar la lista de jugadores para todos los clientes cada vez que alguien se registra.
        enviarActualizacionListaJugadores();
    }

    public synchronized void eliminarCliente(ThreadServidor cliente) {
        if (cliente == null || cliente.nombre == null) {
            pantalla.write("Intento de eliminar cliente nulo o sin nombre.");
            clientesAceptados.removeIf(c -> c == null || c.nombre == null);
            return;
        }

        String nombreCliente = cliente.nombre;
        boolean clienteRemovido = clientesAceptados.remove(cliente);
        Jugador jugadorAsociado = jugadores.get(nombreCliente);
        Jugador jugadorRemovidoDelJuego = jugadores.remove(nombreCliente);
        boolean nombreRemovidoDeListaGeneral = nombresJugadores.remove(nombreCliente);
        nombresEnEspera.remove(nombreCliente); 

        if (jugadorAsociado != null && jugadorAsociado.esFrancotirador()) {
            francotiradoresAsignados.decrementAndGet();
            pantalla.write("Un francotirador (" + nombreCliente + ") se ha desconectado. Francotiradores restantes: " + francotiradoresAsignados.get());
        }

        if (clienteRemovido || jugadorRemovidoDelJuego != null || nombreRemovidoDeListaGeneral) {
            pantalla.write("Cliente desconectado y eliminado: " + nombreCliente);
            enviarActualizacionListaJugadores(); 

            if (!jugadores.isEmpty() && gameLoopTimer != null) {
                // No es necesario enviar aquí, el game loop lo hará.
            } else if (jugadores.isEmpty() && gameLoopTimer != null) {
                pantalla.write("No quedan jugadores activos. Deteniendo el bucle de juego actual...");
                gameLoopTimer.cancel();
                gameLoopTimer = null;
                zombies.clear(); 
                francotiradoresAsignados.set(0);
                nivelActual = 1; // Resetear nivel si no hay jugadores
                // Considerar si se debe cargar mapa1 aquí o esperar a un nuevo inicio.
                // Por ahora, solo se detiene el loop y se limpia.
                pantalla.write("Game loop detenido, zombies limpiados, francotiradores y nivel reseteados.");
            }
        } else {
            pantalla.write("Intento de eliminar cliente " + nombreCliente + " que no estaba en las listas principales.");
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
        List<Jugador> listaJugadoresCopia = new ArrayList<>();
        List<Zombie> listaZombiesCopia = new ArrayList<>();

        synchronized (jugadores) {
            for(Jugador j : jugadores.values()){
                // Enviar todos los jugadores (vivos, muertos, en meta, francotiradores)
                // El cliente decidirá cómo dibujarlos.
                listaJugadoresCopia.add(new Jugador(j)); 
            }
        }
        synchronized (zombies) {
            for(Zombie z : zombies){
                 if (z.getVidas() > 0) { // Solo enviar zombies vivos
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
        Jugador jugador = jugadores.get(nombreJugador);

        if (jugador == null) {
            pantalla.write("Error: Jugador " + nombreJugador + " no encontrado para mover.");
            return;
        }
        if (jugador.esFrancotirador()){
            pantalla.write("Jugador " + nombreJugador + " es francotirador, no puede moverse con teclas.");
            return;
        }
        if(!jugador.isVivo()){
            pantalla.write("Jugador " + nombreJugador + " está muerto, no puede moverse.");
            return;
        }
        if(jugador.isLlegoMeta()){
            pantalla.write("Jugador " + nombreJugador + " ya llegó a la meta, no puede moverse.");
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
            default: return;
        }

        if (isValidMove(newX, newY)) {
            jugador.setX(newX);
            jugador.setY(newY);
        }
        // La actualización de estado se envía en el gameloop.
    }

    private boolean isValidMove(int x, int y) {
        if (mapaBase == null || y < 0 || y >= mapaBase.length || x < 0 || x >= mapaBase[0].length) {
            return false; 
        }
        // Jugadores normales no pueden moverse a muros ('X').
        // Los francotiradores están en 'X' pero no se mueven con esta lógica.
        return mapaBase[y][x] != 'X'; 
    }
    
    // Nuevos métodos de la versión B (Niveles, Tiempo, etc.)
    private synchronized void verificarEstadoJuego() {
        if (jugadores.isEmpty() && !nombresEnEspera.isEmpty() && gameLoopTimer == null) {
            // Si no hay jugadores activos pero sí en espera, y el juego no está corriendo, se podría auto-iniciar.
            // Pero por ahora, el inicio es manual desde PantallaServidor.
            return;
        }
        if (jugadores.isEmpty()) return; // No hay jugadores activos para verificar.

        boolean algunJugadorNoFrancoMurio = false;
        boolean todosJugadoresNoFrancoEnSalida = true;
        int jugadoresNoFrancoActivos = 0;

        synchronized(jugadores) {
            for (Jugador j : jugadores.values()) {
                if (j.esFrancotirador()) continue; // Ignorar francotiradores para estas condiciones

                jugadoresNoFrancoActivos++;
                if (!j.isVivo()) {
                    algunJugadorNoFrancoMurio = true;
                    break; // Si uno muere (y no es francotirador), se reinicia el juego
                }
                if (mapaBase[j.getY()][j.getX()] != 'S') {
                    todosJugadoresNoFrancoEnSalida = false;
                    j.setNotificadoLlegada(false); 
                } else { // Está en 'S'
                    if (!j.isNotificadoLlegada()) {
                        j.setLlegoMeta(true);
                        j.setTiempoEscape(tiempoActualPartidaSegundos);
                        j.setNotificadoLlegada(true);
                        pantalla.write("⏱️ Tiempo registrado para " + j.getNombre() + ": " + tiempoActualPartidaSegundos + "s (Nivel " + nivelActual + ")");
                        Mensaje msgLlegoMeta = new Mensaje("Servidor", "¡Llegaste a la salida en " + tiempoActualPartidaSegundos + "s!", j.getNombre(), TipoMensaje.LLEGO_META);
                        privateMessage(msgLlegoMeta);
                        // El cliente desactivará sus controles al recibir LLEGO_META
                    }
                }
            }
        }

        if (algunJugadorNoFrancoMurio) {
            pantalla.write("Un jugador no francotirador ha muerto. Reiniciando juego al nivel 1...");
            reiniciarJuegoAlNivel1();
        } else if (jugadoresNoFrancoActivos > 0 && todosJugadoresNoFrancoEnSalida) {
            // Todos los jugadores NO francotiradores están vivos Y en la salida
            pantalla.write("Todos los jugadores no francotiradores vivos llegaron a la salida. Avanzando de nivel...");
            avanzarDeNivel();
        }
        // Si no hay jugadoresNoFrancoActivos (ej. solo quedan francotiradores), no se avanza ni reinicia por esta lógica.
        // Se podría añadir una condición para que los francotiradores "ganen" si sobreviven X tiempo o eliminan X zombies.
    }
    
    public synchronized void avanzarDeNivel() {
        if (gameLoopTimer != null) { // Detener el bucle actual antes de cambiar de nivel
            gameLoopTimer.cancel();
            gameLoopTimer = null;
        }
        nivelActual++;
        if (nivelActual > MAX_NIVELES) {
            pantalla.write("¡TODOS LOS NIVELES COMPLETADOS! Reiniciando al nivel 1.");
            // Aquí se podría enviar un mensaje de victoria general antes de reiniciar.
            Mensaje msgVictoriaTotal = new Mensaje("Servidor", "¡FELICIDADES! Han completado todos los niveles.", "ALL", TipoMensaje.PUBLICO);
            broadcoast(msgVictoriaTotal);
            nivelActual = 1;
        }

        this.mapaBase = cargarMapas.cargarMapaDesdeArchivo("mapa" + nivelActual + ".txt");
        if (this.mapaBase == null || this.mapaBase.length == 0) {
            pantalla.write("ERROR: No se pudo cargar mapa" + nivelActual + ".txt. Volviendo a nivel 1.");
            nivelActual = 1;
            this.mapaBase = cargarMapas.cargarMapaDesdeArchivo("mapa1.txt");
            if (this.mapaBase == null || this.mapaBase.length == 0) {
                 pantalla.write("ERROR CRITICO: No se pudo cargar ni mapa" + nivelActual + " ni mapa1.txt. El servidor no puede continuar el juego.");
                 // Detener el juego completamente o manejar el error de otra forma.
                 return;
            }
        }
        
        // Preparar jugadores para el nuevo nivel (resetear estado de llegada a meta, etc.)
        synchronized(jugadores) {
            nombresEnEspera.clear(); // Limpiar espera
            for(Jugador j : jugadores.values()){
                nombresEnEspera.add(j.getNombre()); // Todos los jugadores actuales pasan a espera para el nuevo nivel
                // Resetear estado para el nuevo nivel (excepto francotiradores que mantienen su posición)
                if (!j.esFrancotirador()) {
                    j.setLlegoMeta(false);
                    j.setNotificadoLlegada(false);
                    j.setTiempoEscape(0);
                    // La salud se podría resetear o mantener, según diseño. Por ahora se mantiene.
                    // j.setSalud(100); 
                    // j.setVivo(true);
                }
            }
        }
        // `jugadores.clear()` no se hace aquí porque se repoblará en iniciarJuego con los mismos nombres.

        Mensaje msgNuevoNivel = new Mensaje("Servidor", "mapa" + nivelActual + ".txt", "ALL", TipoMensaje.REINICIAR_JUEGO);
        broadcoast(msgNuevoNivel); // Notificar a clientes del nuevo mapa
        pantalla.write("Avanzando al nivel " + nivelActual + "...");

        if (!nombresEnEspera.isEmpty()) {
            iniciarJuego(); // Esto repoblará `jugadores` desde `nombresEnEspera` y reiniciará zombies y timer.
            pantalla.write("Nivel " + nivelActual + " iniciado.");
        } else {
            pantalla.write("No hay jugadores en espera. No se inició el nuevo nivel " + nivelActual + ".");
        }
    }
        
    public synchronized void reiniciarJuegoAlNivel1() {
        if (gameLoopTimer != null) {
            gameLoopTimer.cancel();
            gameLoopTimer = null;
        }
        nivelActual = 1; // Siempre reiniciar al nivel 1
        this.mapaBase = cargarMapas.cargarMapaDesdeArchivo("mapa" + nivelActual + ".txt");
         if (this.mapaBase == null || this.mapaBase.length == 0) {
             pantalla.write("ERROR CRITICO: No se pudo cargar mapa1.txt para el reinicio. El servidor no puede continuar el juego.");
             return;
         }

        // Todos los jugadores (incluidos los que estaban en juego) vuelven al lobby conceptualmente
        // y se añaden a nombresEnEspera para la nueva partida.
        synchronized(jugadores) {
            nombresEnEspera.clear();
            for(Jugador j : jugadores.values()){
                nombresEnEspera.add(j.getNombre());
                // Resetear completamente el estado del jugador
                j.setLlegoMeta(false);
                j.setNotificadoLlegada(false);
                j.setTiempoEscape(0);
                j.setSalud(100);
                j.setVivo(true);
                // Si era francotirador, al reiniciar podría dejar de serlo o reasignarse.
                // Por simplicidad, al reiniciar a nivel 1, se reasignan roles.
                j.setEsFrancotirador(false); 
                j.setMunicionFrancotirador(0);
            }
        }
        // `jugadores.clear()` no es necesario aquí, se repoblará en `iniciarJuego`.
        francotiradoresAsignados.set(0); // Resetear francotiradores para reasignación

        // Notificar a clientes para que vuelvan al lobby y se preparen para mapa1
        Mensaje msgVolverLobby = new Mensaje("Servidor", "VOLVER_LOBBY_POR_REINICIO", "ALL", TipoMensaje.VOLVER_LOBBY);
        broadcoast(msgVolverLobby);
        Mensaje msgReiniciarMapa = new Mensaje("Servidor", "mapa" + nivelActual + ".txt", "ALL", TipoMensaje.REINICIAR_JUEGO);
        broadcoast(msgReiniciarMapa);

        pantalla.write("Juego reiniciado al Nivel " + nivelActual + " con jugadores en espera...");

        if (!nombresEnEspera.isEmpty()) {
            iniciarJuego(); // Inicia una nueva partida en el nivel 1
        } else {
            pantalla.write("No hay jugadores en espera. No se reinició el juego.");
        }
    }
    
    public void enviarActualizacionTiempo() {
        Mensaje msgTiempo = new Mensaje("Servidor", tiempoActualPartidaSegundos, "ALL", TipoMensaje.ACTUALIZAR_TIEMPO);
        broadcoast(msgTiempo);
    }

    public synchronized void copiarMapaAlProyecto(File archivoOrigen) {
        try {
            String nombreArchivo = archivoOrigen.getName();
            // Validar que el nombre sea mapaX.txt donde X es un número
            if (!nombreArchivo.matches("mapa\\d+\\.txt")) {
                pantalla.write("Error: El nombre del archivo debe ser 'mapa' seguido de un número y '.txt' (ej: mapa3.txt)");
                return;
            }

            File destinoDir = new File("src/main/resources/Mapas/");
            if (!destinoDir.exists()) {
                destinoDir.mkdirs(); // Crear directorio si no existe
            }
            File destino = new File(destinoDir, nombreArchivo);
            
            if (destino.exists()) {
                 int confirm = javax.swing.JOptionPane.showConfirmDialog(null, 
                    "El archivo " + nombreArchivo + " ya existe. ¿Desea sobrescribirlo?", 
                    "Confirmar Sobrescritura", 
                    javax.swing.JOptionPane.YES_NO_OPTION);
                if (confirm == javax.swing.JOptionPane.NO_OPTION) {
                    pantalla.write("Copia del mapa " + nombreArchivo + " cancelada por el usuario.");
                    return;
                }
            }

            java.nio.file.Files.copy(archivoOrigen.toPath(), destino.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            pantalla.write("Mapa " + nombreArchivo + " copiado/actualizado en el proyecto.");
            actualizarMaxNivelesDesdeArchivos(); // Recontar mapas

        } catch (IOException e) {
            pantalla.write("Error al copiar el mapa: " + e.getMessage());
        }
    }

    public List<ThreadServidor> getClientesAceptados() { return clientesAceptados; }
    public ServerSocket getServerSocket() { return serverSocket; }
    public Map<String, Jugador> getJugadoresMap() { return jugadores; } // Renombrado para claridad
    public List<Zombie> getZombiesList() { return zombies; } // Renombrado para claridad
    public List<String> getNombresEnEsperaList() { return nombresEnEspera; } // Renombrado para claridad
    public void setNivelActual(int nivel) { this.nivelActual = nivel; }

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
        }

        Mensaje msgCierre = new Mensaje("Servidor", "El servidor se está cerrando.", "ALL", TipoMensaje.FINALIZAR_JUEGO);
        broadcoast(msgCierre); // Notificar a todos los clientes

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