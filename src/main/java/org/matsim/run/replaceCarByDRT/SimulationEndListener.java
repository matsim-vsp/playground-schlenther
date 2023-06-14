package org.matsim.run.replaceCarByDRT;

import org.matsim.analysis.RunPostprocessing;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerListenerManager;
import org.matsim.core.controler.corelisteners.ControlerDefaultCoreListenersModule;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.events.ControlerEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.ShutdownListener;


public class SimulationEndListener implements ShutdownListener {

    private Controler controler;

    public SimulationEndListener(Controler controler) {
        this.controler = controler;
    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        Config config = controler.getConfig();

        String runDirectory = config.controler().getOutputDirectory();
        String runId = config.controler().getRunId();
        String population = runDirectory + runId + ".output_plans.xml.gz";
        String inner_city_shp = "scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-carBanArea.shp";
        String berlin_shp = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-shp/berlin.shp";
        String pr_stations = "scenarios/berlin/replaceCarByDRT/noModeChoice/prStations/2023-03-29-pr-stations.tsv";

        RunPostprocessing postprocessor = new RunPostprocessing(runDirectory, runId, population, inner_city_shp, berlin_shp, pr_stations);
        postprocessor.run();
    }
}
