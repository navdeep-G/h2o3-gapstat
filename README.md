# Gap Statistic

## Overview

This repository provides an implementation of the Gap Statistic, a clustering evaluation method, integrated into [H2O-3](https://github.com/h2oai/h2o-3), H2O.ai's open-source machine learning platform. The Gap Statistic helps determine the optimal number of clusters in a dataset by comparing the within-cluster dispersion to that of a reference null distribution, effectively evaluating whether a proposed clustering structure is significantly better than random noise.

## Gap Statistic Methodology

The Gap Statistic compares the total within-cluster variation for different numbers of clusters with their expected values under a null reference distribution (i.e., a distribution with no obvious clustering). The method calculates the gap between observed and expected within-cluster dispersion for each number of clusters, and the optimal number of clusters is where the gap is maximized.

### Key Features

- **Integrated with H2O-3**: This implementation is designed to work within the H2O-3 framework, making it compatible with H2O’s distributed and scalable clustering algorithms.
- **Flexible Cluster Evaluation**: The Gap Statistic can evaluate clustering results from various algorithms, such as K-means, that are available in H2O-3.
- **Automated Selection of Optimal Clusters**: Automatically suggests the ideal number of clusters based on where the Gap Statistic reaches its maximum.

## Usage

To use the Gap Statistic within H2O-3, follow these steps:

1. **Install H2O-3**: If you haven't already, install H2O-3 by following the instructions on the [H2O-3 GitHub page](https://github.com/h2oai/h2o-3).
2. **Prepare Data**: Load your dataset into H2O-3.
3. **Run Gap Statistic**: Execute the Gap Statistic function with your chosen clustering algorithm and data to evaluate the optimal number of clusters.

Example usage and detailed instructions can be found in the repository’s example notebook or script files.

## Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/username/gap-statistic-h2o3.git

### Install dependencies

- Ensure H2O-3 is installed as per [H2O.ai’s documentation](https://github.com/h2oai/h2o-3).
- Run the example code provided to test the implementation.

## Implementation Details

This implementation builds upon the principles outlined in the original paper by Tibshirani et al., which introduced the Gap Statistic. We follow this methodology to provide a robust method for determining the cluster count within H2O-3.

### References

- Tibshirani, R., Walther, G., & Hastie, T. (2001). "Estimating the number of clusters in a data set via the Gap statistic." *Journal of the Royal Statistical Society: Series B (Statistical Methodology)*.
  - [Stanford Gap Statistic Methodology](https://statweb.stanford.edu/~gwalther/gap)
  - [R Documentation for Gap Statistic](https://stat.ethz.ch/R-manual/R-devel/library/cluster/html/clusGap.html)

## Contributing

Contributions are welcome! Please open an issue or submit a pull request if you have suggestions or improvements for this implementation.
