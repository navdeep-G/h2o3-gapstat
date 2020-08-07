package hex.schemas;

import hex.gapstat.GapStat;
import hex.gapstat.GapStatModel;
import water.api.API;
import water.api.schemas3.ModelParametersSchemaV3;

public class GapStatV3 extends ModelBuilderSchema<GapStat, GapStatV3, GapStatV3.GapStatParametersV3> {

    public static final class GapStatParametersV3 extends ModelParametersSchemaV3<GapStatModel.GapStatParameters, GapStatParametersV3> {
        static public String[] fields = new String[]{
                "training_frame",
                "ignored_columns",
                "wks",
                "wkbs",
                "sk",
                "k_max",
                "b_max",
                "k",
                "b",
                "gap_stats",
                "k_best",
                "max_iter",
                "bootstrap_fraction",
                "seed"

        };

        // Inputs
        @API(help = "Max iteratiors per clustering")
        public int max_iter = 50;

        @API(help = "Fraction of data size to replicate in each Monte Carlo simulation")
        public double bootstrap_fraction = .1;

        @API(help = "The maximum number of clusters to consider, must be at least two")
        int k_max = 5;

        @API(help = "Number of Monte Carlo (bootstrap) samples")
        int b_max = 10;

        // Outputs
        @API(help = "Optimal number of clusters")
        int best_k = 1;
        @API(help = "The gap statistics per value of k: (1 <= k <= k_max")
        double[] gap_stats;

    }
}