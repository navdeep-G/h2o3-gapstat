package hex.api;

import hex.gapstat.GapStat;
import water.api.AlgoAbstractRegister;
import water.api.RestApiContext;
import water.api.SchemaServer;

public class RegisterRestApi extends AlgoAbstractRegister {

    @Override
    public void registerEndPoints(RestApiContext context) {
        GapStat pam = new GapStat(true);
        registerModelBuilder(context, pam, SchemaServer.getStableVersion());
    }

    @Override
    public String getName() {
        return "GapStat";
    }

}
