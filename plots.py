import matplotlib.pyplot as plt
import numpy as np
from matplotlib.backends.backend_pdf import PdfPages
import sys

# get new algorithm names
def name_transorm(name):

    x = name.split("_")

    m = x[0]
    alg = x[1]

    if alg == 'BCLP':
        return 'OurAlgo($\phi=' + m + '$)'
    if alg == 'HK20':
        return 'HK($\psi=' + m + '$)'

# loads a long from nanoseconds as a float in seconds
def nano_to_seconds(n):
    return int(n)*0.000000001

# function to load file at path
def load_file(path, f):

    # open the file
    file = open(path)
    strings = file.read().replace('\x00', '').split("#")
    file.close()

    values = []
    for s in strings:
        if len(s) > 0:
            values.append(f(s))

    return values

# method to turn total query time to average
def amortize(values):

    for i in range(len(values)):
        values[i] = values[i]/(i + 1)

    return values

def load_data_single(dataset, k, algo, dir='results/'):

    update_times = load_file(dir + dataset + '_' + str(k) + '_' + algo + '_updatetime', nano_to_seconds)
    query_times = load_file(dir + dataset + '_' + str(k) + '_' + algo + '_querytime', nano_to_seconds)
    query_times = amortize(query_times)
    costs = load_file(dir + dataset + '_' + str(k) + '_' + algo + '_cost', float)

    return update_times, query_times, costs

# create a plot for dataset and k
def plot_data(pages, dir='results/'):

    # stores the figures for each page
    figs = []

    for page in pages:
        figs = figs + [plot_data_page(page, dir)]

    with PdfPages('test_results.pdf') as pdf:

        for fig in figs:
            pdf.savefig(fig)  # saves the current figure into a pdf page
            plt.close()

    plt.show()

# create a plot for dataset and k
def plot_data_page(page, dir='results/'):

    fig, axs = plt.subplots(len(page), 3*len(page[0][2]), figsize=(6*3*len(page[0][2]), 5*len(page)))

    if len(page) == 1:
        plot_data_row_2(axs, page[0][0], page[0][1], page[0][2], page[0][3], dir)
    else:
        for i in range(len(page)):
            plot_data_row_2(axs[i], page[i][0], page[i][1], page[i][2], page[i][3], dir)

    fig.tight_layout()

    return fig

# create a plot for dataset and k
def plot_data_row_2(axs, dataset, k, algos, colors, dir='results/'):

    data = [[]]*len(algos)

    for i in range(len(algos)):
        for alg in algos[i]:
            data[i] = data[i] + [load_data_single(dataset, k, alg, dir)]

    n = len(data[0][0][0])
    q = len(data[0][0][1])

    x_updates = np.linspace(1, n, n)
    x_queries = np.linspace(1, n, q)

    for j in range(len(algos)):

        # update times
        for i in range(len(algos[j])):
            axs[j].plot(x_updates, data[j][i][0], label=algos[j][i], color=colors[j][i])
        axs[j].set_title('Total Update Time (' + dataset + ', k = ' + str(k) + ')')
        axs[j].set(xlabel='Updates', ylabel='Total Update Time (sec)')
        axs[j].set_yscale('log')
        axs[j].legend()

        # costs
        for i in range(len(algos[j])):
            axs[len(algos)+j].plot(x_queries, data[j][i][2], label=algos[j][i], color=colors[j][i])
        axs[len(algos)+j].set_title('Cost of Solution (' + dataset + ', k = ' + str(k) + ')')
        axs[len(algos)+j].set(xlabel='Updates', ylabel='Cost')
        axs[len(algos)+j].legend()

        # query times
        for i in range(len(algos[j])):
            axs[2*len(algos)+j].plot(x_queries, data[j][i][1], label=algos[j][i], color=colors[j][i])
        axs[2*len(algos)+j].set_title('Average Query Time (' + dataset + ', k = ' + str(k) + ')')
        axs[2*len(algos)+j].set(xlabel='Updates', ylabel='Average Query Time (sec)')
        axs[2*len(algos)+j].legend()

