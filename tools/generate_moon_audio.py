"""Original nocturnal pentatonic score, synthesized without external samples."""
from generate_lantern_audio import render

if __name__ == "__main__":
    render("moon_shrine", 48, [(2 + i * 4, pitch, .085) for i, pitch in enumerate(
        [146.83, 220, 261.63, 293.66, 392, 293.66, 261.63, 220, 146.83, 196])], True)
    render("moon_memory", 16, [(i * 1.6, pitch, .12) for i, pitch in enumerate(
        [196, 220, 293.66, 392, 440, 392, 293.66])], True)
