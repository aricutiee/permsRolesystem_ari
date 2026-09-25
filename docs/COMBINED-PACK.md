# Combined server pack

The Gradle asset task merges `design/packs/shockSMPpack-fixed.zip` with generated TurtleRoles badges. Shock SMP asset bytes remain unchanged; TurtleRoles owns the root Minecraft 1.21.11 metadata and the `turtleroles` namespace. Conflicting role paths and unsafe archive paths fail the build. Both sets of assets are embedded in the plugin's hosted ZIP.

The tab list uses `design/reference/shock-smp-logo.png` as a reproducible custom-font glyph. Players who have loaded the pack see the lightning emblem above `SHOCK SMP S1`; the footer refreshes once per second with that viewer's live ping and the server's one-minute TPS. Until the pack finishes loading, the title remains readable without the glyph.

Six additional supplied sprites are stored in `design/reference/tab-shocks`. Ice and time flank the fire emblem; emerald and speed sit at the outer header edges; earth and healing flank the footer statistics. Their baked rotations are -18, +18, -48, +42, +32 and -38 degrees respectively. The generator fits each rotated sprite inside transparent margins with nearest-neighbor sampling. Header glyphs stay within explicitly reserved blank lines; footer glyphs have their own top and bottom padding. Decoration font styling is scoped to individual glyphs and shown only after the combined pack loads.

Updated placement: ice/time now use -8/+8 degree rotations and tighter spacing. Small shocks are 12px and baked into two edge strips, with 180px between their centers. Negative space providers place half of each sprite beyond the measured 180px line, without charging that overhang to the header width. The upper pair sits below the title, near the player row; the lower pair aligns with the footer statistics. Wider player lists or client mods that alter TAB width can move the actual panel edges beyond these decoration anchors; final alignment must be checked in the client.

Latest adjustment: both ice and time use -8 degrees. Edge sprites are enlarged to 16px on 196px strips, with an 8px overhang and the same 180px center spacing. Header/footer ascents are 9/0 to keep the larger sprites vertically centered in their prior positions.

Current main row supersedes the separate main glyphs above: a single 96x28 transparent strip holds the ice/fire/time artwork at 32px center intervals. Rotations -60/-22/+15 degrees follow the user's left-slant/upright/right-slant guides. A space glyph compensates for transparent right margin so the row centers as a 96px unit. The title now uses per-character rainbow colors updated every two ticks; subtracting a six-second wall-clock phase from the horizontal hue coordinate moves the gradient left to right. The title remains readable and animated before pack acceptance, without custom glyphs.

Enlarged main row: now 156x48 with 52px center spacing, preserving those rotations. Five blank header lines reserve 45px before the 47px-ascent bitmap, putting its upper edge 5px inside the header. The measured row width remains below the existing 180px edge-decoration layout.

Current title: the user-supplied transparent `design/reference/shock-smp-wordmark.png` replaces the animated text. Its cropped texture is 192px tall, rendered at 48px with ascent 7. Six following line breaks reserve 54px before the upper edge decorations and player list. The three main shocks remain above the image. Text-only SHOCK SMP is used while the pack is unavailable; ping/TPS refreshes once a second. The earlier rainbow animation is removed as requested.

Build: `gradlew.bat test shadowJar`. Outputs: `build/libs/TurtleRoles-1.0.0.jar`, `build/distributions/TurtleRoles-resource-pack.zip`, and the adjacent `.sha1` file. Update the configured HTTPS URL version and SHA-1 whenever the ZIP changes, then restart. Server pack acceptance remains client-controlled; required mode disconnects players who decline. Accepted packs activate both sets of textures together.

Permission update: Admin has only the requested moderation, inventory inspection and gamemode capabilities, plus case history/reversal. Heal/feed/fly and higher administration require SR Admin or higher. `/invsee` and `/tr invsee` share the protected read-only handler. Actual OP remains Owner-only because arbitrary vanilla/third-party OP commands cannot be universally constrained by this plugin.

Executed validation: 17 automated tests passed; all merged JSON parsed; every supplied Shock SMP asset matches its source bytes; and the transparent tab glyph and font mapping were validated. In-game appearance still requires a Minecraft client check.

Wordmark rendering fix: capped the single-glyph source texture to 240 pixels wide so it fits the Minecraft font atlas, preserving the 48-pixel display height. Added validation for every header bitmap glyph's atlas dimensions and the wordmark mapping. All 18 tests pass. Pack SHA-1: e7f4c1b852dae6b36e411b4f81510e5076c645c5.
