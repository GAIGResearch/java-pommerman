import pandas as pd
import numpy as np
from py import heatmapvisuals as vis


agent_mapping = {2: "OSLA", 3: "RuleBased", 4: "RHEA", 6: "MCTS"}
agents = [4, 6]  # Put in here the id's of the agents you want to make heatmaps for
mode_mapping = {0: "FFA", 1: "TEAM"}
game_seeds = [93988, 19067, 64416, 83884, 55636, 27599, 44350, 87872, 40815, 11772, 58367, 17546, 75375, 75772, 58237,
              30464, 27180, 23643, 67054, 19508]
pick_ups = ["CAN KICK", "BLAST STRENGTH", "AMMO"]
observability_mapping = {-1: "OBSERVABLE", 1: "PO1", 2: "PO2", 4: "PO4"}


def heatmap_from_selection(selected):
    '''
    Takes in a pandas data frame, and makes a heat map from all events in it
    :param selected: data frame
    :return: heat map, as an ndarray
    '''
    heatmap = np.zeros((11, 11))
    # Iterate through selected game data
    for index, row in selected.iterrows():
        for event in row["event_data"]:
            x = event['x']
            y = event['y']
            heatmap[x, y] = heatmap[x, y] + 1
    return heatmap


def versus_heatmap(agent_id_1, agent_id_2, data, event_id, observability):
    '''
    Get a heatmap of games including exactly these two agent id's playing against each other
    :param agent_id_1:
    :param agent_id_2:
    :param data: the unpickled pandas data frame
    :param event_id: the kind of event we want to heat map
    :param observability: 1,2,4,-1
    :return: nothing
    '''
    TEAM = 1
    data_frame = data.loc[(data['game_mode'] == TEAM) &
                        (data['event_id'] == event_id) &
                        (data['observability'] == observability)]

    for index, row in data_frame.iterrows():
        if agent_id_1 not in row["agents"] or agent_id_2 not in row["agents"]:
            data_frame.drop(index, inplace=True)

    heatmap = heatmap_from_selection(data_frame)
    map_name = f"{agent_mapping[agent_id_1]}vs{agent_mapping[agent_id_2]}_{mode_mapping[TEAM]}_{event_id}_{observability_mapping[observability]}_heatmap"
    vis.save_image(heatmap, map_name)


def main():
    data = pd.read_pickle("data.pkl")
    # print(str(data))
    # columns=["game_mode", "observability", "agents", "game_seed", "instance", "event_id", "event_data"]
    # Event id: [bomb, death, pickup]
    # Event data bomb: (tick, relative_tick, agent_id, x, y)
    # Event data death: (tick, relative_tick, agent_id, x, y, killer, stuck)
    # Event data pickup: (tick, relative_tick, agent_id, x, y, pickup)


    # Get the heat maps for all kinds of events, for all kinds of observability, for all kinds of game modes
    for game_mode in mode_mapping:
        for event_id in ["bomb", "death", "pickup"]:
            for observability in observability_mapping:
                selected = data.loc[(data['game_mode'] == game_mode) & (data['event_id'] == event_id) & (data['observability'] == observability)]
                heatmap = heatmap_from_selection(selected)
                print(str(heatmap))
                map_name = f"{mode_mapping[game_mode]}_{event_id}_{observability_mapping[observability]}_heatmap"
                vis.save_image(heatmap, map_name)

    # Get individual heat maps for different agent types, given in the array 'agents'. Only for TEAM games
    for game_mode in mode_mapping:
        for event_id in ["bomb", "death", "pickup"]:
            for observability in observability_mapping:
                selected = data.loc[(data['game_mode'] == game_mode) &
                                    (data['event_id'] == event_id) &
                                    (data['observability'] == observability)]

                heatmap_list = [np.zeros((11, 11)) for _ in range(len(agents))]
                for index, row in selected.iterrows():
                    for event in row["event_data"]:
                        x = event['x']
                        y = event['y']
                        for agent_id in agents:
                            if row["agents"][event["agent_id"]] == agent_id:
                                heatmap_list[agents.index(agent_id)][x, y] += 1

                for i, heatmap in enumerate(heatmap_list):
                    map_name = f"{agent_mapping[agents[i]]}_{mode_mapping[game_mode]}_{event_id}_{observability_mapping[observability]}_heatmap"
                    vis.save_image(heatmap, map_name)


if __name__ == "__main__":
    main()

