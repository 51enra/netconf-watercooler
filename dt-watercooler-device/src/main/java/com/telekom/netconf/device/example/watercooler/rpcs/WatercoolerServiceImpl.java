package com.telekom.netconf.device.example.watercooler.rpcs;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.telekom.netconf.device.example.watercooler.datastore.DOMDataService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.opendaylight.yang.gen.v1.urn.dt.network.automation.demo.watercooler.rev220302.TapInput;
import org.opendaylight.yang.gen.v1.urn.dt.network.automation.demo.watercooler.rev220302.TapOutput;
import org.opendaylight.yang.gen.v1.urn.dt.network.automation.demo.watercooler.rev220302.TapOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.dt.network.automation.demo.watercooler.rev220302.WatercoolerService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

public class WatercoolerServiceImpl implements WatercoolerService, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(WatercoolerServiceImpl.class);

    private final ExecutorService executor;

    private DOMDataService domDataService;

    public WatercoolerServiceImpl() {
        this.executor = Executors.newFixedThreadPool(1);
    }

    @Override
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    public ListenableFuture<RpcResult<TapOutput>> tap(final TapInput input) {
        //TODO: Check if cupsize is available as input?
        LOG.info("Tapping water; cupSize {}", input.getCupSize().toString());
        final SettableFuture<RpcResult<TapOutput>> result = SettableFuture.create();
        this.executor.submit(new Callable<RpcResult<TapOutput>>() {
            @Override
            public RpcResult<TapOutput> call() throws Exception {
                final TapOutput tapResult = tapFromWatercooler(input.getCupSize());
                final RpcResult<TapOutput> rpcResult = RpcResultBuilder.success(tapResult).build();
                result.set(rpcResult);
                return rpcResult;
            }
        });
       return result;
    }

    @Override
    public void close() {
        this.executor.shutdown();
    }

    public void setDOMDataService(DOMDataService domDataService) {
        this.domDataService = domDataService;
    }

    private TapOutput tapFromWatercooler(TapInput.CupSize cupSize) {
        final int currentFillLevel = domDataService.getWatercoolerFillLevel();
        final int fillLevelAfterTap = currentFillLevel - cupSize.getIntValue();
        if (fillLevelAfterTap >= 0) {
            domDataService.setWatercoolerFillLevel(fillLevelAfterTap);
            return new TapOutputBuilder()
                    .setRemainingFillLevel(Uint32.valueOf(fillLevelAfterTap))
                    .setTapSuccesful(TapOutput.TapSuccesful.Yes).build();
        } else {
            return new TapOutputBuilder()
                    .setRemainingFillLevel(Uint32.valueOf(currentFillLevel))
                    .setTapSuccesful(TapOutput.TapSuccesful.NoInsufficientWaterLevel).build();
        }
    }
}
