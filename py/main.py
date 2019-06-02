from os import listdir
import pandas as pd
import regex

data = pd.DataFrame(columns=["game_mode", "observability", "agents", "game_seed", "instance", "event_id", "event_data"])
# Event id: [bomb, death, pickup]
# Event data bomb: (tick, relative_tick, agent_id, x, y)
# Event data death: (tick, relative_tick, agent_id, x, y, killer, stuck)
# Event data pickup: (tick, relative_tick, agent_id, x, y, pickup)

path = "gamelogs/"
for configuration in listdir(path):
    config_extract = configuration.split("-")
    # extract configurations
    game_mode = int(config_extract[0])  # GAME MODE
    reps = int(config_extract[1])  # GAME REPETITIONS
    observability = -1
    idx = 4
    if config_extract[2] != "":
        observability = int(config_extract[2])  # GAME OBSERVABILITY
        idx = 3
    agents = [int(config_extract[idx]), int(config_extract[idx + 1]), int(config_extract[idx + 2]),
              int(config_extract[idx + 3])]  # GAME AGENTS

    for game_events in listdir(path + configuration):
        if game_events.endswith("_events.txt"):
            game_events_extract = game_events.split("_")

            # extract events information
            game_seed = int(game_events_extract[0])  # GAME SEED
            game_instance = int(game_events_extract[1])  # GAME INSTANCE

            with open(path + configuration + "/" + game_events) as f:
                # process this events file for 1 game
                events = f.readlines()
                last_tick = int(regex.search('(.*) \|', events[-1]).group(1))

                bomb_map = []
                agent_deaths = []
                pick_ups = []

                for ev in events:
                    tick = int(regex.search('(.*) \|', ev).group(1))  # GAME TICK
                    relative_tick = tick/last_tick
                    agent_id = int(regex.search('\| \[([0-9])\]', ev).group(1))
                    loc_x = int(regex.search('\(([0-9]*)\, ([0-9]*)\)', ev).group(1))
                    loc_y = int(regex.search('\(([0-9]*)\, ([0-9]*)\)', ev).group(2))

                    if "placed a bomb at" in ev:
                        bomb_map.append({"tick": tick, "relative_tick:": relative_tick, "agent_id": agent_id,
                                         "x": loc_x, "y": loc_y})
                    # if "failed to place a bomb at" in ev:
                    # if "'s bomb exploded at" in ev:
                    if "died at" in ev:
                        killer_id = int(regex.search('\[(.*)\]', ev).group(1)[-1])
                        stuck = False
                        if "(was stuck)" in ev:
                            stuck = True
                        agent_deaths.append({"tick": tick, "relative_tick:": relative_tick, "agent_id": agent_id,
                                             "x": loc_x, "y": loc_y, "killer": killer_id, "stuck": stuck})
                    if "picked up" in ev:
                        pick_up = " ".join(regex.findall('([A-Z]+)', ev))
                        pick_ups.append({"tick": tick, "relative_tick:": relative_tick, "agent_id": agent_id,
                                         "x": loc_x, "y": loc_y, "pickup": pick_up})

                data.loc[data.size] = {"game_mode": game_mode, "observability": observability, "agents": agents,
                             "game_seed": game_seed, "instance": game_instance, "event_id": "bomb",
                             "event_data": bomb_map}
                data.loc[data.size] = {"game_mode": game_mode, "observability": observability, "agents": agents,
                             "game_seed": game_seed, "instance": game_instance, "event_id": "death",
                             "event_data": agent_deaths}
                data.loc[data.size] = {"game_mode": game_mode, "observability": observability, "agents": agents,
                             "game_seed": game_seed, "instance": game_instance, "event_id": "pickup",
                             "event_data": pick_ups}

data.to_pickle("data.pkl")
