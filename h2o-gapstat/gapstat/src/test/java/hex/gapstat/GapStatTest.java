package hex.gapstat;

import hex.kmeans.KMeans;
import hex.kmeans.KMeansModel;
import org.junit.BeforeClass;
import org.junit.Test;
import water.TestUtil;
import water.fvec.Frame;

import java.util.Arrays;

public class GapStatTest extends TestUtil {
    @BeforeClass() public static void setup() { stall_till_cloudsize(1); }

    @Test
    public void gapStatIrisFromR() {
        gapStatIris(true);
    }

    @Test
    public void gapStatIrisWHeader() {
        gapStatIris(false);
    }

    public void gapStatIris(boolean useIrisFromR) {
        GapStatModel gs = null;
        Frame fr = null;
        String filePath;
        String target;

        KMeansModel km = null;

        try {
            if (useIrisFromR) {
                filePath = "smalldata/iris/iris_r.csv";
                target = "Species";
            } else {
                filePath = "smalldata/iris/iris_wheader.csv";
                target = "class";
            }
            fr = parse_test_file(filePath);
            fr.remove(target).remove();

            GapStatModel.GapStatParameters parms = new GapStatModel.GapStatParameters();
            parms._train = fr._key;
            parms._k_max = 10;
            parms._b_max = 10;
            parms._seed = 1234;

            gs = new GapStat(parms).trainModel().get();
            System.out.println("Best K: " + gs._output._best_k);
            System.out.println("Gap Statistics per cluster in range from 1 to `k_max`: " + Arrays.toString(gs._output._gap_stats));

            KMeansModel.KMeansParameters parms_km = new KMeansModel.KMeansParameters();
            parms_km._train = fr._key;
            parms_km._seed = 1234;
            parms_km._max_iterations = 50;
            parms_km._k = 10;
            parms_km._estimate_k = true;
            parms_km._init = KMeans.Initialization.Furthest;

            km = new KMeans(parms_km).trainModel().get();

            // Look at clusters for Gap and KMeans
            System.out.println("Number of clusters and: " + km._output._size.length);
            System.out.println("Size of each cluster: " + Arrays.toString(km._output._size));
            System.out.println("Best K: " + gs._output._best_k);
            System.out.println("Gap Statistics per cluster in range from 1 to `k_max`: " + Arrays.toString(gs._output._gap_stats));



        } finally {
            if (fr != null) fr.delete();
            if (gs != null) gs.delete();
            if (km != null) km.delete();
        }
    }
}