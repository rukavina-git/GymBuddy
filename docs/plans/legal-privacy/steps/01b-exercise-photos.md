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
