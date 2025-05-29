/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Personajes;

import java.io.Serializable;

/**
 *
 * @author jos_m
 */
public class Jugador implements Serializable {
    private static final long serialVersionUID = 102L; // Actualizado serialVersionUID
    public String nombre;
    public int x, y;
    public boolean vivo = true;
    public int salud = 100;
    public boolean esFrancotirador = false;
    public int municionFrancotirador = 10;
    private long tiempoEscape = 0; // Nuevo de la versión B
    private boolean notificadoLlegada = false; // Nuevo de la versión B
    private boolean llegoMeta = false; // Nuevo de la versión B

    public Jugador(String nombre, int x, int y) {
        this.nombre = nombre;
        this.x = x;
        this.y = y;
        this.vivo = true;
        this.salud = 100;
        this.esFrancotirador = false;
        this.municionFrancotirador = 10; // Default para francotirador
        this.llegoMeta = false;
        this.notificadoLlegada = false;
        this.tiempoEscape = 0;
    }

    public Jugador(Jugador other) {
        this.nombre = other.nombre;
        this.x = other.x;
        this.y = other.y;
        this.vivo = other.vivo;
        this.salud = other.salud;
        this.esFrancotirador = other.esFrancotirador;
        this.municionFrancotirador = other.municionFrancotirador;
        this.llegoMeta = other.llegoMeta; // Nuevo de la versión B
        this.notificadoLlegada = other.notificadoLlegada; // Nuevo de la versión B
        this.tiempoEscape = other.tiempoEscape; // Nuevo de la versión B
    }

    public void recibirDano(int cantidad) {
        if (esFrancotirador) return; // Los francotiradores en muros son inmunes a zombies (lógica de A)
        if (llegoMeta) return; // Si ya llegó a la meta, no recibe daño (lógica de B)

        if (!vivo) return;
        this.salud -= cantidad;
        System.out.println("Jugador " + nombre + " recibió " + cantidad + " de daño. Salud: " + this.salud);
        if (this.salud <= 0) {
            this.salud = 0;
            this.vivo = false;
            System.out.println("Jugador " + nombre + " ha muerto.");
        }
    }
    
    // Getters y Setters
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    public boolean isVivo() { return vivo; }
    public void setVivo(boolean vivo) { this.vivo = vivo; }
    public int getSalud() { return salud; }
    public void setSalud(int salud) { this.salud = salud; }
    
    public boolean esFrancotirador() { return esFrancotirador; }
    public void setEsFrancotirador(boolean esFrancotirador) { this.esFrancotirador = esFrancotirador; }
    public int getMunicionFrancotirador() { return municionFrancotirador; }
    public void setMunicionFrancotirador(int municionFrancotirador) { this.municionFrancotirador = municionFrancotirador; }
    public void gastarBala() { if(this.municionFrancotirador > 0) this.municionFrancotirador--;}

    // Nuevos getters/setters de la versión B
    public long getTiempoEscape() { return tiempoEscape; }
    public void setTiempoEscape(long tiempoEscape) { this.tiempoEscape = tiempoEscape; }
    public boolean isNotificadoLlegada() { return notificadoLlegada; }
    public void setNotificadoLlegada(boolean notificadoLlegada) { this.notificadoLlegada = notificadoLlegada; }
    public boolean isLlegoMeta() { return llegoMeta; }
    public void setLlegoMeta(boolean llegoMeta) { this.llegoMeta = llegoMeta; }

    public Jugador() {} 
}