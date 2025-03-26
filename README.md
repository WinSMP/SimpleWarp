# SimpleWarp

SimpleWarp is a Paper plugin that allows players to create, remove, edit, and teleport to custom warps.

## Features

- Create new warps with a name and optional coordinates
- Remove existing warps
- Edit the coordinates of existing warps
- Teleport to a warp by name
- Tab completion for warp names

## Building and Installation

SimpleWarp is built using Gradle. To build the plugin, follow these steps:

1. Clone the repository: `git clone https://github.com/walker84837/SimpleWarp.git`
2. Navigate to the project directory: `cd SimpleWarp`
3. Build the plugin: `./gradlew build`

The built plugin jar file will be located in the `build/libs` directory.

## Usage

The plugin adds a `/warp` command with the following subcommands:

- `/warp new [name] [x] [y] [z]`: Creates a new warp with the specified name and optional coordinates.
- `/warp remove [name]`: Removes the warp with the specified name.
- `/warp edit [name] [x] [y] [z]`: Updates the coordinates of the warp with the specified name.
- `/warp teleport [name]` or `/warp tp [name]`: Teleports the player to the warp with the specified name.

The plugin also provides tab completion for warp names.

## Permissions

The plugin uses the following permissions:

- `warp.admin`: Allows players to use the `new`, `remove`, and `edit` subcommands.

## Database

SimpleWarp uses a SQLite database to store warp information. The database file is located in the plugin's data folder (`plugins/SimpleWarp/warps.db`).

## Contributing

If you encounter any issues or have suggestions for improvements, please feel free to open an issue or submit a pull request on the [GitHub repository](https://github.com/walker84837/SimpleWarp).
