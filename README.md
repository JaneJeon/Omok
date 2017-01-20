Omok
===
#### The complete package of Omok & Gomoku board, client + server, and AI!

To compile the board/client, simply clone this repo, make sure maven is installed on your computer, and type `mvn package`.

To compile the server, change `mainClass` in pom.xml from `오목` to `Server` before packaging.

This will create two runnable jars inside the `target` folder. However, only the `jar-with-dependencies` will include the full dependency and run.

Descriptions of what each class does and how they work (especially the step, hash, and calculateScores functions in Jack) will come as soon as I fix Jack's Alpha-Beta pruning behavior.

Check the `TODO`'s on each file to get a glimpse of future improvements & optimizations

---
The previous, deprecated version of this Omok project without maven project structure can be found here: https://github.com/sungilahn/Legacy-Omok
