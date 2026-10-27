"""Original spring variation, synthesized without external samples."""
from generate_lantern_audio import render

if __name__ == "__main__":
    render("sakura", 48, [(1 + i * 3.5, pitch, 0.10) for i, pitch in enumerate(
        [293.66, 369.99, 440, 493.88, 587.33, 493.88, 440, 369.99, 293.66, 440, 587.33, 493.88])], True)
    render("sakura_memory", 14, [(i * 1.35, pitch, 0.13) for i, pitch in enumerate(
        [293.66, 369.99, 440, 587.33, 659.25, 587.33, 440])], True)
