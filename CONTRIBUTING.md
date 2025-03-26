# How to Contribute

## General workflow

0. (External contributors only) Create a fork of the repository
1. Pull any changes from `main` to make sure you're up-to-date
2. Create a branch from `main`
    * Give your branch a name that describes your change (e.g. yourusername/add-scoreboard)
    * Focus on one change per branch
3. Commit your changes
    * Keep your commits small and focused
    * Write descriptive commit messages in [Conventional Commit](https://www.conventionalcommits.org/en/v1.0.0/) format
    * Wrap your commit body to 72 characters, and keep your commit title 50 characters long
4. When you're ready, create a pull request to `main`
   * Keep your PRs small (preferably <300 LOC)
   * Format your title in [Conventional Commit](https://www.conventionalcommits.org/en/v1.0.0/) format
   * List any changes made in your description
   * Link any issues that your pull request is related to as well

### Example:
```text
feat: create scoreboard for total points

Adding a scoreboard is useful for users to find out how many points they have without running any command.

Summary of changes

- Add scoreboard display in-game at game end  
- Change `StorageManager` class to persist scoreboard data
```

After the pull request has been reviewed, approved, and passes all automated checks, it will be merged into main.
