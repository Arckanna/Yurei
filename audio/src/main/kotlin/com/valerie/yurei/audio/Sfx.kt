package com.valerie.yurei.audio

/**
 * Effets sonores courts (SoundPool).
 * Les noms correspondent aux fichiers dans res/raw/ (sans extension).
 * Ex. soul_collect → res/raw/soul_collect.ogg
 */
enum class Sfx(val rawName: String) {
    SoulCollect("soul_collect"),
    Pause("pause"),
    Resume("resume"),
    GameOver("game_over"),
    ButtonTap("button_tap")
}
