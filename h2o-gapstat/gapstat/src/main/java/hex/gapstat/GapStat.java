package hex.gapstat;

import hex.ClusteringModelBuilder;
import hex.ModelCategory;
import hex.kmeans.KMeans;
import hex.kmeans.KMeansModel;
import water.*;
import water.fvec.Chunk;
import water.fvec.Frame;
import water.fvec.NewChunk;

import java.util.Random;

public class GapStat extends ClusteringModelBuilder<GapStatModel, GapStatModel.GapStatParameters, GapStatModel.GapStatOutput> {

    public GapStat(GapStatModel.GapStatParameters parms) {
        super(parms);
        init(false);
    }

    public GapStat(boolean startup_once) {
        super(new GapStatModel.GapStatParameters(), startup_once);
    }

    @Override
    public boolean isSupervised() {
        return false;
    }

    @Override
    public ModelCategory[] can_build() {
        return new ModelCategory[]{ModelCategory.Clustering};
    }

    @Override
    public BuilderVisibility builderVisibility() {
        return BuilderVisibility.Experimental;
    }

    @Override
    public void init(boolean expensive) {
        super.init(expensive);
    }

    @Override
    protected GapStatDriver trainModelImpl() {
        return new GapStatDriver();
    }

    // Main logic for computing Gap Statistics
    private class GapStatDriver extends Driver {
        @Override
        public void compute2() {
            GapStatModel gs_model = null;
            KMeans km = null, km2 = null;
            Job globalJob, globalJob2;
            KMeansModel kMeansModel = null, kMeansModel2 = null;
            Key frKMean2 = Key.make("fr_kmean2");

            _parms._wks = new double[_parms._k_max];
            _parms._wkbs = new double[_parms._k_max];
            _parms._sk = new double[_parms._k_max];
            _parms._gap_stats = new double[_parms._k_max];

            try {
                Scope.enter();
                _parms.read_lock_frames(_job);
                init(true);

                gs_model = new GapStatModel(_job._result, _parms, new GapStatModel.GapStatOutput(GapStat.this));
                gs_model.delete_and_lock(_job);

                for (int k = 1; k <= _parms._k_max; ++k) {
                    // Train KMeans on original data
                    Key kmeansKey = Key.make("kmeans_key_gapstat_" + gs_model._key);
                    globalJob = new Job<>(kmeansKey, ClusteringModelBuilder.javaName("kmeans"), "k-Means");
                    km = KMeans.make("KMeans", globalJob, kmeansKey);
                    km._parms._train = _parms._train;
                    km._parms._ignored_columns = _parms._ignored_columns;
                    km._parms._max_iterations = _parms._max_iter;
                    km._parms._k = k;
                    km._parms._init = KMeans.Initialization.Furthest;
                    km._parms._seed = _parms._seed;

                    kMeansModel = km.trainModel().get();
                    DKV.remove(Key.make(km.dest() + "_clusters"), new Futures());
                    _parms._wks[k - 1] = Math.log(kMeansModel._output._withinss[k - 1]);

                    double[] bwkbs = new double[_parms._b_max];

                    for (int b = 0; b < _parms._b_max; ++b) {
                        // Generate bootstrap sample
                        Frame bs = new MRTask() {
                            @Override
                            public void map(Chunk[] chks, NewChunk[] nchks) {
                                Random rng = new Random();
                                int rows = (int) Math.floor(_parms._bootstrap_fraction * chks[0]._len);

                                for (int row = 0; row < rows; ++row) {
                                    for (int col = 0; col < chks.length; ++col) {
                                        if (train().vecs()[col].isConst()) {
                                            nchks[col].addNum(train().vecs()[col].max());
                                        } else if (train().vecs()[col].isCategorical()) {
                                            nchks[col].addCategorical((int) chks[col].at8(row));
                                        } else {
                                            double d = rng.nextDouble() * train().vecs()[col].max() + train().vecs()[col].min();
                                            nchks[col].addNum(d);
                                        }
                                    }
                                }
                            }
                        }.doAll(train().types(), train()).outputFrame(frKMean2, train().names(), train().domains());

                        // Train KMeans on bootstrap sample
                        Key kmeansKey2 = Key.make("kmeans_key_gapstat_2_" + gs_model._key);
                        globalJob2 = new Job<>(kmeansKey2, ClusteringModelBuilder.javaName("kmeans"), "k-Means");
                        km2 = KMeans.make("KMeans", globalJob2, kmeansKey2);
                        km2._parms._train = bs._key;
                        km2._parms._ignored_columns = _parms._ignored_columns;
                        km2._parms._max_iterations = _parms._max_iter;
                        km2._parms._k = k;
                        km2._parms._init = KMeans.Initialization.Furthest;
                        km2._parms._seed = _parms._seed;

                        kMeansModel2 = km2.trainModel().get();
                        DKV.remove(Key.make(km2.dest() + "_clusters"), new Futures());
                        bwkbs[b] = Math.log(kMeansModel2._output._withinss[k - 1]);

                        _parms._b = b + 1;
                        gs_model.update();
                    }

                    // Compute average and stddev of log(Wk*) from bootstrap
                    double sum_bwkbs = 0.0;
                    for (double val : bwkbs) sum_bwkbs += val;
                    _parms._wkbs[k - 1] = sum_bwkbs / _parms._b_max;

                    double sk2 = 0.0;
                    for (double val : bwkbs) {
                        double diff = val - _parms._wkbs[k - 1];
                        sk2 += diff * diff / _parms._b_max;
                    }

                    _parms._sk[k - 1] = Math.sqrt(sk2) * Math.sqrt(1.0 + 1.0 / _parms._b_max);

                    // Update gap stats
                    _parms._k = k;
                    for (int i = 0; i < _parms._wks.length; ++i)
                        _parms._gap_stats[i] = _parms._wkbs[i] - _parms._wks[i];

                    gs_model.update();
                }

                gs_model._output._best_k = gs_model.compute_best_k();
                gs_model._output._gap_stats = _parms._gap_stats;

            } finally {
                if (gs_model != null) gs_model.unlock(_job);
                if (km != null) {
                    DKV.remove(km._job._key);
                    DKV.remove(km.dest());
                }
                if (km2 != null) {
                    DKV.remove(km2._job._key);
                    DKV.remove(km2.dest());
                }
                if (frKMean2 != null) DKV.remove(frKMean2);
                if (kMeansModel != null) kMeansModel.remove();
                if (kMeansModel2 != null) kMeansModel2.remove();
                _parms.read_unlock_frames(_job);
                Scope.exit(gs_model == null ? null : gs_model._key);
            }

            tryComplete();
        }

        @Override
        public void computeImpl() {
            // Required override; can be left empty
        }
    }
}
