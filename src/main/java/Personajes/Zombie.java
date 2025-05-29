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
    
    public Zombie(Zombie other) {
        this.id = other.id;
        this.x = other.x;
        this.y = other.y;
        this.vidas = other.vidas;
        this.estado = other.estado;
        this.direccionActual = other.direccionActual;
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
        jugadorObjetivo = null; 
        for (Jugador jugador : jugadores) {
            if (!jugador.isVivo() || jugador.isLlegoMeta()) continue; // No perseguir muertos o los que ya llegaron

            int deltaX = jugador.getX() - this.x;
            int deltaY = jugador.getY() - this.y;

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
                    return; 
                }
            }
        }
        if (jugadorObjetivo == null) {
            estado = EstadoZombie.PATRULLANDO;
        }
    }

    private boolean tieneLineaDeVision(Jugador jugador, char[][] mapa) {
        int x1 = this.x;
        int y1 = this.y;
        int x2 = jugador.getX();
        int y2 = jugador.getY();

        if (x1 == x2) { 
            int startY = Math.min(y1, y2);
            int endY = Math.max(y1, y2);
            for (int i = startY + 1; i < endY; i++) {
                if (mapa[i][x1] == 'X') return false;
            }
        } else if (y1 == y2) { 
            int startX = Math.min(x1, x2);
            int endX = Math.max(x1, x2);
            for (int i = startX + 1; i < endX; i++) {
                if (mapa[y1][i] == 'X') return false;
            }
        } else {
            return false; 
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
            direccionActual = obtenerDireccionAleatoria(); 
        }
    }

    private void perseguir(char[][] mapa) {
        if (jugadorObjetivo == null || !jugadorObjetivo.isVivo() || jugadorObjetivo.isLlegoMeta()) {
            estado = EstadoZombie.PATRULLANDO;
            return;
        }

        if (Math.abs(this.x - jugadorObjetivo.getX()) + Math.abs(this.y - jugadorObjetivo.getY()) == 1) {
            if (ticksDesdeUltimoAtaque >= ticksParaAtaque) {
                atacar(jugadorObjetivo);
                ticksDesdeUltimoAtaque = 0;
            }
            return; 
        }
        
        int targetX = jugadorObjetivo.getX();
        int targetY = jugadorObjetivo.getY();
        int currentX = this.x;
        int currentY = this.y;
        int dx = targetX - currentX;
        int dy = targetY - currentY;
        String mejorDireccion = "";
        int newX = currentX, newY = currentY;

        if (Math.abs(dx) > Math.abs(dy)) {
            if (dx > 0) { mejorDireccion = "RIGHT"; newX++;} 
            else if (dx < 0) { mejorDireccion = "LEFT"; newX--;}
        } else if (Math.abs(dy) > Math.abs(dx)) {
            if (dy > 0) { mejorDireccion = "DOWN"; newY++;}
            else if (dy < 0) { mejorDireccion = "UP"; newY--;}
        } else if (dx != 0) { 
             if (dx > 0) { mejorDireccion = "RIGHT"; newX++;} 
             else { mejorDireccion = "LEFT"; newX--;}
        } else if (dy != 0) { 
            if (dy > 0) { mejorDireccion = "DOWN"; newY++;}
            else { mejorDireccion = "UP"; newY--;}
        }

        if (!mejorDireccion.isEmpty() && puedeMover(newX, newY, mapa)) {
            this.x = newX;
            this.y = newY;
            this.direccionActual = mejorDireccion;
        } else { 
            newX = currentX; newY = currentY; 
            if (Math.abs(dx) <= Math.abs(dy) && dx != 0) { 
                 if (dx > 0) { mejorDireccion = "RIGHT"; newX++;} 
                 else { mejorDireccion = "LEFT"; newX--;}
            } else if (Math.abs(dy) <= Math.abs(dx) && dy != 0) { 
                if (dy > 0) { mejorDireccion = "DOWN"; newY++;}
                else { mejorDireccion = "UP"; newY--;}
            }

            if (!mejorDireccion.isEmpty() && puedeMover(newX, newY, mapa)) {
                this.x = newX;
                this.y = newY;
                this.direccionActual = mejorDireccion;
            } else {
                this.direccionActual = obtenerDireccionAleatoria();
            }
        }
    }

    private void atacar(Jugador jugador) {
        // Nueva condición de la versión B
        if (jugador.isLlegoMeta()) {
            System.out.println("Zombie " + id + " intenta atacar a " + jugador.getNombre() + ", pero ya llegó a la salida. No se permite el daño.");
            return; 
        }
        System.out.println("Zombie " + id + " ataca a " + jugador.getNombre());
        jugador.recibirDano(DAMAGE);
    }

    private boolean puedeMover(int newX, int newY, char[][] mapa) {
        if (newY < 0 || newY >= mapa.length || newX < 0 || newX >= mapa[0].length) {
            return false; 
        }
        if (mapa[newY][newX] == 'X') {
            return false; 
        }
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
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public String getId() { return id; }
    public int getVidas() { return vidas; }
    public String getDireccionActual() { return direccionActual; } 
    public EstadoZombie getEstado() { return estado; } 
    
    public Zombie() {}
}