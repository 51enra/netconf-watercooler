package com.telekom.netconf.device.example.watercooler;

import com.telekom.netconf.device.example.watercooler.datastore.DOMDataService;
import com.telekom.netconf.device.example.watercooler.device.WatercoolerDeviceRunner;
import com.telekom.netconf.device.example.watercooler.processors.WatercoolerServiceTapProcessor;
import com.telekom.netconf.device.example.watercooler.rpcs.WatercoolerServiceImpl;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.lighty.core.common.models.ModuleId;
import io.lighty.netconf.device.NetconfDevice;
import io.lighty.netconf.device.NetconfDeviceBuilder;
import io.lighty.netconf.device.utils.ModelUtils;
import java.io.InputStream;
import java.util.Set;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    private ShutdownHook shutdownHook;

    public static void main(String[] args) {
        Main app = new Main();
        app.start(args, true, true);
    }

    public void start(String[] args) {
        start(args, false, true);
    }
    @SuppressFBWarnings({"SLF4J_SIGN_ONLY_FORMAT", "OBL_UNSATISFIED_OBLIGATION"})
    public void start(String[] args, boolean registerShutdownHook, final boolean initDataStore) {
        int port = getPortFromArgs(args);
        LOG.info("Deutsche Telekom Watercooler device started at port {}", port);
        LOG.info("\\  \\       /  /  __.              .__        ________               .__");
        LOG.info(" \\  \\     /  /__/  |______________|  | ______\\  ___  \\  ____ ___   _|__| __________");
        LOG.info("  \\  \\/\\ /  /__    __/   __/  .__/|  |/   __/ \\  \\ \\  \\/  __ \\  \\ / |  |/ .__/  __ \\");
        LOG.info("   \\  / \\  /   |  |  |  |  |  |__ |  ||  |    /  /_/  /\\  ___/\\  . /|  |  |__\\  ___/");
        LOG.info("    \\/   \\/    |__|  |  |   \\____\\|__||  |   /____   /  \\____> \\__/ |__|\\_____\\____>");
        LOG.info("                      \\/               \\/          \\/                             ");

        LOG.info("[Based on https://github.com/PANTHEONtech/lighty-netconf-simulator]");

        //1. Load models from classpath
        Set<YangModuleInfo> watercoolerModule = ModelUtils.getModelsFromClasspath(
                ModuleId.from(
                        "urn:dt-network-automation-demo:watercooler", "watercooler", "2022-03-02"));

        //2. Initialize RPCs
        WatercoolerServiceImpl watercoolerService = new WatercoolerServiceImpl();
        WatercoolerServiceTapProcessor watercoolerServiceTapProcessor = new WatercoolerServiceTapProcessor(watercoolerService);

        //3. Initialize Netconf device
        final NetconfDeviceBuilder netconfDeviceBuilder = new NetconfDeviceBuilder()
                .setCredentials("admin", "admin")
                .setBindingPort(port)
                .withModels(watercoolerModule)
                .withDefaultRequestProcessors()
                .withDefaultNotificationProcessor()
                .withDefaultCapabilities()
                .withRequestProcessor(watercoolerServiceTapProcessor);

        // Initialize DataStores
        if (initDataStore) {
            InputStream initialOperationalData = Main.class
                    .getResourceAsStream("/initial-operational-datastore.xml");
            InputStream initialConfigurationData = Main.class
                    .getResourceAsStream("/initial-config-datastore.xml");

            netconfDeviceBuilder.setInitialOperationalData(initialOperationalData)
                    .setInitialConfigurationData(initialConfigurationData);
        }

        NetconfDevice netconfDevice = netconfDeviceBuilder.build();

        DOMDataService domDataService = new DOMDataService(netconfDevice.getNetconfDeviceServices().getDOMDataBroker());
        watercoolerService.setDOMDataService(domDataService);

        netconfDevice.start();

        WatercoolerDeviceRunner watercoolerDeviceRunner = new WatercoolerDeviceRunner(domDataService);

        //5. Register shutdown hook
        shutdownHook = new ShutdownHook(netconfDevice, watercoolerService, watercoolerDeviceRunner);
        if (registerShutdownHook) {
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        }
    }

    public void shutdown() {
        if (shutdownHook != null) {
            shutdownHook.execute();
        }
    }

    private static class ShutdownHook extends Thread {

        private final NetconfDevice netConfDevice;
        private final WatercoolerServiceImpl watercoolerService;
        private final WatercoolerDeviceRunner watercoolerDeviceRunner;

        ShutdownHook(NetconfDevice netConfDevice, WatercoolerServiceImpl watercoolerService,
                WatercoolerDeviceRunner watercoolerDeviceRunner) {
            this.netConfDevice = netConfDevice;
            this.watercoolerService = watercoolerService;
            this.watercoolerDeviceRunner = watercoolerDeviceRunner;
        }

        @Override
        public void run() {
            this.execute();
        }

        @SuppressWarnings("checkstyle:IllegalCatch")
        public void execute() {
            LOG.info("Shutting down DT Watercooler device.");
            if (watercoolerService != null) {
                watercoolerService.close();
            }
            if (watercoolerDeviceRunner != null) {
                watercoolerDeviceRunner.close();
            }
            if (netConfDevice != null) {
                try {
                    netConfDevice.close();
                } catch (Exception e) {
                    LOG.error("Failed to close Netconf device properly", e);
                }
            }
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private static int getPortFromArgs(String[] args) {
        try {
            return Integer.parseInt(args[0]);
        } catch (Exception e) {
            return 17830;
        }
    }

}
