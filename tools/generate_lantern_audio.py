"""Original, deterministic synthesis. No samples or external recordings.

22.05 kHz mono PCM; ambient loop: soft pentatonic plucks, air and low drone.
Run with Python 3 (stdlib only) to reproduce the shipped WAV resources.
"""
from pathlib import Path
import math
import random
import wave
from array import array

RATE = 22050
DEST = Path(__file__).resolve().parents[1] / "audio/src/main/res/raw"


def render(name, seconds, notes, ambient=False):
    rng = random.Random(22)
    output = array("h")
    air = 0.0
    peak = 0.0
    energy = 0.0
    for index in range(int(seconds * RATE)):
        t = index / RATE
        value = 0.0
        if ambient:
            air = air * 0.985 + rng.uniform(-1, 1) * 0.015
            value += air * 0.15
            value += 0.035 * math.sin(2 * math.pi * 110 * t)
            value += 0.018 * math.sin(2 * math.pi * 165 * t + 0.3 * math.sin(t * 0.2))
        for start, pitch, gain in notes:
            age = t - start
            if 0 <= age < 7:
                envelope = min(1, age / 0.04) * math.exp(-age / 1.6)
                tone = math.sin(2 * math.pi * pitch * age)
                tone += 0.22 * math.sin(2 * math.pi * pitch * 2.01 * age) * math.exp(-age)
                tone += 0.1 * math.sin(2 * math.pi * pitch * 3 * age) * math.exp(-age * 2)
                value += gain * envelope * tone
        # Smooth ends make the loop unobtrusive and avoid clicks on short effects.
        fade = min(1, t / (2 if ambient else 0.02), (seconds - t) / (2 if ambient else 0.2))
        value *= max(0, fade)
        peak = max(peak, abs(value))
        energy += value * value
        output.append(round(max(-0.95, min(0.95, value)) * 32767))
    assert peak < 0.95, (name, peak)
    assert energy > 0, name
    DEST.mkdir(parents=True, exist_ok=True)
    with wave.open(str(DEST / f"{name}.wav"), "wb") as file:
        file.setnchannels(1)
        file.setsampwidth(2)
        file.setframerate(RATE)
        file.writeframes(output.tobytes())
    print(f"{name}: {seconds}s, peak={peak:.3f}, rms={math.sqrt(energy / len(output)):.3f}")


if __name__ == "__main__":
    pitches = [220, 261.63, 293.66, 392, 440, 392, 293.66, 261.63]
    render("ambient", 48, [(2 + i * 5, p, 0.12) for i, p in enumerate(pitches)], True)
    render("memory", 12, [(i * 1.2, p, 0.15) for i, p in enumerate([220, 293.66, 392, 440, 523.25, 587.33])], True)
    render("soul_collect", 1.5, [(0, 660, 0.16), (0.15, 880, 0.08)])
    render("pause", 0.7, [(0, 330, 0.06)])
    render("resume", 0.8, [(0, 440, 0.06)])
    render("game_over", 1.5, [(0, 220, 0.08)])
    render("button_tap", 0.3, [(0, 660, 0.04)])
