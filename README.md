AIIDE 2019 Artifact
-------------------

* Final paper: https://github.com/GAIGResearch/java-pommerman/raw/master/AIIDE-19_paper-46.pdf

* Download the artifact: clone this repository (master branch) or download as .zip file here: https://github.com/GAIGResearch/java-pommerman/archive/master.zip

* Instructions to run the artifact:
 0) You need Java 9.0.1 version (or higher) to run this code.
 1) Option 1: download the source, compile and execute the class Run.java (see below for execution modes). 
 2) Option 2: directly run run.jar, included in jars/


Executing Run.java / run.jar
----------------------------

This runs either a single game of pommerman (visuals on) or a series of games (headless), reporting statistics at the end. The usage is 'java Run' or 'java -jar run.jar' with 7 parameters:

* \[arg index = 0\] Game Mode. 0: FFA; 1: TEAM <br>
* \[arg index = 1\] Repetitions per seed \[N\]. "1" for one game only with visuals. <br>
* \[arg index = 2\] Vision Range \[R\] <br>
* \[arg index = 3-6\] Agents: <br>
	* 0 DoNothing <br>
	* 1 Random <br>
	* 2 OSLA <br>
	* 3 SimplePlayer <br>
	* 4 RHEA 200 itereations, shift buffer On, pop size 1, random init, length: 12 <br>
	* 5 MCTS 200 iterations, length: 12 <br>
	* 6 Human Player (controls: cursor keys + space bar)  <br>

Notes:
 * If you provide N=1 for the second argument, the program will run a single game, with graphics on, using the agents specified in parameters 3-6.
 * If you provide N=20 for the second argument, the program will run 20 games with the *specific seeds* used in the AIIDE 2019 paper (graphics off, results reported at the end).
 * If you provide any other N>1, the program will run N games with *random seeds*, with graphics off, using the agents specified in parameters 3-6 and results being reported at the end.
 * The Human Player (option 6) is only available when N=1.

You can modify the code to execute different games as well (i.e. different agents or their parameters). For extra Java-pommerman wiki/documentation, visit this: https://github.com/GAIGResearch/java-pommerman/wiki


Extra
-----

All raw data and plots for the results reported in the paper is in the data/ folder (contains *more* plots than what could be included in the paper). You can either:

1) Download the Zip file here https://github.com/GAIGResearch/java-pommerman/blob/master/data/Analysis%20of%20Statistical%20Forward%20Planning%20methods%20in%20Pommerman.zip?raw=true
2) Explore the raw data and all generated plots on the repository yourself: https://github.com/GAIGResearch/java-pommerman/tree/master/data/Analysis%20of%20Statistical%20Forward%20Planning%20methods%20in%20Pommerman


