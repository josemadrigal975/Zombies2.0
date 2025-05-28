/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Modelos;

/**
 *
 * @author jos_m
 */


public enum TipoMensaje {
    PUBLICO,
    PRIVADO,
    DISPARO,
    MOVER,
    LLEGO_META,
    ACTIVAR_CONTROLES,
    ACTUALIZAR_TIEMPO,
    REINICIAR_JUEGO,
    CONTROL,
    VOLVER_LOBBY,
    INICIALIZAR, // Podría usarse para enviar el estado inicial completo, incluyendo zombies
    FINALIZAR_JUEGO,
    ACTUALIZAR_JUGADORES,    // Para la lista de nombres en el lobby/combobox
    ACTUALIZAR_ESTADO_JUEGO  // NUEVO: Para posiciones de jugadores, zombies, salud, etc.
    // ACTUALIZAR_POSICIONES ya no se usará directamente, se reemplaza por ACTUALIZAR_ESTADO_JUEGO
}