import matplotlib.pyplot as plt
import numpy as np
import seaborn as sns
import pickle


def load_and_display(pickle_path):
    '''
    Takes the path to a pickled numpy array and displays it as a heat map
    :param pickle_path: path to pickled numpy array
    :return:  nothing
    '''
    fo = open(pickle_path, 'rb')
    a = pickle.load(fo)

    display(a)


def convert_to_image(pickle_path, image_name):
    '''
    Takes the path to a pickled numpy array and displays it as a heat map
    :param pickle_path: path to pickled numpy array
    :return:  nothing
    '''
    fo = open(pickle_path, 'rb')
    a = pickle.load(fo)

    save_image(a, image_name)


def display(a):
    '''
    Displays a numpy array as a heat map
    :param a: the numpy array to be displayed
    :return:
    '''
    mask = np.zeros_like(a)
    mask[np.triu_indices_from(mask)] = True
    with sns.axes_style("white"):
        ax = sns.heatmap(a, square=True,  cmap="YlGnBu")
        plt.show()


def save_image(a, image_path):
    '''
    Save a numpy array as a heat map image
    :param a: the numpy array to be saved
    :return:
    '''
    image_path = f'heatmaps/{image_path}'
    mask = np.zeros_like(a)
    mask[np.triu_indices_from(mask)] = True
    with sns.axes_style("white"):
        ax = sns.heatmap(a, square=True,  cmap="YlGnBu")
        plt.savefig(f'{image_path}.png', bbox_inches='tight')
        print(f"Image saved to {image_path}.png")
        plt.close()


def main():
    heatmap_out_filename = '../out/gamelogs/events/heatmaps/bomb_heatmap.pkl'
    convert_to_image(heatmap_out_filename, "../out/gamelogs/events/heatmaps/bomb_heatmap")

    heatmap_out_filename = '../out/gamelogs/events/heatmaps/death_heatmap.pkl'
    convert_to_image(heatmap_out_filename, "../out/gamelogs/events/heatmaps/death_heatmap")


if __name__ == "__main__":
    main()
