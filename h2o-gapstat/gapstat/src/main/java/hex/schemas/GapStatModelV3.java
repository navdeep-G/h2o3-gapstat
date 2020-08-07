package hex.schemas;

import hex.gapstat.GapStatModel;
import water.api.API;
import water.api.schemas3.ModelOutputSchemaV3;
import water.api.schemas3.ModelSchemaV3;

public class GapStatModelV3 extends ModelSchemaV3<GapStatModel, GapStatModelV3, GapStatModel.GapStatParameters, GapStatV3.GapStatParametersV3, GapStatModel.GapStatOutput, GapStatModelV3.GapStatModelOutputV3> {

    public static final class GapStatModelOutputV3 extends ModelOutputSchemaV3<GapStatModel.GapStatOutput, GapStatModelOutputV3> {
        @API(help = "The gap statistics per value of k.")
        double[] gap_stats;
    }

    public GapStatV3.GapStatParametersV3 createParametersSchema() {
        return new GapStatV3.GapStatParametersV3();
    }

    public GapStatModelOutputV3 createOutputSchema() {
        return new GapStatModelOutputV3();
    }


    // Version&Schema-specific filling into the impl
    @Override
    public GapStatModel createImpl() {
        GapStatModel.GapStatParameters parms = parameters.createImpl();
        return new GapStatModel(model_id.key(), parms, null);
    }
}