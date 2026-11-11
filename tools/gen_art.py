"""Generates pet sprite sheets, the tea house and its things with the Codex CLI image tool.

Codex saves images into ~/.codex/generated_images/<session>/; after each run the image of that session
is copied to art/sheets/<id>.png. Already present sheets are skipped.

    python tools/gen_art.py              # everything missing
    python tools/gen_art.py puffin seal  # only these
"""
import re
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "art" / "sheets"
GEN = Path.home() / ".codex" / "generated_images"

STYLE = (
    "Style: detailed 16-bit pixel art like a modern cozy indie game, crisp pixels, clean dark outline, "
    "3-4 tone shading ramps, soft calm pastel-warm palette, cute, big gentle eyes."
)
GRID = (
    "Layout: 3 columns x 2 rows of equal square cells on a flat solid pure magenta (#FF00FF) background. "
    "One creature per cell, centered, with clear margin; nothing touches the cell borders. No text, no grid lines, "
    "no shadows on the background, no other objects except those listed. Never use magenta or hot pink in the creature."
)

# id: (look, calm practice pose)
PETS = {
    "puffin": ("a round Atlantic puffin chick with soft dark grey fluff, a white face, a small orange-and-grey beak and orange feet, a tiny teal knitted scarf",
               "sitting cross-legged in meditation on a small round cushion, wings folded, eyes peacefully closed"),
    "puffer": ("a round yellow-beige pufferfish with soft small spines, tiny fins and a little lavender bandana",
               "puffed up into a perfect round ball holding its breath, eyes closed, a few tiny air bubbles above"),
    "macaque": ("a Japanese snow macaque with fluffy grey-brown fur, a pink face and a small folded white towel on its head",
                "sitting chest-deep in a small steaming hot spring tub, eyes closed, snowflakes falling"),
    "sloth": ("a fluffy beige sloth with a mossy green tint and a sleepy smile, a sprig of leaves behind one ear",
              "hanging from a short branch with both arms, breathing slowly with eyes closed, very relaxed"),
    "jelly": ("a small glowing jellyfish with a translucent soft lilac bell and gentle trailing tentacles, a cute face",
              "pulsing gently with a soft glow ring around it, eyes closed"),
    "owl": ("a round brown tawny owl with big amber eyes and a dark blue knitted shawl",
            "meditating next to a small glowing paper lantern, eyes half closed"),
    "heron": ("a slender grey heron with a white face, a black crest feather and a tiny straw hat on its back",
              "walking one slow mindful step, one leg lifted, eyes calm"),
    "moth": ("a fluffy cream-white moth with soft feathery antennae and pale gold wing patterns",
             "sitting and gazing calmly at a small lit candle flame in front of it"),
    "koala": ("a grey koala with fluffy ears wearing a blue striped nightcap",
              "hugging a soft pillow, breathing slowly, almost asleep"),
    "hermit": ("a little hermit crab with a red-orange body living in a pale blue spiral shell",
               "peeking out of its shell holding a tiny steaming cup of tea in a claw, relieved smile"),
    "axolotl": ("a pink-peach axolotl with frilly coral gills and a happy smile",
                "floating calmly with its gills softly glowing, eyes closed"),
    "whale": ("a small round blue whale calf with a pale belly and a few white spots",
              "floating peacefully, breathing out a gentle spout of water mist, eyes closed"),
    "panda": ("a red panda with a fluffy striped tail and a small green neckerchief",
              "holding a little heart-shaped autumn leaf to its chest, grateful smile, eyes closed"),
    "capybara": ("a calm brown capybara with a small orange yuzu fruit on its head",
                 "sitting very still and listening, a small bronze bell beside it with a soft ring line"),
    "cat": ("a fluffy grey tabby cat with white paws and a lavender collar with a tiny bell",
            "lying in a perfect loaf pose, eyes closed, purring"),
    "frog": ("a round green tree frog with a cream belly and a small lily-pad hat",
             "singing a gentle phrase with its throat pouch puffed out, eyes happy"),
    "tortoise": ("a little tortoise with a domed olive shell with honey-coloured plates and a tiny red knitted beanie",
                 "breathing out slowly, a small soft white cloud of breath leaving its mouth, eyes closed"),
    "fox": ("a red fox kit with a fluffy white-tipped tail and a small sky-blue scarf",
            "sitting still and watching a single maple leaf fall in front of it, calm attentive eyes"),
    "seal": ("a round grey harbour seal pup with dark speckles and big soft eyes",
             "balancing a small wooden cube on the tip of its nose, perfectly still, focused calm"),
    "dragon": ("a small mist dragon with pearl-white scales, soft lilac mane, tiny cloud-like wings and little horns",
               "curled on a small cloud, meditating, breathing out a ribbon of soft mist"),
    "chameleon": ("a small round chameleon with a curly tail, soft pastel scales that shift from mint to pink to lilac, big gentle eyes",
                  "holding a tiny heart that glows, its skin softly changing colour in rainbow pastel bands"),
    "raven": ("a fluffy young raven with glossy blue-black feathers, a tiny round pair of spectacles and a little cream scarf",
              "writing in a small open leather diary with a feather quill, a tiny inkpot beside it"),
    "snail": ("a cute snail with a soft peach body and a spiral shell that looks like a tiny cosy house with a round window and a little chimney",
              "reading a tiny open notebook, a small lamp glowing in its shell window"),
}


