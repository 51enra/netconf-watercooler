package com.telekom.netconf.device.example.watercooler.processors;

import org.opendaylight.yang.gen.v1.urn.dt.network.automation.demo.watercooler.rev220302.TapInput;
import org.opendaylight.yang.gen.v1.urn.dt.network.automation.demo.watercooler.rev220302.TapOutput;
import org.opendaylight.yang.gen.v1.urn.dt.network.automation.demo.watercooler.rev220302.WatercoolerService;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;

public class WatercoolerServiceTapProcessor extends WatercoolerServiceAbstractProcessor<TapInput, TapOutput> {

    private static final Logger LOG = LoggerFactory.getLogger(WatercoolerServiceTapProcessor.class);

    private final WatercoolerService watercoolerService;
    private final QName qName = QName.create("urn:dt-network-automation-demo:watercooler", "tap");

    public WatercoolerServiceTapProcessor(final WatercoolerService watercoolerService) {
        this.watercoolerService = watercoolerService;
    }

    @Override
    protected Future<RpcResult<TapOutput>> execMethod(TapInput input) {
        return this.watercoolerService.tap(input);
    }

    @Override
    public QName getIdentifier() {
        return this.qName;
    }
}
