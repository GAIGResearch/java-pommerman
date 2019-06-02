import math
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
import scipy.stats

data = pd.read_pickle("data.pkl")
# print(str(data))
# columns=["game_mode", "observability", "agents", "game_seed", "instance", "event_id", "event_data"]
# Event id: [bomb, death, pickup]
# Event data bomb: (tick, relative_tick, agent_id, x, y)
# Event data death: (tick, relative_tick, agent_id, x, y, killer, stuck)
# Event data pickup: (tick, relative_tick, agent_id, x, y, pickup)

agent_mapping = {2: "OSLA", 3: "RuleBased", 4: "RHEA", 6: "MCTS"}
agents = [2, 3, 4, 6]
mode_mapping = {0: "FFA", 1: "TEAM"}
game_seeds = [93988, 19067, 64416, 83884, 55636, 27599, 44350, 87872, 40815, 11772, 58367, 17546, 75375, 75772, 58237,
              30464, 27180, 23643, 67054, 19508]
pick_ups = ["CAN KICK", "BLAST STRENGTH", "AMMO"]
obs_options = [1, 2, 4, -1]
# colors = {2: "b", 3: "orange", 4: "g", 6: "r"}
colors = ["b", "orange", "g", "r"]


def suicide_query(game_mode=0, observability=-1, game_seed=-1, agent=-1):
    """
    Calculates the number of suicides for a type of agent given game mode, observability, and game seed.
    If game seed passed is -1, then all game seeds are aggregated.
    """

    event_id = "death"

    # Keep only those games within given configuration
    if game_seed != -1:
        selection = data.loc[(data['game_mode'] == game_mode) & (data['observability'] == observability) &
                             (data['game_seed'] == game_seed)]
    else:
        selection = data.loc[(data['game_mode'] == game_mode) & (data['observability'] == observability)]
        if agent != -1:
            for index, row in selection.iterrows():
                if agent not in row["agents"]:
                    selection.drop(index, inplace=True)

    # print(selection.size)

    team_kill_count = []
    ngames = 0  # Number of games in which this agent dies
    suicides = 0  # Number of games in which this agent commits suicide
    events_per_sample = []
    team_kills = 0

    # Iterate through selected game data
    for index, row in selection.iterrows():
        if agent in row["agents"] and row['event_id'] == event_id:  # This agent played in the game

            # Find its agent ID depending on its position in the agent list. There may be more than 1 agent of this
            # type in the game, so iterate over all and check individually.
            ll = row["agents"]
            indices = [i for i, el in enumerate(ll) if el == agent]

            for agent_id in indices:
                # teammate = (agent_id + 2) % 4
                sample_event_counter = 0
                for event in row["event_data"]:
                    if event["agent_id"] == agent_id:  # This agent dies
                        if event["killer"] == agent_id:  # Suicide
                            sample_event_counter += 1
                        # if event["killer"] == teammate:  # Killed by teammate
                        #     team_kills += 1
                    # if event["agent_id"] == teammate:  # Teammate dies
                    #    if event["killer"] == agent_id:  # Killed by this agent
                    #        team_kill_count += 1
                ngames += 1
                events_per_sample.append(sample_event_counter)
                suicides += sample_event_counter

    # suicide_count.append(100*suicides/ngames)  # Showing percentage of game suicides
    # team_kill_count.append(100*team_kills/games)

    # percentage = 100 * suicides / ngames
    # mean = ngames * (percentage / 100)
    # variance = mean * (1 - (percentage / 100))
    # std_dev = math.sqrt(variance)
    # std_err = std_dev / math.sqrt(ngames)
    # h = std_err * scipy.stats.t.ppf(1.95 / 2., ngames - 1)  # 95 confidence interval
    # return percentage, h

    # print(events_per_sample)
    mean = suicides/ngames
    variance = sum([pow(x - mean, 2) for x in events_per_sample])/len(events_per_sample)
    std_dev = math.sqrt(variance)
    std_err = std_dev/math.sqrt(len(events_per_sample))
    h = std_err * scipy.stats.t.ppf(1.95 / 2., ngames - 1)  # 95% confidence interval
    return mean * 100, h * 100  # , team_kill_count


