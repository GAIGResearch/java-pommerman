import regex
import numpy as np
import os
import pickle


def heat_maps(event_logs):

    bomb_heatmap = np.zeros((11, 11))
    death_heatmap = np.zeros((11, 11))

    for events_filename in event_logs:
        try:
            with open(events_filename) as file:
                events = file.readlines()

            events = [x.strip() for x in events]  # Remove '\n' characters.
            print(events)
            for event in events:
                # Extract information using regular expressions:
                tick = int(regex.search('(.*) \|', event).group(1))
                agent_id = int(regex.search('\| \[([0-9])\]', event).group(1))
                x_coord = int(regex.search('\(([0-9]*)\, ([0-9]*)\)', event).group(1))
                y_coord = int(regex.search('\(([0-9]*)\, ([0-9]*)\)', event).group(2))
                if 'placed a bomb' in event:  # Checking for a particular event.
                    bomb_heatmap[x_coord, y_coord] = bomb_heatmap[x_coord, y_coord] + 1
                if 'died' in event:
                    death_heatmap[x_coord, y_coord] = death_heatmap[x_coord, y_coord] + 1
        except PermissionError:
            print(f"file {events_filename} could not be opened")
    return bomb_heatmap, death_heatmap


def analyze(events_folder_path):
    '''
    Analyzes event log data and build heat maps that is saves in a new ./heatmap folder within the given path
    :param events_folder_path: the path to the folder of event logs that you want to analyze
    :return: nothing.
    '''
    try:
        os.makedirs(events_folder_path)
        print(f"{events_folder_path} created")
    except FileExistsError:
        print(f"{events_folder_path} already existed")
    os.chdir(events_folder_path)

    event_logs = os.listdir(".")
    print(f"Event log files: {event_logs}")

    bomb_heatmap, death_heatmap = heat_maps(event_logs)

    try:
        os.makedirs("heatmaps")
        print(f"heatmaps folder created")
    except FileExistsError:
        print("heatmaps folder already existed")

    heatmap_out_filename = 'heatmaps/bomb_heatmap.ndarr'
    print(bomb_heatmap)
    fo = open(heatmap_out_filename, 'w')
    fo.write(str(bomb_heatmap))
    fo.close()

    heatmap_out_filename = 'heatmaps/bomb_heatmap.pkl'
    fo = open(heatmap_out_filename, 'wb')
    pickle.dump(bomb_heatmap, fo)
    fo.close()

    heatmap_out_filename = 'heatmaps/death_heatmap.ndarr'
    print(death_heatmap)
    fo = open(heatmap_out_filename, 'w')
    fo.write(str(death_heatmap))
    fo.close()

    heatmap_out_filename = 'heatmaps/death_heatmap.pkl'
    fo = open(heatmap_out_filename, 'wb')
    pickle.dump(death_heatmap, fo)
    fo.close()


def main():
    events_folder_path = '../out/gamelogs/events'
    analyze(events_folder_path)


if __name__ == "__main__":
    main()
