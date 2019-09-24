AIIDE 2019 Artifact
-------------------

* Final paper: https://github.com/GAIGResearch/java-pommerman/raw/master/AIIDE-19_paper-46.pdf

* Download the artifact: clone this repository (master branch) or download as .zip file here: https://github.com/GAIGResearch/java-pommerman/archive/master.zip

* Instructions to run the artifact:
 0) You need Java 8.0.1 version (or higher) to run this code.
 1) Option 1: download the source, compile and execute the class Run.java (see below for execution modes). 
 2) Option 2: directly run run.jar, included in jars/ 


Executing Run.java / run.jar
----------------------------

This runs either a single game of pommerman (visuals on) or a series of games (headless), reporting statistics at the end. The usage is 'java Run' or 'java -jar run.jar' with 8 parameters:

* \[arg index = 0\] Game Mode. 0: FFA; 1: TEAM <br>
* \[arg index = 1\] Number of level generation seeds \[S\]. "-1" to execute with the ones from the paper (20). <br>
* \[arg index = 2\] Repetitions per seed \[N\]. "1" for one game only with visuals. <br>
* \[arg index = 3\] Vision Range \[VR\]. (0, 1, 2 for PO; -1 for Full Observability)<br>
* \[arg index = 4-7\] Agents. When in TEAM, agents are mates as indices 4-6, 5-7: <br>
	* 0 DoNothing <br>
	* 1 Random <br>
	* 2 OSLA <br>
	* 3 SimplePlayer <br>
	* 4 RHEA 200 itereations, shift buffer On, pop size 1, random init, length: 12 <br>
	* 5 MCTS 200 iterations, length: 12 <br>
	* 6 Human Player (controls: cursor keys + space bar)  <br>


Examples: 
 * A single game with full observability, FFA. This is also the default mode when no arguments are passed:
 	* *java -jar run.jar 0 1 1 -1 2 3 4 5*
 * A single game with partial observability, FFA, where you're in control of one player:
 	* *java -jar run.jar 0 1 1 2 0 1 2 6*
 * Executes several games, headless, FFA. Two different random seeds for the level generation, repeated 5 times each (for a total of 5x2 games). 
 	* *java -jar run.jar 0 2 5 4 2 3 4 1* 
 * Executes several games, headless, TEAM, repeated 10 times each. Same configuration as the one used in the paper, including the 20 seeds.
 	* *java -jar run.jar 1 -1 10 4 5 3 5 3* 


Notes:
 * If you provide N=1, the program will run a single game, with graphics on, using the agents specified in parameters 4-7.
 * If you provide S=-1, the program will run N games with the *specific _20_ seeds* used in the AIIDE 2019 paper (graphics off, results reported at the end).
 * If you provide any other S>1, the program will run N games with *_S_ random seeds* (total games, NxS), graphics off, using the agents specified in parameters 4-7 and results being reported at the end.
 * The Human Player (option 6) is only available when N=1.

You can modify the code to execute different games as well (i.e. different agents or their parameters). For extra Java-pommerman wiki/documentation, visit this: https://github.com/GAIGResearch/java-pommerman/wiki

All games you play are logged in res/gamelogs/

Extra
-----

All raw data and plots for the results reported in the paper is in the data/ folder (contains *more* plots than what could be included in the paper). You can either:

1) Download the Zip file here https://github.com/GAIGResearch/java-pommerman/blob/master/data/Analysis%20of%20Statistical%20Forward%20Planning%20methods%20in%20Pommerman.zip?raw=true
2) Explore the raw data and all generated plots on the repository yourself: https://github.com/GAIGResearch/java-pommerman/tree/master/data/Analysis%20of%20Statistical%20Forward%20Planning%20methods%20in%20Pommerman


