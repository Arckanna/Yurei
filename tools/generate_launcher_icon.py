"""Reproducible vector launcher artwork and legacy density exports (Pillow).

Coordinates share Android's 108-unit adaptive-icon viewport. The central
66-unit safe zone contains the dragon, moon and petals.
"""
from pathlib import Path
import re
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app/src/main/res"
NIGHT = "#14243B"
# Closed silhouettes, deliberately legible without fine strokes.
PATHS = [
    ("#E6C98F", "M 65 31 C 77 37 80 53 72 63 C 66 72 54 73 45 67 C 59 71 71 61 70 49 C 70 42 68 36 65 31 Z"),
    ("#EDF3F1", "M 41 39 C 29 43 26 57 33 68 C 40 81 60 84 74 72 C 67 84 44 89 31 75 C 18 61 24 41 36 35 Z"),
    ("#BCD6D9", "M 30 51 C 25 64 36 77 49 79 C 60 81 68 77 74 72 C 64 83 46 83 36 74 C 29 68 27 60 30 51 Z"),
    # Swept horns and neck crest.
    ("#E6C98F", "M 37 37 C 33 31 34 27 37 24 C 36 30 40 32 42 33 L 42 38 Z"),
    ("#EDF3F1", "M 31 41 L 27 35 L 35 37 L 34 31 L 41 35 Z"),
    # Long muzzle, brow and cheek distinguish the dragon from a snake.
    ("#EDF3F1", "M 35 34 C 40 30 47 31 51 36 L 59 39 C 62 40 62 44 58 46 L 50 47 C 46 53 39 53 35 49 C 30 46 30 39 35 34 Z"),
    ("#BCD6D9", "M 35 46 C 40 49 45 48 49 44 L 58 43 C 60 43 61 42 61 41 C 63 45 57 47 50 47 C 46 53 38 52 35 46 Z"),
    (NIGHT, "M 43 37 Q 47 36 49 39 Q 45 42 43 37 Z"),
    (NIGHT, "M 56 40 Q 59 39 59 41 Q 57 43 56 40 Z"),
    # Flowing whisker as a filled tapered shape.
    ("#EDF3F1", "M 51 46 C 59 49 65 48 67 43 C 67 50 59 52 51 48 Z"),
    ("#EDF3F1", "M 46 49 C 49 56 56 57 61 54 C 57 59 49 58 44 51 Z"),
    # Sakura accent, away from the eye and moon.
    ("#E8AFC7", "M 48 61 C 42 60 40 55 43 53 C 47 52 50 56 48 61 Z"),
    ("#F1C7D8", "M 50 61 C 49 55 53 51 56 54 C 58 57 54 61 50 61 Z"),
    ("#D993B2", "M 49 63 C 54 61 58 64 56 67 C 53 70 49 67 49 63 Z"),
]


def polygon(data):
    tokens = re.findall(r"[MLCQZ]|-?\d+(?:\.\d+)?", data)
    i, current, points = 0, (0, 0), []
    while i < len(tokens):
        cmd = tokens[i]; i += 1
        count = {"M": 2, "L": 2, "Q": 4, "C": 6, "Z": 0}[cmd]
        v = list(map(float, tokens[i:i + count])); i += count
        if cmd in ("M", "L"):
            current = tuple(v); points.append(current)
        elif cmd in ("Q", "C"):
            start = current
            for step in range(1, 41):
                t = step / 40; u = 1 - t
                if cmd == "Q":
                    point = tuple(u*u*start[k] + 2*u*t*v[k] + t*t*v[k+2] for k in (0, 1))
                else:
                    point = tuple(u**3*start[k] + 3*u*u*t*v[k] + 3*u*t*t*v[k+2] + t**3*v[k+4] for k in (0, 1))
                points.append(point)
            current = points[-1]
    return points


def vector(paths):
    return '<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">\n' + ''.join(
        f'    <path android:fillColor="{color}" android:pathData="{path}" />\n' for color, path in paths) + '</vector>\n'


def render(pixels, round_icon=False, monochrome=False):
    scale = 8
    image = Image.new("RGBA", (pixels*scale, pixels*scale), NIGHT if not monochrome else "#D4DFE8")
    draw = ImageDraw.Draw(image)
    for color, path in PATHS:
        if monochrome:
            color = "#D4DFE8" if color == NIGHT else "#243B51"
        # Launcher masks expose the central 72 units of the 108-unit layers.
        points = [((x-18)/72*pixels*scale, (y-18)/72*pixels*scale) for x,y in polygon(path)]
        draw.polygon(points, fill=color)
    mask = Image.new("L", image.size, 0)
    d = ImageDraw.Draw(mask)
    if round_icon:
        d.ellipse((0, 0, image.width-1, image.height-1), fill=255)
    else:
        d.rounded_rectangle((0, 0, image.width-1, image.height-1), radius=image.width*.23, fill=255)
    image.putalpha(mask)
    return image.resize((pixels, pixels), Image.Resampling.LANCZOS)


if __name__ == "__main__":
    (RES / "drawable/ic_launcher_foreground.xml").write_text(vector(PATHS), encoding="utf-8")
    (RES / "drawable/ic_launcher_background.xml").write_text(vector([(NIGHT, "M 0 0 L 108 0 L 108 108 L 0 108 Z")]), encoding="utf-8")
    # Eye and nostril are transparent holes in themed icons, not opaque dark paint.
    mono = [("#FFFFFF", p) for c,p in PATHS if c != NIGHT]
    # Even-odd cutouts are attached to the head silhouette below.
    head = mono[5][1] + " " + PATHS[7][1] + " " + PATHS[8][1]
    mono[5] = ("#FFFFFF", head)
    # Remove the shaded cheek in monochrome so it cannot fill the eye cutouts.
    mono.pop(6)
    xml = vector(mono).replace('android:fillColor=', 'android:fillType="evenOdd" android:fillColor=')
    (RES / "drawable/ic_launcher_monochrome.xml").write_text(xml, encoding="utf-8")
    for name in ("ic_launcher", "ic_launcher_round"):
        path = RES / f"mipmap-anydpi-v26/{name}.xml"
        s = path.read_text(encoding="utf-8").replace('<monochrome android:drawable="@drawable/ic_launcher_foreground"', '<monochrome android:drawable="@drawable/ic_launcher_monochrome"')
        path.write_text(s, encoding="utf-8")
    for density,pixels in [("mdpi",48),("hdpi",72),("xhdpi",96),("xxhdpi",144),("xxxhdpi",192)]:
        for rounded in (False,True):
            render(pixels, rounded).save(RES / f'mipmap-{density}/ic_launcher{"_round" if rounded else ""}.webp', lossless=True)
    preview = ROOT / "app/build/icon-review"
    preview.mkdir(parents=True, exist_ok=True)
    sheet = Image.new("RGB", (960, 360), "#F3F0EA")
    for i,icon in enumerate([render(280), render(280,True), render(280,True,True)]):
        sheet.paste(icon,(20+i*320,20),icon)
    for i,icon in enumerate([render(48),render(48,True),render(48,True,True)]):
        sheet.paste(icon,(136+i*320,305),icon)
    sheet.save(preview / "yurei-icons.png")
