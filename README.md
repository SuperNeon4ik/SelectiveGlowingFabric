# Selective Glowing (Fabric)
Comically simple server mod to apply fake glowing to players. 

### Usage
There's only one command, that controls everything.
```
/glow <targets: entities> <displayplayers: players>
```
`targets` - entities, that are gonna be glowing.\
`displayplayers` - players, that will see the `targets` glowing. (command *overrides* old `displayplayers`)

Reset glowing for targets with this command.
```
/glow <targets: entities> *reset
```
Or reset all glowing overrides with this command.
```
/glow *reset
```