# construct a page for some value of k
def get_page(k, algos):

    blues = ['#20B2AA', '#00BFFF', '#0000FF']
    reds = ['#FFA500', '#EE82EE', '#FF0000']

    q = len(algos)

    page = [

     ['census', k, algos, [blues + reds]],
     ['song', k, algos, [blues + reds]],
     ['kddcup', k, algos, [blues + reds]],

     # ['census', k, algos, [['#0000FF'] + ['#FF0000']]],
     # ['song', k, algos, [['#0000FF'] + ['#FF0000']]],
     # ['kddcup', k, algos, [['#0000FF'] + ['#FF0000']]],
     # ['drift', k, algos, [['#0000FF'] + ['#FF0000']]],
     # ['sift10M', k, algos, [['#0000FF'] + ['#FF0000']]],

    ]

    return page

def get_pages(kValues, algos):

    pages = []
    for k in kValues:
        pages = pages + [get_page(k, algos)]

    return pages

# save a sing graph as a pdf
def plot_single_graph(dataset, algos, k, colors, dir1, dir2, name):

    data = []

    markers = ['']*len(algos)

    if len(algos) == 2:
        markers = ['', '.']

    for alg in algos:
        data = data + [load_data_single(dataset, k, alg, dir1)]

    n = len(data[0][0])
    q = len(data[0][1])

    x_updates = np.linspace(1, n, n)
    x_queries = np.linspace(1, n, q)

    size = (6,4.3)

    # update times
    fig = plt.figure(figsize=size)

    for i in range(len(algos)):
        plt.plot(x_updates, data[i][0], label=name_transorm(algos[i]), color=colors[i], marker=markers[i], markevery=int(n/50), markersize=5)
    plt.suptitle('Total Update Time (sec) (' + dataset + ', k = ' + str(k) + ')')
    plt.xlabel('Updates')
    # plt.ylabel('Total Update Time (sec)')
    plt.yscale('log')
    plt.legend()

    fig.tight_layout()

    with PdfPages(dir2 + 'update time plots/' + name + '_updates_' + dataset + "_" + str(k) + ".pdf") as pdf:
        pdf.savefig(fig)

    plt.close()

    # costs
    fig = plt.figure(figsize=size)

    for i in range(len(algos)):
        plt.plot(x_queries, data[i][2], label=name_transorm(algos[i]), color=colors[i], marker=markers[i], markevery=int(q/50), markersize=5)
    plt.suptitle('Cost of Solution (' + dataset + ', k = ' + str(k) + ')')
    plt.xlabel('Updates')
    # plt.ylabel('Cost')
    plt.legend()

    fig.tight_layout()

    with PdfPages(dir2 + 'cost plots/' + name + '_cost_' + dataset + "_" + str(k) + ".pdf") as pdf:
        pdf.savefig(fig)

    plt.close()

    # query times
    fig = plt.figure(figsize=size)

    for i in range(len(algos)):
        plt.plot(x_queries, data[i][1], label=name_transorm(algos[i]), color=colors[i], marker=markers[i], markevery=int(q/50), markersize=5)
    plt.suptitle('Average Query Time (sec) (' + dataset + ', k = ' + str(k) + ')')
    plt.xlabel('Updates')
    # plt.ylabel('Average Query Time (sec)')
    plt.legend()

    fig.tight_layout()

    with PdfPages(dir2 + 'query time plots/' + name + '_queries_' + dataset + "_" + str(k) + ".pdf") as pdf:
        pdf.savefig(fig)

    plt.close()

# CODE TO CREATE INDIVIDUAL PLOTS

def generate_plots(datasets, algos, k_values, colors, dir1, dir2, name):

    for dataset in datasets:
        for k in k_values:
            plot_single_graph(dataset, algos, k, colors, dir1, dir2, name)

