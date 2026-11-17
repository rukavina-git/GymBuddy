# 01b Exercise photos from a clear source

## Problem
The 60 built-in exercises show photos hotlinked from
github.com/yuhonas/free-exercise-db. The repo is Unlicense, but the
photos' origin isn't stated, so they can't be used commercially with
confidence. Owner (2026-11-13): replace before release; real photos
or animations preferred, drawings last resort.

## Research (2026-11-13)
- **Gym Visual** (gymvisual.com): realistic 3D exercise animations
  sold royalty-free for apps. The original source of many
  animations copied elsewhere. Terms and prices not public to us;
  needs a written quote for ~60 animations in a paid app.
- **MoveKit**: pay-once clips (a listing says from $1.99 per clip,
  packs up to $99) with a paid-app licence. Seen only on an
  aggregator; terms must be checked on the vendor site.
- **WorkoutLabs**: illustrated animations, $1,200/year or $3,500+
  perpetual. Drawings, expensive: no.
- **ExerciseDB** (Gumroad, one-time): 1,500 GIFs with "commercial
  rights", but a third-party write-up warns the media may carry
  copyright issues (Gym Visual look). Not without proof of origin.
- **wger**: per-image Creative Commons, much of it BY-SA. SA is
  banned: no.
- **Free stock** (Pexels, Unsplash, Pixabay): real photos, free
  commercial use without credit, but their own licences (not on the
  owner's list) and poor coverage of specific lifts. Gap filler only.
- **Own photos**: the owner (or a friend) shoots each exercise in a
  gym. Full ownership, consistent look, no cost but time.

## Recommendation
Ask Gym Visual and MoveKit for written terms and a price for the 60
exercises (in-app use, paid app, perpetual). Pick the clearer and
cheaper; own photos are the fallback. Whatever is chosen: bundled or
hosted by us (no hotlinking), recorded in DESIGN.md §10 with the
licence or invoice, listed on the licences page if the licence needs
credit.

## Owner decisions (2026-11-13)
- No paid images. Current photos dropped: backend V8 clears the
  hotlinked URLs and bumps the exercise library to v10; the bundled
  asset drops them too. Exercise pages show the stand-in.
- New images: drawn by us (Claude), not photographed by the owner.
  Start with a few exercises to test the style, owner reviews, then
  the rest.
- Images come from an AI image model (ChatGPT/Gemini); both allow
  commercial use. Keep the chats as the record of where and when each
  image was made. Every image is checked for correct technique.
- Tried (2026-11-13): flat cartoon and 3D mannequin, single image,
  3-frame strip and start/end pairs. Owner's choice: **one image per
  exercise, 3D mannequin, working position, working muscles tinted
  (matte warm orange, no glow)**. Frames dropped: more images, more
  chances to show something wrong.
- Next: lock the prompt on 2-3 exercises, then the full set of 60.
  Images bundled in the app (WebP), no hotlinking.
- Workflow (2026-11-13): Gemini, 15 prompts × 2×2 grids = 60 images,
  grouped by equipment. Prompts: `design/exercise-images/prompts.html`
  (also the owner's tracker artifact). Owner only pastes and sends;
  Claude reviews every tile, writes retries for failed tiles, crops,
  sizes and bundles them. Exercises without a good image ship with the
  stand-in.

## Daily process (owner on free Gemini, a few grids a day, no rush)
1. Owner generates the next grid(s) from the tracker artifact (same as
   `design/exercise-images/prompts.html`; odd grids male, even female)
   and sends the images to Claude, saying which tiles are good.
2. Claude saves the original to `design/exercise-images/raw/grid-NN.jpg`,
   cuts the good tiles (506 px squares, offsets 1/517), saves them as
   `android/app/src/main/res/drawable-nodpi/ex_<name>.jpg` (JPEG 85),
   adds the id → drawable line in `ExerciseImages.kt`, runs
   `ExerciseImagesTest`, commits.
3. Failed tiles: Claude writes a single-image retry prompt.
Layout (owner, 2026-11-13): centred square at 60% width, rounded
corners (16:9 crop cut heads and feet). Exercise list stays text-only
until most exercises have images.

## Status
Done: #2 Back Squat, #56 Front Squat, #3 Conventional Deadlift,
#59 Sumo Deadlift (grid 01). Next: grid 02.
