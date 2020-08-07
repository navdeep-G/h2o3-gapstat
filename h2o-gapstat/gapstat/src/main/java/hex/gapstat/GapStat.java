package hex.gapstat;

import hex.ClusteringModelBuilder;
import hex.Model;
import hex.ModelCategory;
import hex.kmeans.KMeans;
import hex.kmeans.KMeansModel;
import water.*;
import water.fvec.Chunk;
import water.fvec.Frame;

import water.Job;
import water.fvec.*;

import java.util.Random;
import static water.util.Utils.getDeterRNG;

public class GapStat extends ClusteringModelBuilder<GapStatModel, GapStatModel.GapStatParameters, GapStatModel.GapStatOutput> {
    @Override
    public boolean isSupervised() {
        return false;
    }


    @Override
    public ModelCategory[] can_build() {
        return new ModelCategory[]{ModelCategory.Clustering,};
    }

    @Override
    public BuilderVisibility builderVisibility() {
        return BuilderVisibility.Experimental;
    }

    public GapStat(GapStatModel.GapStatParameters parms) {
        super(parms);
        init(false);
    }

    public GapStat(boolean startup_once) {
        super(new GapStatModel.GapStatParameters(), startup_once);
    }

    @Override
    protected GapStatDriver trainModelImpl() {
        return new GapStatDriver();
    }

    @Override
    public void init(boolean expensive) {
        super.init(expensive);
    }


    // ----------------------
    private class GapStatDriver extends Driver {
        @Override
        public void compute2() {
            GapStatModel gs_model = null;
            KMeans km = null;
            KMeans km2 = null;
            Job globalJob = null;
            Job globalJob2 = null;
            KMeansModel kMeansModel = null;
            KMeansModel kMeansModel2 = null;
            Key frKMean2 = Key.make("fr_kmean2");
            // the initial pooled within cluster sum of squares for each iteration.
            _parms._wks = new double[_parms._k_max];
            // the log of the _wks
            _parms._wkbs = new double[_parms._k_max];
            // The standard error from the Monte Carlo simulated data for each iteration.
            _parms._sk = new double[_parms._k_max];
            // The gap statistics per value of k: (1 <= k <= k_max.
            _parms._gap_stats = new double[_parms._wks.length];
            try {
                Scope.enter();
                _parms.read_lock_frames(_job);
                init(true);

                gs_model = new GapStatModel(_job._result, _parms, new  GapStatModel.GapStatOutput(GapStat.this));
                gs_model.delete_and_lock(_job);

                for (int k = 1; k <= _parms._k_max; ++k) {
                    Key<Model> kmeansKey = Key.make("kmeans_key_gapstat_" + gs_model._key);
                    globalJob = new Job<>(kmeansKey, ClusteringModelBuilder.javaName("kmeans"), "k-Means");
                    km = KMeans.make("KMeans", globalJob, kmeansKey);
                    km._parms._train = _parms._train;
                    km._parms._ignored_columns = _parms._ignored_columns;
                    km._parms._max_iterations = _parms._max_iter;
                    km._parms._k = k;
                    km._parms._init = KMeans.Initialization.Furthest;
                    km._parms._seed = _parms._seed;
                    kMeansModel = km.trainModel().get();

                    Futures fs = new Futures();
                    DKV.remove(Key.make(km.dest()+"_clusters"), fs);
                    _parms._wks[k - 1] = Math.log(kMeansModel._output._tot_withinss);

                    double[] bwkbs = new double[_parms._b_max];
                    for (int b = 0; b < _parms._b_max; ++b) {
                        Frame bs = new MRTask() {
                            @Override
                            public void map(Chunk[] chks, NewChunk[] nchks) {
                                final Random rng = getDeterRNG(_parms._seed + chks[0].cidx());

                                for (int row = 0; row < Math.floor(_parms._bootstrap_fraction * chks[0]._len); ++row) {
                                    for (int col = 0; col < chks.length; ++ col) {
                                        if (train().vecs()[col].isConst()) {
                                            nchks[col].addNum(train().vecs()[col].max());
                                            continue;
                                        }
                                        if (train().vecs()[col].isCategorical()) {
                                            nchks[col].addCategorical((int)chks[col].at8(row));
                                            continue;
                                        }
                                        double d = rng.nextDouble() * train().vecs()[col].max() + train().vecs()[col].min();
                                        nchks[col].addNum(d);
                                    }
                                }
                            }
                        }.doAll(train().types(), train()).outputFrame(frKMean2, train().names(), train().domains());
                        Key<Model> kmeansKey2 = Key.make("kmeans_key_gapstat_2_" + gs_model._key);
                        globalJob2 = new Job<>(kmeansKey2, ClusteringModelBuilder.javaName("kmeans"), "k-Means");
                        km2 = KMeans.make("KMeans", globalJob2, kmeansKey2);
                        km2._parms._train = bs._key;
                        km2._parms._ignored_columns = _parms._ignored_columns;
                        km2._parms._max_iterations = _parms._max_iter;
                        km2._parms._k = k;
                        km2._parms._init = KMeans.Initialization.Furthest;
                        km2._parms._seed = _parms._seed;
                        kMeansModel2 = km2.trainModel().get();

                        Futures fs2 = new Futures();
                        DKV.remove(Key.make(km2.dest()+"_clusters"), fs2);
                        double res_bs = kMeansModel2._output._tot_withinss;
                        bwkbs[b] = Math.log(res_bs);
                        _parms._b = b+1;
                        gs_model.update();
                    }
                    double sum_bwkbs = 0.;
                    for (double d: bwkbs) sum_bwkbs += d;
                    _parms._wkbs[k - 1] = sum_bwkbs / _parms._b_max;
                    double sk_2 = 0.;
                    for (double d: bwkbs) {
                        sk_2 += (d - _parms._wkbs[k - 1]) * (d - _parms._wkbs[k - 1]) * 1. / (double) _parms._b_max;
                    }
                    _parms._sk[k - 1] = Math.sqrt(sk_2) * Math.sqrt(1 + 1. / (double) _parms._b_max);
                    _parms._k = k;
                    for(int i = 0; i < _parms._wks.length; ++i) _parms._gap_stats[i] = _parms._wkbs[i] - _parms._wks[i];
                    gs_model.update();
                }
                gs_model._output._best_k = gs_model.compute_best_k();
                gs_model._output._gap_stats = gs_model._parms.gaps();
            } finally {
                if (gs_model != null) gs_model.unlock(_job);
                if (km != null) DKV.remove(km._job._key);
                if (km2 != null) DKV.remove(km2._job._key);
                if (km != null) DKV.remove(km.dest());
                if (km2 != null) DKV.remove(km2.dest());
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

        }
    }
}