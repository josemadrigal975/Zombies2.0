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
    private static final long serialVersionUID = 101L; // Nueva version por cambios
    public String nombre;
    public int x, y;
    public boolean vivo = true;
    public int salud = 100;
    public boolean esFrancotirador = false; // NUEVO
    public int municionFrancotirador = 10; // NUEVO

    public Jugador(String nombre, int x, int y) {
        this.nombre = nombre;
        this.x = x;
        this.y = y;
        this.vivo = true;
        this.salud = 100;
        this.esFrancotirador = false;
        this.municionFrancotirador = 10; // Default para francotirador
    }


      public Jugador(Jugador other) {
        this.nombre = other.nombre;
        this.x = other.x;
        this.y = other.y;
        this.vivo = other.vivo;
        this.salud = other.salud;
        this.esFrancotirador = other.esFrancotirador; // NUEVO
        this.municionFrancotirador = other.municionFrancotirador; // NUEVO
    }

    public void recibirDano(int cantidad) {
        if (esFrancotirador) return; // Los francotiradores en muros son inmunes a zombies

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
    
    public boolean esFrancotirador() { return esFrancotirador; } // NUEVO
    public void setEsFrancotirador(boolean esFrancotirador) { this.esFrancotirador = esFrancotirador; } // NUEVO
    public int getMunicionFrancotirador() { return municionFrancotirador; } // NUEVO
    public void setMunicionFrancotirador(int municionFrancotirador) { this.municionFrancotirador = municionFrancotirador; } // NUEVO
    public void gastarBala() { if(this.municionFrancotirador > 0) this.municionFrancotirador--;} //NUEVO

    public Jugador() {} 
}