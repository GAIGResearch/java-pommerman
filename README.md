AIIDE 2019 Artifact:

Final paper:
Download the source: clone this repository (master branch) or download as .zip file here: 
Instructions to run the artifact:
 0) You need Java 9.0.1 version (or higher) to run this code.
 1) Option 1: download the source, compile and execute the class Run.java (see below for execution modes). 
 2) Option 2: directly run run.jar, included in jars/

Execution modes:

Run.java / run.jar
------------------

This runs either a single game of pommerman (visuals on) or a series of games (headless), reporting statistics at the end. The usage is:

Usage: java Run \[args\]
	 \[arg index = 0\] Game Mode. 0: FFA; 1: TEAM
	 \[arg index = 1\] Repetitions per seed \[N\]. "1" for one game only with visuals.
	 \[arg index = 2\] Vision Range \[R\]
	 \[arg index = 3-6\] Agents:
		 0 DoNothing
		 1 Random
		 2 OSLA
		 3 SimplePlayer
		 4 RHEA 200 itereations, shift buffer On, pop size 1, random init, length: 12
		 5 MCTS 200 iterations, length: 12





Java-pommerman wiki/documentation: https://github.com/GAIGResearch/java-pommerman/wiki
