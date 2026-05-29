package itmo.blps.bitrix.jca;

import itmo.blps.bitrix.jca.model.BitrixDealSnapshot;
import jakarta.resource.NotSupportedException;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.ResourceAdapterInternalException;
import jakarta.resource.spi.endpoint.MessageEndpoint;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

import java.io.PrintWriter;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BitrixResourceAdapter implements ResourceAdapter {

    private static final Logger LOG = Logger.getLogger(BitrixResourceAdapter.class.getName());

    private BitrixManagedConnectionFactory connectionFactory;
    private PrintWriter logWriter;
    private ScheduledExecutorService scheduler;
    private final Map<ActivationSpec, ScheduledFuture<?>> pollingTasks = new ConcurrentHashMap<>();
    private final Map<ActivationSpec, EndpointHolder> endpoints = new ConcurrentHashMap<>();
    private volatile Instant lastPollWatermark = Instant.EPOCH;

    @Override
    public void start(BootstrapContext ctx) throws ResourceAdapterInternalException {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "bitrix-ra-poller");
            t.setDaemon(true);
            return t;
        });
        log(Level.INFO, "Bitrix Resource Adapter started");
    }

    @Override
    public void stop() {
        pollingTasks.values().forEach(f -> f.cancel(true));
        pollingTasks.clear();
        endpoints.clear();
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        log(Level.INFO, "Bitrix Resource Adapter stopped");
    }

    @Override
    public void endpointActivation(MessageEndpointFactory endpointFactory, ActivationSpec spec)
            throws ResourceException {
        if (!(spec instanceof BitrixActivationSpec activationSpec)) {
            throw new ResourceException("Unsupported ActivationSpec");
        }
        activationSpec.setResourceAdapter(this);
        EndpointHolder holder = new EndpointHolder(endpointFactory);
        endpoints.put(activationSpec, holder);
        schedulePolling(activationSpec, holder);
        log(Level.INFO, "Bitrix inbound endpoint activated, poll interval={0}s",
                activationSpec.getPollingIntervalSeconds());
    }

    @Override
    public void endpointDeactivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) {
        ScheduledFuture<?> task = pollingTasks.remove(spec);
        if (task != null) {
            task.cancel(true);
        }
        endpoints.remove(spec);
    }

    @Override
    public XAResource[] getXAResources(ActivationSpec[] specs) throws ResourceException {
        throw new NotSupportedException("XA not supported");
    }

    public void setConnectionFactory(BitrixManagedConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public BitrixManagedConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    private void schedulePolling(BitrixActivationSpec spec, EndpointHolder holder) {
        int interval = Math.max(10, spec.getPollingIntervalSeconds());
        ScheduledFuture<?> future = scheduler.scheduleWithFixedDelay(
                () -> pollDeals(spec, holder),
                5,
                interval,
                TimeUnit.SECONDS);
        pollingTasks.put(spec, future);
    }

    private void pollDeals(BitrixActivationSpec spec, EndpointHolder holder) {
        if (connectionFactory == null) {
            return;
        }
        BitrixConnection conn = null;
        try {
            conn = connectionFactory.createConnection();
            Instant since = lastPollWatermark;
            var deals = conn.listDealsModifiedAfter(since, spec.getDealCategoryId());
            Instant maxModify = since;
            for (BitrixDealSnapshot deal : deals) {
                if (deal.getDateModify() != null && deal.getDateModify().isAfter(maxModify)) {
                    maxModify = deal.getDateModify();
                }
                deliverEvent(holder, new BitrixEventRecord(deal));
            }
            if (!deals.isEmpty()) {
                lastPollWatermark = maxModify;
            }
            deliverPollCycleComplete(holder);
        } catch (Exception e) {
            log(Level.WARNING, "Bitrix inbound poll failed: " + e.getMessage());
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    private void deliverPollCycleComplete(EndpointHolder holder) {
        MessageEndpoint endpoint = null;
        try {
            endpoint = holder.factory.createEndpoint(null);
            endpoint.beforeDelivery(BitrixEventListener.class.getMethod("onPollCycleComplete"));
            BitrixEventListener listener = (BitrixEventListener) endpoint;
            listener.onPollCycleComplete();
        } catch (Exception e) {
            log(Level.WARNING, "Failed to deliver Bitrix poll cycle event: " + e.getMessage());
        } finally {
            if (endpoint != null) {
                try {
                    endpoint.afterDelivery();
                } catch (Exception ignored) {
                }
                try {
                    endpoint.release();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void deliverEvent(EndpointHolder holder, BitrixEventRecord event) {
        MessageEndpoint endpoint = null;
        try {
            endpoint = holder.factory.createEndpoint(null);
            endpoint.beforeDelivery(BitrixEventListener.class.getMethod("onDealUpdated", BitrixEventRecord.class));
            BitrixEventListener listener = (BitrixEventListener) endpoint;
            listener.onDealUpdated(event);
        } catch (Exception e) {
            log(Level.WARNING, "Failed to deliver Bitrix event: " + e.getMessage());
        } finally {
            if (endpoint != null) {
                try {
                    endpoint.afterDelivery();
                } catch (Exception ignored) {
                }
                try {
                    endpoint.release();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void log(Level level, String msg, Object... params) {
        if (logWriter != null) {
            logWriter.println(String.format(msg.replace("{}", "%s"), params));
        } else {
            LOG.log(level, msg, params);
        }
    }

    private static final class EndpointHolder {
        private final MessageEndpointFactory factory;

        private EndpointHolder(MessageEndpointFactory factory) {
            this.factory = factory;
        }
    }
}
