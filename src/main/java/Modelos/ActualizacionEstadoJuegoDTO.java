/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Modelos;

/**
 *
 * @author Evelyn
 */

import Personajes.Jugador;
import Personajes.Zombie;
import java.io.Serializable;
import java.util.List;

public class ActualizacionEstadoJuegoDTO implements Serializable {
    private static final long serialVersionUID = 4L;
    private List<Jugador> jugadores;
    private List<Zombie> zombies;

    public ActualizacionEstadoJuegoDTO(List<Jugador> jugadores, List<Zombie> zombies) {
        this.jugadores = jugadores;
        this.zombies = zombies;
    }

    public List<Jugador> getJugadores() {
        return jugadores;
    }

    public List<Zombie> getZombies() {
        return zombies;
    }
}