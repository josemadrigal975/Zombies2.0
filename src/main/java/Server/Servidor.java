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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author jos_m
 */
public class Servidor {
    private final int PORT = 8084;
    ServerSocket server;
    public PantallaServidor pantalla;
    ArrayList<ThreadServidor> clientesAceptados;
    Map<String, Jugador> jugadores;
    ConexionThreads conexionsThread;
    private List<String> nombresJugadores = new ArrayList<>();
    private List<String> nombresEnEspera = new ArrayList<>();
    private char[][] mapa;



    public Servidor(PantallaServidor pantalla) {
        this.pantalla = pantalla;
        clientesAceptados = new ArrayList<>();
        this.mapa = cargarMapas.cargarMapaDesdeArchivo("src/resources/mapa1.txt");
        
        
        
        
        
        jugadores = new HashMap<>();
        connect();
        conexionsThread = new ConexionThreads(this);
        conexionsThread.start();
    }

    public void connect() {
        try {
            server = new ServerSocket(PORT);
            System.out.println("Servidor funcionandno en puerto " + PORT);
        } catch (IOException ex) {
            pantalla.write("Error iniciando servidor: " + ex.getMessage());
        }
    }

    public synchronized void broadcoast(Mensaje mensaje) {
        for (ThreadServidor cliente : clientesAceptados) {
            try {
                cliente.salida.writeObject(mensaje);
            } catch (IOException ex) {
                pantalla.write("Error enviando mensaje: " + ex.getMessage());
            }
        }
        pantalla.write("Enviado a todos: " + mensaje);
    }
    
    public synchronized void agregarJugadorEnEspera(String nombre) {
        nombresEnEspera.add(nombre);
        nombresJugadores.add(nombre);
        pantalla.write("Jugador en espera: " + nombre);
        enviarUpdateJugadores(); // lista de nombres
    }
    
    public synchronized void iniciarJuego() {
        for (String nombre : nombresEnEspera) {
            for (int fila = 0; fila < mapa.length; fila++) {
                for (int col = 0; col < mapa[0].length; col++) {
                    if (mapa[fila][col] == 'P') {
                        Jugador nuevo = new Jugador(nombre, col, fila);
                        jugadores.put(nombre, nuevo);
                        mapa[fila][col] = '.';
                        pantalla.write("Jugador " + nombre + " asignado a (" + col + ", " + fila + ")");
                        break;
                    }
                }
            }
        }
        nombresEnEspera.clear();

        List<Jugador> listaJugadores = new ArrayList<>(jugadores.values());
        Mensaje mensaje = new Mensaje("Servidor", listaJugadores, "TODOS", TipoMensaje.INICIALIZAR);
        broadcoast(mensaje);
    }


    public synchronized void privateMessage(Mensaje mensaje) {
        for (ThreadServidor cliente : clientesAceptados) {
            try {
                if (mensaje.getReceptor().equals(cliente.nombre)) {
                    cliente.salida.writeObject(mensaje);
                    break;
                }
            } catch (IOException ex) {
                pantalla.write("Error mensaje privado: " + ex.getMessage());
            }
        }
    }
    
    public void registrarJugador(String nombre) {
        jugadores.put(nombre, new Jugador(nombre, 1, 1)); // Posición inicial
        nombresJugadores.add(nombre);
        enviarUpdateJugadores();
    }

    public void eliminarCliente(ThreadServidor cliente) {
        clientesAceptados.remove(cliente);
        jugadores.remove(cliente.nombre);
        nombresJugadores.remove(cliente.nombre);
        pantalla.write("Cliente desconectado: " + cliente.nombre);
        enviarUpdateJugadores(); 
    }
    
    public void enviarUpdateJugadores(){
        String lista = String.join(",", nombresJugadores);
        Mensaje update = new Mensaje("Servidor", "UPDATE_JUGADORES " + lista, "ALL", null);
        broadcoast(update);
    }
    
    public List<ThreadServidor> getClientesAceptados() {
        return clientesAceptados;
    }

}
