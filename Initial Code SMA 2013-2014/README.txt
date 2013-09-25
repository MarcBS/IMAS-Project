
To execute the practical exercise with the Central Agent and the Coordinator Agent use PRAC.BAT. To start JADE with its gui, in order to manually introduce the agents, use PRAC2.BAT

These agents initialize the information of the simulation and the Central Agent send this information to the Coordinator Agent. This is the first step of the process.

(each team will have to change the folders according to their configuration, or will have to define an execution profile if they use a tool as Eclipse)

The Central Agent reads the configuration of the game. This information is obatined from "game.txt". The class sma.InfoGame reads this kind of files. We also have methods to write the information of the city, in order to compare the initial state and the final state.



