The Omok/Gomoku Engine designed for humans
===

Designed to play against humans, with focus on quick response time and smart decision tree building & parsing.

The AI is very strong against humans, and can search depth 11 in 100~400ms when warmed up!

You can also play local multiplayer or over the internet with another person.

---

To compile the board/client, simply clone this repo, make sure maven is installed on your computer, and type `mvn package`.

This will create three runnable jars inside the `target` folder. `Client` and `Server` will include the full dependency and run.

In addition, to set up the server, create a "serverConfig.txt" on /main/resources folder with your public IP in which your server.jar is running on.

Descriptions of what each class does and how they work (especially the step, hash, and calculateScores functions in Jack) will come as soon as I fix Jack's Alpha-Beta pruning behavior.

Check the `TODO`'s on each file to get a glimpse of future improvements & optimizations

---
The previous, deprecated version of this Omok project without maven project structure can be found here: https://github.com/sungilahn/Legacy-Omok
