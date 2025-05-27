/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Personajes;

/**
 *
 * @author Evelyn
 */

import java.io.Serializable;
import java.util.List;
import java.util.Random;



public class Zombie implements Serializable {
    private static final long serialVersionUID = 3L; // Para serialización
    public String id; // Identificador único para cada zombie
    public int x, y;
    public int vidas;
    private EstadoZombie estado;
    private String direccionActual; // "UP", "DOWN", "LEFT", "RIGHT"
    private Jugador jugadorObjetivo;
    private static final int RANGO_VISION = 3;
    private static final int DAMAGE = 10; // Daño que hace el zombie
    private int ticksParaMovimiento; // Control de velocidad
    private int ticksDesdeUltimoMovimiento;
    private int ticksParaAtaque;
    private int ticksDesdeUltimoAtaque;

    private Random random = new Random();

    public Zombie(String id, int x, int y, int vidas, int velocidadTicks) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.vidas = vidas;
        this.estado = EstadoZombie.PATRULLANDO;
        this.direccionActual = obtenerDireccionAleatoria();
        this.ticksParaMovimiento = velocidadTicks; // e.g., 5 ticks para moverse
        this.ticksDesdeUltimoMovimiento = 0;
        this.ticksParaAtaque = 10; // e.g., 10 ticks para atacar (más lento que moverse)
        this.ticksDesdeUltimoAtaque = 0;
    }
    
    // Copy constructor (útil para enviar copias a clientes sin afectar el original del servidor)
    public Zombie(Zombie other) {
        this.id = other.id;
        this.x = other.x;
        this.y = other.y;
        this.vidas = other.vidas;
        this.estado = other.estado;
        this.direccionActual = other.direccionActual;
        // jugadorObjetivo no se copia intencionalmente para el DTO, el servidor maneja la lógica
        this.ticksParaMovimiento = other.ticksParaMovimiento;
        this.ticksDesdeUltimoMovimiento = other.ticksDesdeUltimoMovimiento;
        this.ticksParaAtaque = other.ticksParaAtaque;
        this.ticksDesdeUltimoAtaque = other.ticksDesdeUltimoAtaque;
    }


    public void actualizar(List<Jugador> jugadores, char[][] mapa) {
        ticksDesdeUltimoMovimiento++;
        ticksDesdeUltimoAtaque++;

        buscarJugador(jugadores, mapa);

        if (ticksDesdeUltimoMovimiento >= ticksParaMovimiento) {
            if (estado == EstadoZombie.PATRULLANDO) {
                patrullar(mapa);
            } else if (estado == EstadoZombie.PERSIGUIENDO) {
                perseguir(mapa);
            }
            ticksDesdeUltimoMovimiento = 0;
        }
    }

    private void buscarJugador(List<Jugador> jugadores, char[][] mapa) {
        jugadorObjetivo = null; // Resetear objetivo
        for (Jugador jugador : jugadores) {
            if (!jugador.isVivo()) continue;

            int deltaX = jugador.getX() - this.x;
            int deltaY = jugador.getY() - this.y;

            // Comprobar si está en la dirección que el zombie mira y dentro del rango
            boolean enRangoYDireccion = false;
            if (direccionActual.equals("UP") && deltaX == 0 && deltaY < 0 && deltaY >= -RANGO_VISION) {
                enRangoYDireccion = true;
            } else if (direccionActual.equals("DOWN") && deltaX == 0 && deltaY > 0 && deltaY <= RANGO_VISION) {
                enRangoYDireccion = true;
            } else if (direccionActual.equals("LEFT") && deltaY == 0 && deltaX < 0 && deltaX >= -RANGO_VISION) {
                enRangoYDireccion = true;
            } else if (direccionActual.equals("RIGHT") && deltaY == 0 && deltaX > 0 && deltaX <= RANGO_VISION) {
                enRangoYDireccion = true;
            }

            if (enRangoYDireccion) {
                if (tieneLineaDeVision(jugador, mapa)) {
                    jugadorObjetivo = jugador;
                    estado = EstadoZombie.PERSIGUIENDO;
                    // System.out.println("Zombie " + id + " detectó a " + jugador.getNombre());
                    return; // Encontró un objetivo
                }
            }
        }
        // Si no encontró objetivo o el objetivo se perdió
        if (jugadorObjetivo == null) {
            estado = EstadoZombie.PATRULLANDO;
        }
    }

    private boolean tieneLineaDeVision(Jugador jugador, char[][] mapa) {
        int x1 = this.x;
        int y1 = this.y;
        int x2 = jugador.getX();
        int y2 = jugador.getY();

        // Simplificación: solo línea recta horizontal o vertical
        if (x1 == x2) { // Vertical
            int startY = Math.min(y1, y2);
            int endY = Math.max(y1, y2);
            for (int i = startY + 1; i < endY; i++) {
                if (mapa[i][x1] == 'X') return false;
            }
        } else if (y1 == y2) { // Horizontal
            int startX = Math.min(x1, x2);
            int endX = Math.max(x1, x2);
            for (int i = startX + 1; i < endX; i++) {
                if (mapa[y1][i] == 'X') return false;
            }
        } else {
            return false; // No está en línea recta simple (para esta implementación)
        }
        return true;
    }

    private void patrullar(char[][] mapa) {
        int newX = this.x;
        int newY = this.y;

        switch (direccionActual) {
            case "UP": newY--; break;
            case "DOWN": newY++; break;
            case "LEFT": newX--; break;
            case "RIGHT": newX++; break;
        }

        if (puedeMover(newX, newY, mapa)) {
            this.x = newX;
            this.y = newY;
        } else {
            direccionActual = obtenerDireccionAleatoria(); // Cambiar dirección si choca
        }
    }

    private void perseguir(char[][] mapa) {
        if (jugadorObjetivo == null || !jugadorObjetivo.isVivo()) {
            estado = EstadoZombie.PATRULLANDO;
            return;
        }

        // Intentar atacar si está adyacente
        if (Math.abs(this.x - jugadorObjetivo.getX()) + Math.abs(this.y - jugadorObjetivo.getY()) == 1) {
            if (ticksDesdeUltimoAtaque >= ticksParaAtaque) {
                atacar(jugadorObjetivo);
                ticksDesdeUltimoAtaque = 0;
            }
            return; // No se mueve si está atacando o en cooldown de ataque
        }
        
        int targetX = jugadorObjetivo.getX();
        int targetY = jugadorObjetivo.getY();
        int currentX = this.x;
        int currentY = this.y;

        int dx = targetX - currentX;
        int dy = targetY - currentY;

        String mejorDireccion = "";
        int newX = currentX, newY = currentY;

        // Priorizar movimiento en el eje con mayor distancia
        if (Math.abs(dx) > Math.abs(dy)) {
            if (dx > 0) { mejorDireccion = "RIGHT"; newX++;} 
            else if (dx < 0) { mejorDireccion = "LEFT"; newX--;}
        } else if (Math.abs(dy) > Math.abs(dx)) {
            if (dy > 0) { mejorDireccion = "DOWN"; newY++;}
            else if (dy < 0) { mejorDireccion = "UP"; newY--;}
        } else if (dx != 0) { // Distancias iguales, intentar X primero
             if (dx > 0) { mejorDireccion = "RIGHT"; newX++;} 
             else { mejorDireccion = "LEFT"; newX--;}
        } else if (dy != 0) { // Distancias iguales, X es 0, intentar Y
            if (dy > 0) { mejorDireccion = "DOWN"; newY++;}
            else { mejorDireccion = "UP"; newY--;}
        }


        if (!mejorDireccion.isEmpty() && puedeMover(newX, newY, mapa)) {
            this.x = newX;
            this.y = newY;
            this.direccionActual = mejorDireccion;
        } else { // Si la dirección principal está bloqueada, intentar la secundaria
            newX = currentX; newY = currentY; // Reset
            if (Math.abs(dx) <= Math.abs(dy) && dx != 0) { // Si la prioridad era Y o iguales, y X no es 0
                 if (dx > 0) { mejorDireccion = "RIGHT"; newX++;} 
                 else { mejorDireccion = "LEFT"; newX--;}
            } else if (Math.abs(dy) <= Math.abs(dx) && dy != 0) { // Si la prioridad era X o iguales, y Y no es 0
                if (dy > 0) { mejorDireccion = "DOWN"; newY++;}
                else { mejorDireccion = "UP"; newY--;}
            }

            if (!mejorDireccion.isEmpty() && puedeMover(newX, newY, mapa)) {
                this.x = newX;
                this.y = newY;
                this.direccionActual = mejorDireccion;
            } else {
                // Si ambas están bloqueadas, podría cambiar a patrullar o quedarse quieto
                // Por ahora, cambiamos la dirección aleatoriamente para intentar desatascarse
                this.direccionActual = obtenerDireccionAleatoria();
            }
        }
    }

    private void atacar(Jugador jugador) {
        System.out.println("Zombie " + id + " ataca a " + jugador.getNombre());
        jugador.recibirDano(DAMAGE); // Asumimos que Jugador tiene este método
    }

    private boolean puedeMover(int newX, int newY, char[][] mapa) {
        if (newY < 0 || newY >= mapa.length || newX < 0 || newX >= mapa[0].length) {
            return false; // Fuera de límites
        }
        if (mapa[newY][newX] == 'X') {
            return false; // Pared
        }
        // Podría añadirse colisión con otros zombies si se desea
        return true;
    }

    private String obtenerDireccionAleatoria() {
        int i = random.nextInt(4);
        return switch (i) {
            case 0 -> "UP";
            case 1 -> "DOWN";
            case 2 -> "LEFT";
            default -> "RIGHT";
        };
    }
    
    public void tomarDano(int cantidad) {
        this.vidas -= cantidad;
        if (this.vidas < 0) this.vidas = 0;
        System.out.println("Zombie " + id + " recibió " + cantidad + " de daño. Vidas restantes: " + this.vidas);
        // Lógica de muerte del zombie si es necesario
    }

    // Getters para serialización y uso en cliente
    public int getX() { return x; }
    public int getY() { return y; }
    public String getId() { return id; }
    public int getVidas() { return vidas; }
    public String getDireccionActual() { return direccionActual; } // Para debug o dibujar orientado
    public EstadoZombie getEstado() { return estado; } // Para debug o dibujar diferente
    
    // Para el cliente (no necesita setters para x,y,vidas si solo muestra)
    public Zombie() {} // Constructor vacío para deserialización si es necesario
}