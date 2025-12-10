# Cobblehome

A simple client-side home system for Cobblemon on NeoForge 1.21.1.

## Prerequisites

- **Java Development Kit (JDK) 21** or higher
- **Git** for cloning the repository

## Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/cobblehome.git
cd cobblehome
```

### 2. Build the Mod

On Linux/macOS:
```bash
./gradlew build
```

On Windows:
```bash
gradlew.bat build
```

The compiled mod JAR will be located in `build/libs/cobblehome-1.0-SNAPSHOT.jar`

### 3. Testing in Development

To run Minecraft with your mod in a development environment:

```bash
./gradlew runClient
```

This will launch Minecraft with your mod loaded for testing.

## Installation

1. Build the mod using the steps above
2. Copy `build/libs/cobblehome-1.0-SNAPSHOT.jar` to your Minecraft `mods` folder
3. Make sure you have **Cobblemon** and **Kotlin for Forge** installed
4. Launch Minecraft with NeoForge 1.21.1

## Dependencies

This mod requires:
- **NeoForge**: 21.1.182+
- **Cobblemon**: 1.7.1+
- **Kotlin for Forge**: 5.10.0+ (automatically included with Cobblemon)

## Development

### Project Structure

```
cobblehome/
├── src/main/java/          # Java source files
├── src/main/resources/     # Resources (configs, assets, etc.)
│   └── META-INF/
│       └── neoforge.mods.toml  # Mod metadata
├── build.gradle.kts        # Build configuration
└── gradle.properties       # Gradle properties
```

### Useful Gradle Commands

- `./gradlew build` - Build the mod
- `./gradlew runClient` - Run Minecraft client with the mod
- `./gradlew runServer` - Run Minecraft server with the mod
- `./gradlew clean` - Clean build artifacts
- `./gradlew tasks` - List all available tasks

## License

MIT License

## Author

Heitor Maciel
