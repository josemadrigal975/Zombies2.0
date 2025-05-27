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
    public String nombre;
    public int x, y;
    public boolean vivo = true;
    public int salud = 100;
    public boolean francotirador = false;

    public Jugador(String nombre, int x, int y) {
        this.nombre = nombre;
        this.x = x;
        this.y = y;
        this.vivo = true;
        this.salud = 100;
    }


      public Jugador(Jugador other) {
        this.nombre = other.nombre;
        this.x = other.x;
        this.y = other.y;
        this.vivo = other.vivo;
        this.salud = other.salud;
        this.francotirador = other.francotirador;
    }

    public void recibirDano(int cantidad) {
        if (!vivo) return;
        this.salud -= cantidad;
        System.out.println("Jugador " + nombre + " recibió " + cantidad + " de daño. Salud: " + this.salud);
        if (this.salud <= 0) {
            this.salud = 0;
            this.vivo = false;
            System.out.println("Jugador " + nombre + " ha muerto.");
            // Aquí podría enviarse un mensaje al servidor o el servidor detectar la muerte
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
    public int getSalud() { return salud; } // Getter para salud
    public void setSalud(int salud) { this.salud = salud; } // Setter para salud
    public boolean isFrancotirador() { return francotirador; }
    public void setFrancotirador(boolean francotirador) { this.francotirador = francotirador; }

    public Jugador() {} // Constructor vacío para deserialización
}

