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
    DISPARO, // Para disparos normales
    DISPARO_FRANCOTIRADOR, // Mantenido de la versión A
    MOVER,
    LLEGO_META, // Nuevo de la versión B
    ACTIVAR_CONTROLES, // Nuevo de la versión B
    ACTUALIZAR_TIEMPO, // Nuevo de la versión B
    REINICIAR_JUEGO, // Nuevo de la versión B
    CONTROL, // Nuevo de la versión B (para comandos como INICIAR_JUEGO desde el cliente o SALIR_PARTIDA)
    VOLVER_LOBBY, // Nuevo de la versión B
    INICIALIZAR,
    FINALIZAR_JUEGO,
    ACTUALIZAR_JUGADORES,    // Para la lista de nombres en el lobby/combobox
    ACTUALIZAR_ESTADO_JUEGO  // Para posiciones de jugadores, zombies, salud, etc.
}