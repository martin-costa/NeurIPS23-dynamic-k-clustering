# NeurIPS'23 Fully Dynamic k-Clustering

This repository contains the Java code used to run the experiments in our NeurIPS'23 paper 'Fully Dynamic $k$-Clustering in $\tilde O(k)$ Update Time'.

For a detailed description of our algorithm and the experiments, please see [our paper](https://drive.google.com/file/d/1CyV2aT0j3slsOQ4nBGYVl7R6T8QUuOL1/view "our paper").

## Input Data

The input data should be stored in a file with the following format: The first line should contain two numbers $n$ and and $d$ separated by a single space. The first number $n$ should be equal to the number of data points, and the second number $d$ should be equal to the number of dimensions of each data point. The $n$ subsequent lines should each correspond to one of the $n$ points, where each line contains the $d$-dimensional embedding of the corresponding point as $d$ numbers separated by single spaces.

**Location:** The file should be located in a folder named *data*, which should be placed in the same directory as the folder containing the code in this repository.

## Running the Algorithms

To run the algorithms, we have Java files `RunDynamicMP.java` and `RunHenzingerKale.java`, that can be used to run our dynamic algorithm and the dynamic algorithm of Henzinger and Kale respectively.

Both of these have the following input parameters:
* The number of centers in our solution, $k$
* The name of the dataset, *dataset*  
* The number of datapoints to be used in the update stream, $n$
* The size of the sliding window, *windowLength*
* The number of queries to be performed, *queryCount*

Additionally, `RunDynamicMP.java` takes as input the parameter $\phi$, which determines how many points are sampled at each layer, and the (optional) parameters $\beta$ and $\epsilon$, which control the sizes of the layers and how often they are reconstructed respectively. If not specified, $\beta$ and $\epsilon$ are set to $0.5$ and $0.2$ respectively by default. `RunHenzingerKale.java` also takes as input the parameter $\psi$, which determines the sizes of the coresets constructed by the algorithm.

In order to run these algorithms, ensure you have Java installed and run the following commands in the terminal:

```
java RunDynamicMP k dataset n widownLength queryCount phi beta epsilon
```

```
java RunHenzingerKale k dataset n widownLength queryCount psi
```

For example, executing the command

```
java RunDynamicMP 10 song 400 200 15 40
```

will initialise our dynamic algorithm with parameters $k=10$, $\phi = 40$, $\beta = 0.5$, $\epsilon = 0.2$, and then take the first $400$ points from the dataset *song* and feed them to our algorithm sequentially using a sliding window of length $200$.

## Output Data

Running `RunDynamicMP.java` will create 3 files:
* [dataset]\_[k]\_[phi]\_BCLP\_updatetime
* [dataset]\_[k]\_[phi]\_BCLP\_querytime
* [dataset]\_[k]\_[phi]\_BCLP\_cost

in the folder *test\_results*, where [dataset], [k], and [phi] respectively denote the values of these parameters. For example, running the command above will produce files named 'song_10_40_updatetime', 'song_10_40_querytime', and 'song_10_40_cost'. Each file consists of a sequence of numbers, separated by the symbol '#'. The $i^{th}$ number in:

* [dataset]\_[k]\_[phi]\_BCLP\_updatetime is the time taken to handle the $i^{th}$ update (in nano seconds)
* [dataset]\_[k]\_[phi]\_BCLP\_querytime is the time taken to handle the $i^{th}$ query (in nano seconds)
* [dataset]\_[k]\_[phi]\_BCLP\_cost is the cost of the solution produced by the $i^{th}$ query

Running `RunHenzingerKale.java` will produce an analogous output, where the prefix of the files is [dataset]\_[k]\_[psi]\_KH20 instead.