def generate_justifications(name=""):

    datasets = ['song', 'census', 'kddcup']

    k_values = [50]

    bclp_algos = ['250_BCLP', '500_BCLP', '1000_BCLP']
    hk20_algos = ['250_HK20', '500_HK20', '1000_HK20']

    algos = bclp_algos + hk20_algos

    blues = ['#20B2AA', '#00BFFF', '#0000FF']
    reds = ['#FFA500', '#EE82EE', '#FF0000']

    colors = blues + reds

    generate_plots(datasets, algos, k_values, colors, "justification" + name + " (data)/", "justification" + name + " (plots)/", "justification" + name)

def generate_experiments(name=""):

    datasets = ['song', 'census', 'kddcup', 'drift', 'sift10M']

    k_values = [10, 50, 100]

    algos = ['500_BCLP'] + ['1000_HK20']

    colors = ['#0000FF'] + ['#FF0000']

    generate_plots(datasets, algos, k_values, colors, "experiments" + name + " (data)/", "experiments" + name + " (plots)/", "experiments" + name)

def generate_long_test():

    datasets = ['kddcup']

    k_values = [50]

    algos = ['500_BCLP'] + ['1000_HK20']

    colors = ['#0000FF'] + ['#FF0000']

    generate_plots(datasets, algos, k_values, colors, "large_test (data)/", "large_test (plots)/", "large_test")

# CODE TO EXTRACT METRICS

def get_query_times(dir):

    # datasets = ['kddcup']
    datasets = ['song', 'census', 'kddcup', 'drift', 'sift10M']

    # k_values = [50]
    k_values = [10, 50, 100]

    # algos = ['250_BCLP', '500_BCLP', '1000_BCLP', '250_HK20', '500_HK20', '1000_HK20']
    algos = ['500_BCLP'] + ['1000_HK20']

    average_query_times = np.zeros((len(datasets), len(k_values), len(algos)))

    for i in range(len(datasets)):
        for j in range(len(k_values)):
            for l in range(len(algos)):

                _,query_times,_ = load_data_single(datasets[i], k_values[j], algos[l], dir)

                average_query_times[i][j][l] = query_times[-1]

    # print them
    print("AVERAGE QUERY TIMES")

    for i in range(len(datasets)):
        print("-------------------------------------------")
        print("DATASET: " + datasets[i])
        print("-------------------------------------------")
        print("")

        for j in range(len(k_values)):
            print("k = " + str(k_values[j]) + ":")

            for l in range(len(algos)):
                print("  " + algos[l] + ":  " + str(average_query_times[i][j][l]))

            print("")

def get_ratios(dir):

    datasets = ['song', 'census', 'kddcup', 'drift', 'sift10M']
    # datasets = ['kddcup']

    k_values = [10, 50, 100]
    # k_values = [50]

    alg1 = '500_BCLP'
    alg2 = '1000_HK20'

    average_query_time_ratios = [[0,0,0],[0,0,0],[0,0,0],[0,0,0],[0,0,0]]
    average_cost_ratios = [[0,0,0],[0,0,0],[0,0,0],[0,0,0],[0,0,0]]
    total_update_time_ratios = [[0,0,0],[0,0,0],[0,0,0],[0,0,0],[0,0,0]]

    # average_query_time_ratios = [[0]]
    # average_cost_ratios = [[0]]
    # total_update_time_ratios = [[0]]

    # compute the ratios
    for i in range(len(datasets)):
        for j in range(len(k_values)):

            update_times1, query_times1, costs1 = load_data_single(datasets[i], k_values[j], alg1, dir)
            update_times2, query_times2, costs2 = load_data_single(datasets[i], k_values[j], alg2, dir)

            average_cost_ratio = 0
            for l in range(len(costs1)):
                if costs1[l] == 0 and costs2[l] == 0:
                    average_cost_ratio += 1/len(costs1)
                else:
                    average_cost_ratio += (costs2[l]/costs1[l])/len(costs1)

            average_cost_ratios[i][j] = average_cost_ratio

            average_query_time_ratios[i][j] = query_times2[-1]/query_times1[-1]

            total_update_time_ratios[i][j] = update_times2[-1]/update_times1[-1]

    # print them
    for i in range(len(datasets)):
        print("-------------------------------------------")
        print("DATASET: " + datasets[i])
        print("-------------------------------------------")

        for j in range(len(k_values)):
            print("k = " + str(k_values[j]))

            print("average cost ratio:       " + str(average_cost_ratios[i][j]))
            print("average query time ratio: " + str(average_query_time_ratios[i][j]))
            print("total update time ratio:  " + str(total_update_time_ratios[i][j]))
            print()

