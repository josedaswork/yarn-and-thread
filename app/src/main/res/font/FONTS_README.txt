# Font note
# The HTML app uses Google Fonts (Courier Prime, Special Elite, Playfair Display).
# For Android, these are included via downloadable fonts or bundled TTF files.
#
# Option A (recommended): Add to res/font/
#   1. Download from fonts.google.com:
#      - courier_prime.ttf
#      - special_elite.ttf
#      - playfair_display.ttf
#   2. Place them in app/src/main/res/font/
#   3. Reference in themes.xml: <item name="android:fontFamily">@font/courier_prime</item>
#
# Option B: Use system fonts (already works — no action needed)
#   The app compiles fine with system fonts. Custom fonts are aesthetic only.