def event_count_query(event_id, game_mode=0, observability=-1, game_seed=-1, agent=-1):

    # Keep only those games within given configuration
    if game_seed != -1:
        selection = data.loc[(data['game_mode'] == game_mode) & (data['observability'] == observability) &
                             (data['game_seed'] == game_seed)]
    else:
        selection = data.loc[(data['game_mode'] == game_mode) & (data['observability'] == observability)]
        if agent != -1:
            for index, row in selection.iterrows():
                if agent not in row["agents"]:
                    selection.drop(index, inplace=True)

    ngames = 0  # Number of games in which this agent plays bombs
    event_counter = 0  # Number of bombs this agent places
    events_per_sample = []
    # Iterate through selected game data
    for index, row in selection.iterrows():
        if agent in row["agents"] and row['event_id'] == event_id:  # This agent played in the game
            ll = row["agents"]
            indices = [i for i, el in enumerate(ll) if el == agent]

            for agent_id in indices:
                sample_event_counter = 0
                for event in row["event_data"]:
                    if event["agent_id"] == agent_id:  # This agent places bomb
                        sample_event_counter += 1
                ngames += 1
                events_per_sample.append(sample_event_counter)
                event_counter += sample_event_counter
    mean = event_counter/ngames
    variance = sum([pow(x - mean, 2) for x in events_per_sample])/len(events_per_sample)
    std_dev = math.sqrt(variance)
    std_err = std_dev/math.sqrt(len(events_per_sample))
    h = std_err * scipy.stats.t.ppf(1.95 / 2., ngames - 1)  # 95% confidence interval

    return event_counter/ngames, h


def plot_suicides(mode=0):
    plot_data = [[] for _ in range(len(agent_mapping))]
    stderr_data = [[] for _ in range(len(agent_mapping))]
    for agent in agents:
        for o in obs_options:
            suicide_rate, stderr = suicide_query(game_mode=mode, observability=o, agent=agent)
            print("Suicides in game mode " + mode_mapping[mode] + ", observability " + str(o))
            plot_data[agents.index(agent)].append(suicide_rate)
            stderr_data[agents.index(agent)].append(stderr)
            print(agent_mapping[agent] + ": " + str(suicide_rate))

    x = [1, 2, 4, 11]
    xt = ['PO:1', 'PO:2', 'PO:4', '$\infty$']
    for d in range(len(plot_data)):
        plt.plot(x, plot_data[d], label=agent_mapping[agents[d]], color=colors[d])
        y_minus_error = np.subtract(plot_data[d], stderr_data[d])
        y_plus_error = np.add(plot_data[d], stderr_data[d])
        plt.fill_between(x, y_minus_error, y_plus_error, alpha=0.2, edgecolor=None, facecolor=colors[d], linewidth=0, antialiased=True)

    plt.xticks(x, xt)
    plt.legend()
    plt.xlabel("Vision range", fontsize=16)
    plt.ylabel("suicide %", fontsize=16)
    plt.yticks(np.arange(0.0, 101.0, 10.0))
    plt.grid(color='lightgrey', linestyle='--', linewidth=1)
    plt.savefig(f'suicide_{mode}.png')
    plt.show()


def plot_event_count(event_name, mode=0):
    plot_data = [[] for _ in range(len(agent_mapping))]
    std_err_data = [[] for _ in range(len(agent_mapping))]
    for agent in agents:
        for o in obs_options:
            events_per_game, std_err = event_count_query(event_name, game_mode=mode, observability=o, agent=agent)
            print("Bombs in game mode " + mode_mapping[mode] + ", observability " + str(o))
            plot_data[agents.index(agent)].append(events_per_game)
            std_err_data[agents.index(agent)].append(std_err)
            print(agent_mapping[agent] + ": " + str(events_per_game) + " std.err: " + str(std_err))

    x = [1, 2, 4, 11]
    xt = ['PO:1', 'PO:2', 'PO:4', '$\infty$']
    for d in range(len(plot_data)):
        plt.plot(x, plot_data[d], label=agent_mapping[agents[d]], color=colors[d])
        y_minus_error = np.subtract(plot_data[d], std_err_data[d])
        y_plus_error = np.add(plot_data[d], std_err_data[d])
        plt.fill_between(x, y_minus_error, y_plus_error, alpha=0.2, edgecolor=None, facecolor=colors[d], linewidth=0, antialiased=True)

    plt.xticks(x, xt)
    plt.legend()
    plt.xlabel("Vision range", fontsize=16)
    plt.ylabel(f"{event_name}s per game", fontsize=16)
    if event_name == "bomb":
        plt.yticks(np.arange(0.0, 51.0, 5.0))
    elif event_name == "pickup":
        plt.yticks(np.arange(0.0, 6.0, 1.0))
    plt.grid(color='lightgrey', linestyle='--', linewidth=1)
    plt.savefig(f'{event_name}_{mode}.png')
    plt.show()


def main():
    plot_suicides(0)  # FFA
    # plot_suicides(1)  # TEAM

    # plot_event_count("bomb", 0)
    # plot_event_count("bomb", 1)
    # plot_event_count("pickup", 0)
    # plot_event_count("pickup", 1)


if __name__ == "__main__":
    main()
