package com.telekom.netconf.device.example.watercooler.datastore;

import com.google.common.util.concurrent.FluentFuture;
import io.lighty.netconf.device.utils.TimeoutUtil;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.yang.gen.v1.urn.dt.network.automation.demo.watercooler.rev220302.Watercooler;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DOMDataService {

    private static final Logger LOG = LoggerFactory.getLogger(DOMDataService.class);

    private static final String WATERCOOLERNS = "urn:dt-network-automation-demo:watercooler";
    private static final String WATERCOOLERREV = "2022-03-02";

    private static final QName WATERCOOLERQNAME = QName.create(WATERCOOLERNS, WATERCOOLERREV, "watercooler");
    private static final QName FILLLEVELQNAME = QName.create(WATERCOOLERNS, WATERCOOLERREV, "fillLevel");
    private static final QName REFILLRATEQNAME = QName.create(WATERCOOLERNS, WATERCOOLERREV, "refillRate");
    private static final QName OVERFLOWINDICATORQNAME =
            QName.create(WATERCOOLERNS, WATERCOOLERREV, "overflowIndicator");

    private final DOMDataBroker domDataBroker;

    public DOMDataService(DOMDataBroker domDataBroker) {
        this.domDataBroker = domDataBroker;
    }

    private Optional<NormalizedNode> readFromDOMDataStore(
            LogicalDatastoreType datastoreType, YangInstanceIdentifier path) {
        Optional<NormalizedNode> optionalElement = Optional.empty();
        try {
            DOMDataTreeReadTransaction domDataReadOnlyTransaction = this.domDataBroker.newReadOnlyTransaction();
            FluentFuture<Optional<NormalizedNode>> readElement =
                    domDataReadOnlyTransaction.read(datastoreType, path);
            optionalElement =
                    readElement.get(TimeoutUtil.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (ExecutionException | TimeoutException e) {
            LOG.error("Exception thrown while reading from datastore!", e);
        } catch (InterruptedException e) {
            LOG.error("Interrupted while reading from datastore!", e);
            Thread.currentThread().interrupt();
        }
        return optionalElement;
    }

    private void writeToDOMDataStore(
            LogicalDatastoreType datastoreType, YangInstanceIdentifier path, NormalizedNode node) {
        final DOMDataTreeWriteTransaction writeTx =
                this.domDataBroker.newWriteOnlyTransaction();
        writeTx.merge(LogicalDatastoreType.OPERATIONAL, path, node);
        try {
            writeTx.commit().get(TimeoutUtil.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (ExecutionException | TimeoutException e) {
            LOG.error("Exception thrown while committing fillLevel to datastore!", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while  committing fillLevel to datastore!", e);
        }
    }

    public int getWatercoolerFillLevel() {
        YangInstanceIdentifier path = YangInstanceIdentifier.of(WATERCOOLERQNAME).node(FILLLEVELQNAME);
        Optional<NormalizedNode> optionalNN = readFromDOMDataStore(LogicalDatastoreType.OPERATIONAL, path);
        if (optionalNN.isPresent()) {
            return ((Uint32)optionalNN.get().body()).intValue();
        } else {
            LOG.error("Could not read fillLevel from datastore!");
            return -1;
        }
    }

    public int getWatercoolerRefillRate() {
        YangInstanceIdentifier path = YangInstanceIdentifier.of(WATERCOOLERQNAME).node(REFILLRATEQNAME);
        Optional<NormalizedNode> optionalNN = readFromDOMDataStore(LogicalDatastoreType.CONFIGURATION, path);
        if (optionalNN.isPresent()) {
            return ((Uint32)optionalNN.get().body()).intValue();
        } else {
            LOG.error("Could not read refillRate from datastore!");
            return -1;
        }
    }

    public Watercooler.OverflowIndicator getWatercoolerOverflowIndicator() {
        YangInstanceIdentifier path = YangInstanceIdentifier.of(WATERCOOLERQNAME).node(OVERFLOWINDICATORQNAME);
        Optional<NormalizedNode> optionalNN = readFromDOMDataStore(LogicalDatastoreType.OPERATIONAL, path);
        if (optionalNN.isPresent()) {
            Optional<Watercooler.OverflowIndicator> optionalOI =
                    Watercooler.OverflowIndicator.forName(optionalNN.get().body().toString());
            if(optionalOI.isPresent()) {
                return optionalOI.get();
            }
        }
        LOG.error("Could not read overflow Indicator from datastore!");
        return null;
    }

    public void setWatercoolerFillLevel(int fillLevel) {
        if (fillLevel < 0 | fillLevel > 100) {
            LOG.error("Attempt to set watercooler fillLevel to out of range value {}.", fillLevel);
        }
        LeafNode<Object> leafNode = Builders.leafBuilder().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(FILLLEVELQNAME)).withValue(Uint32.valueOf(fillLevel)).build();
        YangInstanceIdentifier path = YangInstanceIdentifier.of(WATERCOOLERQNAME).node(FILLLEVELQNAME);
        writeToDOMDataStore(LogicalDatastoreType.OPERATIONAL, path, leafNode);
    }

    public void setWatercoolerRefillRate(int refillRate) {
        if (refillRate < 0 | refillRate > 10) {
            LOG.error("Attempt to set watercooler refillRate to out of range value {}.", refillRate);
        }
        LeafNode<Object> leafNode = Builders.leafBuilder().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(REFILLRATEQNAME)).withValue(Uint32.valueOf(refillRate)).build();
        YangInstanceIdentifier path = YangInstanceIdentifier.of(WATERCOOLERQNAME).node(REFILLRATEQNAME);
        writeToDOMDataStore(LogicalDatastoreType.CONFIGURATION, path, leafNode);
    }

    public void setWatercoolerOverflowIndicator(Watercooler.OverflowIndicator overflowIndicator) {
        LeafNode<Object> leafNode = Builders.leafBuilder().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(OVERFLOWINDICATORQNAME)).withValue(overflowIndicator.getName()).build();
        YangInstanceIdentifier path = YangInstanceIdentifier.of(WATERCOOLERQNAME).node(OVERFLOWINDICATORQNAME);
        writeToDOMDataStore(LogicalDatastoreType.OPERATIONAL, path, leafNode);
    }
}
