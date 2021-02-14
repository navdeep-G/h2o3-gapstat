package hex.gapstat;

import hex.ClusteringModel;
import hex.ModelCategory;
import hex.ModelMetrics;
import water.H2O;
import water.Key;

public class GapStatModel extends ClusteringModel<GapStatModel, GapStatModel.GapStatParameters, GapStatModel.GapStatOutput> {

    public static class GapStatParameters extends ClusteringModel.ClusteringParameters {
        public String algoName() {
            return "GapStat";
        }

        public String fullName() {
            return "Gap Statistic";
        }

        public String javaName() {
            return GapStatModel.class.getName();
        }

        @Override
        public long progressUnits() {
            return _k;
        }

        // The maximum number of clusters to consider in each iteration, must be at least two
        int _k_max = 5;
        // the initial pooled within cluster sum of squares for each iteration.
        double[] _wks = new double[/*_k_max*/]{0};
        // the log of the _wks
        double[] _wkbs = new double[/*_k_max*/]{0};
        // The standard error from the Monte Carlo simulated data for each iteration.
        double[] _sk = new double[/*_k_max*/]{0};
        // Number of Monte Carlo (bootstrap) samples.
        int _b_max = 10;
        // The current value of k_max: (2 <= k <= k_max).
        int _k;
        // The current value of B (1 <= b <= b_max.
        int _b;
        // Optimal number of clusters.
        int _best_k;
        // The gap statistics per value of k: (1 <= k <= k_max.
        double[] _gap_stats = new double[/*_wks.length*/]{0};
        // Max iteratiors per clustering
        public int _max_iter = 50;
        // Fraction of data size to replicate in each Monte Carlo simulation.
        public double _bootstrap_fraction = 1.0;


        public double[] wks() { return _wks; }
        public double[] sk() {return _sk; }
        public double[] gaps() {return _gap_stats; }

    }

    public static class GapStatOutput extends ClusteringModel.ClusteringOutput {

        public int _best_k;
        public double[] _gap_stats;

        public GapStatOutput(GapStat b) {
            super(b);
        }

        @Override
        public ModelCategory getModelCategory() {
            return ModelCategory.Clustering;
        }
    }

    public GapStatModel(Key selfKey, GapStatParameters parms, GapStatOutput output) {
        super(selfKey, parms, output);
    }

    @Override
    public ModelMetrics.MetricBuilder makeMetricBuilder(String[] domain) {
        throw H2O.unimpl("No Model Metrics for GapStatModel.");
    }

    @Override
    protected double[] score0(double data[/*ncols*/], double preds[/*nclasses+1*/]) {
        throw H2O.unimpl();

    }

    public int compute_best_k() {
        double[] gaps = _parms.gaps();
        double[] log_wks = _parms.wks();
        double[] sks = _parms.sk();
        int kmin = -1;
        for (int i = 0; i < gaps.length - 1; ++i) {
            int cur_k = i + 1;
            if (gaps[cur_k] == 0) {
                kmin = 0;
                _parms._best_k = 1; //= kmin;
                break;
            }
            if (i == gaps.length - 1) {
                kmin = cur_k;
                _parms._best_k = kmin;
                break;
            }
            if (gaps[i] >= (gaps[i + 1] - sks[i + 1])) {
                kmin = cur_k;
                _parms._best_k = kmin;
                break;
            }
        }

        if (kmin <= 0) _parms._best_k = 1;

        if (log_wks[log_wks.length - 1] != 0) {
            if (kmin > 1) _parms._best_k = kmin;
        }

        if (_parms._best_k <= 0) _parms._best_k = (int)Double.NaN;
        if (_parms._best_k == 0) _parms._best_k = 1;

        return kmin;
    }

}