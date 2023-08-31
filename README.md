<div align="center">
    <h1>Selective Glowing (Fabric)</h1>
    <h4>Comically simple server mod to apply fake glowing to players.</h4>
    <img src="assets/selective-glowing-icon-rounded-x256.png" height="128" alt="Selective Glowing logo">
</div>

### What problem does Selective Glowing solve?
Server owners, who make their own games with command blocks and datapacks on fabric servers, may want to make a mini-game that shows some player glowing for some other specific player.
You can't make that in vanilla Minecraft. You need a mod or a plugin for your server to modify the packets sent to players. That's what this project is about.

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
