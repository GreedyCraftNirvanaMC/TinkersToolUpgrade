# Tinkers Tool Upgrade

A Minecraft **1.21.1 / NeoForge** port of boni's MIT-licensed **Tinkers Tool Leveling**, for TinkersNirvana. Optional TinkersArmoryNirvana integration adds progression to modular armor.

## Installation

Use Java 21 and NeoForge 21.1.265 or newer. Install this mod on both the client and server, together with:

- **TinkersNirvana 1.21.1-1.0.3+** (`tconstruct`), required.
- **TinkersArmoryNirvana 1.21.1-1.0.4+** (`conarm`), optional.

Neither dependency is bundled. Importing Forge 1.12.2 worlds/item NBT is not supported.

## Progression

- Effective survival-mode block breaking grants 1 XP per block, including native area mining. Mature crop harvesting with a kama/scythe also counts. Canceled or unsuccessful actions do not award XP.
- Successful tilling and path creation grant 1 XP. Simulation queries do not count.
- Melee damage accumulates on the target and is awarded when it dies, attributed to the actual tool and player. Totems and canceled deaths do not settle rewards. The tool must still be uniquely identifiable in that player's inventory.
- Blocking with a supported tool awards XP based on blocked damage; successful battlesign projectile reflections also count.
- Every equipped, initialized, unbroken modular armor piece receives XP from actual health damage. This is an addon policy; damage fully prevented by armor does not award armor XP.
- Each upgrade grants exactly **one extra modifier slot** through public dependency APIs. Materials, installed modifiers, other addons' rewards and inventory contents are preserved.
- Item XP and identity persist and synchronize. Armor uses the namespaced extension API. Tool levels use `ToolUpgradeApi`; XP uses a separate data component, avoiding per-hit changes to tool material data.
- Level names, XP tooltips, messages and the original level-up chime are included. Original translations are retained with English fallback for new text.

Creative/spectator gameplay and broken equipment do not earn XP. One grant can award multiple upgrades. A configured level cap is exact; lowering it never revokes existing slots. Integer overflow is prevented. Rejected upgrades do not consume their XP cost.

Initial modifier slots are configured by TinkersNirvana itself. This addon only supplies upgrade rewards.

**Ranged launcher integration awaits the dependency's projectile-source API.** Special arrows/bolts expose merged trait snapshots, which cannot safely identify the actual launcher. Integration will use the new API rather than guess from the currently held item.

## Configuration

NeoForge creates `serverconfig/tinkerstoolupgrade-server.toml` in each world and synchronizes it to clients. Put a template in `defaultconfigs/` for new worlds.

| Setting | Default | Meaning |
| --- | --- | --- |
| `general.maximumLevels` | `-1` | Maximum level; `0`/`-1` means unlimited |
| `general.levelUpMessages` | `true` | Upgrade messages |
| `general.levelUpSounds` | `true` | Upgrade chime for its owner |
| `experience.defaultBaseXp` | `500` | First-upgrade tool cost |
| `experience.levelMultiplier` | `2.0` | Subsequent cost multiplier; minimum 2 |
| `experience.baseXpForItems` | Four area tools at `4500` | Entries such as `tconstruct:hammer=4500` override costs |
| `experience.armorBaseXp` | `500` | First-upgrade armor cost |
| `experience.armorXpMultiplier` | `1.0` | XP per point of actual damage, per equipped piece |

The first cost is the base XP; each subsequent cost multiplies the previous one and rounds down, preserving the original order. Costs saturate at the signed 64-bit limit. Existing XP and rewards survive configuration changes. Armor slot totals remain subject to the armory API's validation limits.

## Command

`/levelupTool [xp]` requires permission level 4 and supported equipment in the main hand. Without an argument, it grants enough XP for the next upgrade; otherwise it grants the supplied positive amount. Works with tools and assembled modular armor, including a creative administrator. Broken equipment and level limits still apply.

## Building

Build both dependency projects first using Java 21. Put the checkouts alongside this one, or provide absolute JAR paths:

```powershell
.\gradlew.bat build -PtinkersNirvanaJar=C:/path/to/TinkersNirvana.jar -PtinkersArmoryNirvanaJar=C:/path/to/TinkersArmoryNirvana.jar
```

Default paths are in `build.gradle`. Both JARs are required to compile the optional integration. `-PwithoutArmory` omits armory from development runtime while retaining compile-time API access. `runClient` and `runServer` use the same properties. Output is in `build/libs/`; dependencies, game files and local paths are not committed.

## Credits and license

Original progression design, translations and chime: **boni / Tinkers Tool Leveling**, MIT. See [LICENSE](LICENSE), [NOTICE.md](NOTICE.md) and [TEMPLATE_LICENSE.txt](TEMPLATE_LICENSE.txt).