def get_ratios_justification(dir):

    datasets = ['song', 'census', 'kddcup']

    k = 50

    algo_pairs = [['250_BCLP', '500_BCLP'], ['1000_BCLP', '500_BCLP'], ['250_HK20','1000_HK20'], ['500_HK20','1000_HK20']]

    average_query_time_ratios = [[0,0,0,0],[0,0,0,0],[0,0,0,0]]
    average_cost_ratios = [[0,0,0,0],[0,0,0,0],[0,0,0,0]]
    total_update_time_ratios = [[0,0,0,0],[0,0,0,0],[0,0,0,0]]

    # compute the ratios
    for i in range(len(datasets)):
        for j in range(len(algo_pairs)):

            update_times1, query_times1, costs1 = load_data_single(datasets[i], 50, algo_pairs[j][1], dir)
            update_times2, query_times2, costs2 = load_data_single(datasets[i], 50, algo_pairs[j][0], dir)

            average_cost_ratio = 0
            for l in range(len(costs1)):
                if costs1[l] == 0 and costs2[l] == 0:
                    average_cost_ratio += 1/len(costs1)
                else:
                    average_cost_ratio += (costs2[l]/costs1[l])/len(costs1)

            average_cost_ratios[i][j] = average_cost_ratio

            average_query_time_ratios[i][j] = query_times2[-1]/query_times1[-1]

            total_update_time_ratios[i][j] = update_times2[-1]/update_times1[-1]

    # print them
    for i in range(len(datasets)):
        print("-------------------------------------------")
        print("DATASET: " + datasets[i])
        print("-------------------------------------------")

        for j in range(len(algo_pairs)):
            print(algo_pairs[j][0] + ' / ' + algo_pairs[j][1])

            print("average cost ratio:       " + str(average_cost_ratios[i][j]))
            print("average query time ratio: " + str(average_query_time_ratios[i][j]))
            print("total update time ratio:  " + str(total_update_time_ratios[i][j]))
            print()

if __name__ == '__main__':

    # get_ratios_justification('justification (data)/')

    # get_ratios_justification('justification_random (data)/')
    #
    # get_ratios('experiments (data)/')
    #
    # get_ratios('experiments_random (data)/')

    # generate the individual plots

    # generate_justifications()
    #
    # generate_justifications("_random")
    #
    # generate_experiments()
    #
    # generate_experiments("_random")

    # generate_long_test()

    # get_ratios('large_test (data)/')
    #
    # get_query_times('experiments (data)/')

    # print justification

    bclp_algos = ['250_BCLP', '500_BCLP', '1000_BCLP']
    hk20_algos = ['250_HK20', '500_HK20', '1000_HK20']

    plot_data(get_pages([50], [bclp_algos + hk20_algos]), 'justification (data)/')

    # print main results

    # plot_data(get_pages([10, 50, 100], [['500_BCLP'] + ['1000_HK20']]), 'experiments (data)/')

    # plot_data(get_pages([50], [['500_BCLP'] + ['1000_HK20']]), 'large_test (data)/')