def pet_prompt(desc, signature):
    return f"""Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: a sprite sheet for a cozy meditation and breathing app pet: {desc}.
{STYLE}
{GRID}
Cells, left to right, top to bottom:
1) baby: tiny, round dumpling-shaped body, stubby limbs, very cute
2) teen: bigger, a bit more grown up
3) adult: grown, cute, standing or sitting calmly, content, eyes open
4) adult {signature}
5) adult happy and glowing with warm joy, eyes closed in a big soft smile, two tiny hearts
6) adult asleep, curled up, peaceful face
The same character and palette in every cell, facing three-quarters right."""


OTHER = {
    "eggs": """Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: a sprite sheet of 20 pet eggs for a cozy meditation app.
""" + STYLE + """
Layout: 5 columns x 4 rows of equal square cells on a flat solid pure magenta (#FF00FF) background. One egg per cell, centered, with clear margin; no text, no grid lines. Never use magenta or hot pink in the eggs.
Each egg is the same size and egg-shaped, but with its own pattern and colours that hint at the creature inside, in this order (left to right, top to bottom):
1 dark grey with white face patch and an orange stripe (puffin), 2 pale yellow with soft spines (pufferfish), 3 grey-brown fluffy with a pink spot and snowflakes (snow macaque), 4 mossy beige with leaves (sloth), 5 translucent glowing lilac (jellyfish),
6 brown with feather scallops and amber spots (owl), 7 pale grey with a black streak (heron), 8 cream with soft gold moth patterns (moth), 9 grey fluffy with a blue nightcap stripe (koala), 10 pale blue spiral shell pattern (hermit crab),
11 pink-peach with coral frills (axolotl), 12 blue with white spots (whale), 13 rusty red with tail stripes (red panda), 14 brown with a small yuzu-orange dot (capybara), 15 grey tabby stripes (cat),
16 green with cream belly and a lily pad pattern (frog), 17 olive with honey hexagon plates (tortoise), 18 orange-red with a white tip (fox), 19 grey with dark speckles (seal), 20 pearl white with lilac mist swirls, softly glowing (dragon).""",
    "room": """Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: a background scene for a cozy meditation app: the inside of a tiny wooden tea house on a sea cliff, seen from the front, side-on like a 2D game room.
""" + STYLE + """ Landscape 3:2.
The room: warm honey-coloured wooden walls and beams, tatami-like woven floor, soft warm lamp light. In the middle of the back wall a LARGE round moon window (about half the image height, centred horizontally, in the upper half) with a thin wooden frame and a few wooden lattice bars only at its very edge.
IMPORTANT: the glass area of the round window must be filled with flat solid pure magenta (#FF00FF) – the sky and sea will be painted there later. Use magenta nowhere else.
Keep the room mostly EMPTY: no people, no animals, no text, no scrolls with writing. The centre of the floor and the walls left and right of the window must be clear, free space (furniture will be added later).
Only small details: a low wooden ledge under the window, a paper lamp hanging from the ceiling at the left, a woven rug edge.""",
    "items": """Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: a sprite sheet of 16 objects for a cozy meditation tea house, each a separate object, seen from the front (side-on like a 2D game room).
""" + STYLE + """
Layout: 4 columns x 4 rows of equal square cells on a flat solid pure magenta (#FF00FF) background. One object per cell, centered, with clear margin; no text, no grid lines, no shadows on the background. Never use magenta in the objects.
Objects in order (left to right, top to bottom):
1 a brass singing bowl on a small cushion with a wooden striker, 2 a bonsai pine in a shallow blue pot, 3 a small stone incense holder with a curl of smoke, 4 a big bronze gong on a wooden stand,
5 a low tea table with a clay teapot and two cups, 6 a round zafu meditation cushion, deep blue, 7 a hanging wind chime of bamboo and shells, 8 a tall paper floor lantern glowing warm,
9 a shelf with seashells and smooth stones, 10 a potted monstera-like fern, 11 a small tabletop zen sand garden with rake lines and three stones, 12 a cluster of three lit candles on a stone,
13 a hanging scroll with a painted wave (no letters), 14 a glass fishbowl with a single orange goldfish, 15 a brass telescope on a tripod, 16 a woven basket with a folded wool blanket.""",
    "deep": """Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: a sprite sheet of 16 sea finds for a cozy breathing app, where a pet dives deeper the longer you hold your breath.
""" + STYLE + """
Layout: 4 columns x 4 rows of equal square cells on a flat solid pure magenta (#FF00FF) background. One thing per cell, centered, with clear margin; no text, no grid lines. Never use magenta or hot pink in them.
In order from shallow to deep (left to right, top to bottom):
1 a pink starfish, 2 a striped clownfish, 3 a curly green seahorse, 4 a sea urchin with violet spines,
5 a swaying kelp frond with a small snail, 6 a friendly sea turtle, 7 an eagle ray, 8 a pearl in an open oyster,
9 a lost ship's brass bell covered in tiny barnacles, 10 a small octopus hiding in a bottle, 11 a glowing lanternfish, 12 a ghostly blue-white jellyfish,
13 a bioluminescent anglerfish with a soft glowing lure (cute, not scary), 14 a giant squid tentacle curling from the dark, 15 a sunken stone statue head with seaweed, 16 a glowing deep sea crystal cluster in cyan.""",
    "eggs2": """Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: a sprite sheet of 3 pet eggs for a cozy meditation app.
""" + STYLE + """
Layout: 3 columns x 1 row of equal square cells on a flat solid pure magenta (#FF00FF) background. One egg per cell, centered, with clear margin; no text, no grid lines. Never use magenta or hot pink in the eggs.
Each egg is the same size and egg-shaped, with its own pattern, in this order: 1 soft pastel rainbow scales mint-pink-lilac (chameleon), 2 glossy blue-black with a small white quill feather pattern (raven), 3 peach with a spiral shell pattern and a tiny round window (snail).""",
    "logo": """Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: an app icon character: the head and shoulders of a round Atlantic puffin chick with soft dark grey fluff, a white face, a small orange-and-grey beak, eyes peacefully closed in calm meditation, a tiny teal knitted scarf.
""" + STYLE + """ Square, the chick centred and filling about 70% of the image, on a flat solid pure magenta (#FF00FF) background. No text, no frame.""",
}


def image_of(output):
    """The image made in the Codex session named in its output (other sessions may draw at the same time)."""
    m = re.search(r"session id:\s*([0-9a-f-]+)", output)
    if not m:
        return None
    pngs = sorted((GEN / m.group(1)).glob("*.png"), key=lambda p: p.stat().st_mtime)
    return pngs[-1] if pngs else None


def run(name, prompt):
    dst = OUT / f"{name}.png"
    if dst.exists():
        print("skip", name)
        return
    r = subprocess.run([shutil.which("codex.cmd") or "codex", "exec", "--skip-git-repo-check", "--sandbox", "read-only", "-"],
                       input=prompt, text=True, encoding="utf-8", capture_output=True, cwd=str(ROOT))
    p = image_of(r.stdout + r.stderr)
    if p is None:
        print("FAIL", name, r.stdout[-400:], r.stderr[-400:], flush=True)
        return
    shutil.copy(p, dst)
    print("ok", name, p, flush=True)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    want = sys.argv[1:]
    jobs = [(k, pet_prompt(*v)) for k, v in PETS.items()] + list(OTHER.items())
    for name, prompt in jobs:
        if want and name not in want:
            continue
        run(name, prompt)


if __name__ == "__main__":
    main()
