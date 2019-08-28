# Lotus - Omok Engine designed for humans

[![Dependabot Status](https://api.dependabot.com/badges/status?host=github&repo=JaneJeon/Omok)](https://dependabot.com)

Lotus is an advanced Omok and Gomoku engine designed to play against humans, with focus on quick response time and smart decision tree building & parsing.

The AI is very strong against humans, and can search depth 11 in 100~400 ms when warmed up!

Lotus is also a fully-featured Omok/Gomoku board with intuitive controls, clean UI, responsive play area, and customization options:

* Select play mode (Local 2P | Online multiplayer | Player vs AI)
* Determine which moves you want to see, and whether you want them numbered (along with its font!)
* Difficulty settings for AI modes

Instructions:
---

You need the latest version of the Java 8 runtime to run the board.

If you want to compile the board/client yourself, simply clone this repo, make sure maven is installed on your computer, and type `mvn package`.

This will create two runnable jars inside the `target` folder. Both `Client` and `Networking.Server` will include the full dependency and run, with `Client` being the actual board.
 
 Note that you will need to install maven to compile, and if you're not using an IDE, you may need to manually install and link Lotus's dependencies.

To run the server, upload `Networking.Server` to a public server with static IP.

In addition, to set up the server, create a "serverConfig.txt" on /main/resources folder with your public IP in which your server.jar is running on.

---
The previous, deprecated version of this Omok project without maven project structure can be found here: https://github.com/sungilahn/Legacy-Omok
