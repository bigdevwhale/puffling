"""Cuts the Codex sheets in art/sheets into app drawables.

    python tools/cut.py

Pets (3×2): pet_<id>_<0..5>.webp – baby, teen, adult, practice, joy, asleep.
Eggs (5×4): egg_<id>.webp. Items (4×4): item_<id>.webp. Sea finds (4×4): find_<id>.webp.
The room keeps its moon window see-through (room_bg.webp); the logo becomes the launcher foreground.
The magenta background is keyed out; each frame is cropped to its creature.
"""
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
SHEETS = ROOT / "art" / "sheets"
OUT = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"

PETS = ["puffin", "puffer", "macaque", "sloth", "jelly", "owl", "heron", "moth", "koala", "hermit",
        "axolotl", "whale", "panda", "capybara", "cat", "frog", "tortoise", "fox", "seal", "dragon"]  # the order of the egg sheet
DIARY = ["chameleon", "raven", "snail"]  # the order of the second egg sheet
ITEMS = ["bowl", "bonsai", "incense", "gong", "tea", "cushion", "chime", "lantern",
         "shelf", "plant", "garden", "candles", "scroll", "fishbowl", "telescope", "blanket"]
FINDS = ["star", "clown", "horse", "urchin", "kelp", "turtle", "ray", "pearl",
         "bell", "octopus", "lantern", "ghost", "angler", "squid", "statue", "crystal"]
MAX = 320


def key(im):
    im = im.convert("RGBA")
    px = im.load()
    w, h = im.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            m = min(r, b) - g  # how magenta
            if m > 90:
                px[x, y] = (0, 0, 0, 0)
            elif m > 40:
                k = (m - 40) / 50
                px[x, y] = (int(r - (r - g) * k), g, int(b - (b - g) * k), int(255 * (1 - k)))
    return im


def frames(sheet, cols, rows, keep=MAX):
    im = key(Image.open(sheet))
    w, h = im.size
    cw, ch = w / cols, h / rows
    out = []
    for j in range(rows):
        for i in range(cols):
            cell = im.crop((int(i * cw), int(j * ch), int((i + 1) * cw), int((j + 1) * ch)))
            box = cell.getchannel("A").point(lambda v: 255 if v > 30 else 0).getbbox()
            cell = cell.crop(box) if box else cell
            s = keep / max(cell.size)
            if s < 1:
                cell = cell.resize((max(1, round(cell.width * s)), max(1, round(cell.height * s))), Image.LANCZOS)
            out.append(cell)
    return out


def save(im, name):
    im.save(OUT / f"{name}.webp", "WEBP", quality=90, method=6)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for p in PETS + DIARY:
        f = SHEETS / f"{p}.png"
        if not f.exists():
            print("missing", p)
            continue
        for n, fr in enumerate(frames(f, 3, 2)):
            save(fr, f"pet_{p}_{n}")
        print("pet", p)
    if (SHEETS / "eggs.png").exists():
        for name, fr in zip(PETS, frames(SHEETS / "eggs.png", 5, 4)):
            save(fr, f"egg_{name}")
        print("eggs")
    if (SHEETS / "eggs2.png").exists():
        for name, fr in zip(DIARY, frames(SHEETS / "eggs2.png", 3, 1)):
            save(fr, f"egg_{name}")
        print("eggs2")
    if (SHEETS / "items.png").exists():
        for name, fr in zip(ITEMS, frames(SHEETS / "items.png", 4, 4)):
            save(fr, f"item_{name}")
        print("items")
    if (SHEETS / "deep.png").exists():
        for name, fr in zip(FINDS, frames(SHEETS / "deep.png", 4, 4, 256)):
            save(fr, f"find_{name}")
        print("finds")
    if (SHEETS / "room.png").exists():
        g = key(Image.open(SHEETS / "room.png"))
        g = g.resize((960, round(960 * g.height / g.width)), Image.LANCZOS)
        g.save(OUT / "room_bg.webp", "WEBP", quality=88, method=6)
        print("room")
    if (SHEETS / "logo.png").exists():
        # adaptive icon foreground: 432 px, the chick inside the 66/108 safe circle
        lg = frames(SHEETS / "logo.png", 1, 1, 1024)[0]
        side = 432; inner = int(side * 60 / 108)
        lg.thumbnail((inner, inner), Image.LANCZOS)
        fg = Image.new("RGBA", (side, side), (0, 0, 0, 0))
        fg.paste(lg, ((side - lg.width) // 2, (side - lg.height) // 2 + 8), lg)
        fg.save(OUT / "ic_launcher_fg.png")
        lg2 = frames(SHEETS / "logo.png", 1, 1, 512)[0]
        bg = Image.new("RGBA", (512, 512), (0x5F, 0xA8, 0xC9, 255))
        lg2.thumbnail((400, 400), Image.LANCZOS)
        bg.paste(lg2, ((512 - lg2.width) // 2, (512 - lg2.height) // 2 + 20), lg2)
        (ROOT / "docs").mkdir(exist_ok=True)
        bg.save(ROOT / "docs" / "logo.png")
        print("logo")


if __name__ == "__main__":
    main()
