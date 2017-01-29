The ultimate Omok & Gomoku Engine designed for *humans*
===

Designed to play against humans, the response time is extremely snappy *and* the moves are smart!

The AI is now pretty much unbeatable by humans, and can search a node in ~0.01ms, which means it can search depth 11 in 100~400ms when warmed up! (the default depth is 9 - JVM is wicked fast!)

If constantly losing is not your thing, you can play local multiplayer or over the internet with another person.

---

To compile the board/client, simply clone this repo, make sure maven is installed on your computer, and type `mvn package`.

This will create three runnable jars inside the `target` folder. `Client` and `Server` will include the full dependency and run.

Descriptions of what each class does and how they work (especially the step, hash, and calculateScores functions in Jack) will come as soon as I fix Jack's Alpha-Beta pruning behavior.

Check the `TODO`'s on each file to get a glimpse of future improvements & optimizations

---
The previous, deprecated version of this Omok project without maven project structure can be found here: https://github.com/sungilahn/Legacy-Omok
