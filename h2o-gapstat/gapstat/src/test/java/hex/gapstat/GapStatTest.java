package hex.gapstat;

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
            parms._k_max = 6;
            parms._b_max = 10;
            parms._seed = 1234;

            gs = new GapStat(parms).trainModel().get();
            System.out.println("Best K: " + gs._output._best_k);
            System.out.println("Gap Statistics per cluster in range from 1 to `k_max`: " + Arrays.toString(gs._output._gap_stats));

        } finally {
            if (fr != null) fr.delete();
            if (gs != null) gs.delete();
        }
    }
}