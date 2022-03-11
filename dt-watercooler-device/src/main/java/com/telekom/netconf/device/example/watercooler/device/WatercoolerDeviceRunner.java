package com.telekom.netconf.device.example.watercooler.device;

import com.telekom.netconf.device.example.watercooler.datastore.DOMDataService;
import org.opendaylight.yang.gen.v1.urn.dt.network.automation.demo.watercooler.rev220302.Watercooler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.lang.Integer.min;

public class WatercoolerDeviceRunner implements AutoCloseable{

    private static final Logger LOG = LoggerFactory.getLogger(WatercoolerDeviceRunner.class);

    private final ScheduledExecutorService executor;

    private DOMDataService domDataService;

    public WatercoolerDeviceRunner(DOMDataService domDataService) {
        this.domDataService = domDataService;
        this.executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this.run, 0, 2, TimeUnit.SECONDS);
        LOG.info("WatercoolerDeviceRunner started.");
    }
    
    private Runnable run = new Runnable() {
        @Override
        public void run() {
            int refillRate = domDataService.getWatercoolerRefillRate();
            int fillLevel = min(domDataService.getWatercoolerFillLevel() + refillRate, 100);
            domDataService.setWatercoolerFillLevel(fillLevel);
            Watercooler.OverflowIndicator overflowIndicator = (fillLevel<99)?
                    Watercooler.OverflowIndicator.Off: Watercooler.OverflowIndicator.On;
            domDataService.setWatercoolerOverflowIndicator(overflowIndicator);
        }
    };

    @Override
    public void close() {
        this.executor.shutdown();
    }
}
