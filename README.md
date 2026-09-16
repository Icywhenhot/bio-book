Bio Book
========

A character journal for Minecraft 1.21.1 and NeoForge 21.1.243 or newer in the 21.1 series. Uses Mojang mappings and Java 21.

Install
-------

Build with `./gradlew build` on Linux/macOS or `./gradlew.bat build` on Windows. Install `build/libs/biobook-1.0.2.jar` in the mods folder on the server and every player's client. Singleplayer is supported too. No additional mods are required.

Use
---

- Open chat with T and click the skin portrait beside a player message.
- Press B, or use `/bio`, to open your own journal. B can be rebound in Controls.
- Read public character lore in the Lore tab. Scroll with the mouse wheel or Page Up/Page Down.
- Use Edit bio in your own journal to set a character name, tagline, and story.
- My notes holds your personal notes about that character. Other players do not receive these notes.
- Save with the button or Ctrl+S. A saved message appears only after the server acknowledges the change.
- Escape or the close button returns to chat and restores the unfinished chat message.
- Unsaved journal drafts survive closing, reopening, and resizing during the current connection. Save before disconnecting; unsaved drafts are cleared on disconnect.

Storage and limits
------------------

Public bios and personal notes are stored in the world's `data/biobook.dat`, indexed by player UUID. They survive restarts and dimension changes. Back up this file with the world. Server administrators with filesystem access can inspect stored notes; they are not encrypted.

Character names allow 48 characters, taglines 96, and each lore entry or note 4096. The server limits packet sizes, strips control characters and formatting codes, restricts public edits to the sending player's UUID, and returns private notes only to their author.

Presentation
------------

The journal uses an ink-blue palette, brass accents, skin portraits, rounded panels, eased opening and closing movement, animated button highlights, and smooth lore scrolling. It keeps the world running while open. Portraits inherit vanilla chat positioning, wrapping, fading, line spacing, and scale. No image assets or external web requests are needed beyond Minecraft's normal skin handling.

Compatibility
-------------

Portraits attach to player chat messages with an authenticated sender identity supplied by Minecraft. System messages and plugins that replace player messages with anonymous system text do not provide that identity. This mod adds its own portraits; combining it with another chat-head renderer may produce duplicate heads or mixin conflicts. Chat replacement mods need separate compatibility testing.


[NeoForge 1.21.1 networking](https://docs.neoforged.net/docs/1.21.1/networking/payload/)

[NeoForge 1.21.1 saved data](https://docs.neoforged.net/docs/1.21.1/datastorage/saveddata/)
