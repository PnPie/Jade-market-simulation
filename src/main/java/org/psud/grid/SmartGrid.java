package org.psud.grid;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import org.psud.grid.role.Consumer;
import org.psud.grid.role.Observer;
import org.psud.grid.role.Supplier;

import java.util.ArrayList;
import java.util.List;

public class SmartGrid {

    private static final int DEFAULT_SUPPLIER_NUM = 2;
    private static final int DEFAULT_CONSUMER_NUM = 4;

    // Runtime is an singleton instance which represents the JADE runtime system
    private static final Runtime jadeRuntime = Runtime.instance();

    private List<AgentController> suppliers = new ArrayList<>();
    private List<AgentController> consumers = new ArrayList<>();
    private AgentController observer;

    private String host;
    private int port;
    private String platformID;
    private boolean isMain;

    SmartGrid(String host, int port, String platformID, boolean isMain) {
        this.host = host;
        this.port = port;
        this.platformID = platformID;
        this.isMain = isMain;
    }

    static {
        jadeRuntime.setCloseVM(true);
    }


    /**
     * Create a container, which holds agent.
     * <p>
     * A main container could coordinate all the other nodes to form a distributed platform.
     *
     * @param host       the host on which the main container listens to
     * @param port       the port on which the main container listens to
     * @param platformID name of the platform
     * @param isMain     specify whether it's a main container if true, otherwise it would be an agent container
     */
    private AgentContainer createContainer(String host, int port, String platformID, boolean isMain) {
        Profile profile = new ProfileImpl(host, port, platformID, isMain);
        if (isMain) {
            return jadeRuntime.createMainContainer(profile);
        } else {
            return jadeRuntime.createAgentContainer(profile);
        }
    }

    private void init() throws StaleProxyException {
        AgentContainer container = createContainer(host, port, platformID, isMain);
        for (int i = 0; i < DEFAULT_SUPPLIER_NUM; i++) {
            AgentController supplier = container.createNewAgent("Supplier-" + i, Supplier.class.getName(), new Object[0]);
            suppliers.add(supplier);
        }
        for (int i = 0; i < DEFAULT_CONSUMER_NUM; i++) {
            AgentController consumer = container.createNewAgent("Consumer-" + i, Consumer.class.getName(), new Object[0]);
            consumers.add(consumer);
        }
        observer = container.createNewAgent("Observer", Observer.class.getName(), new Object[0]);
    }

    public void startUp() throws StaleProxyException {
        init();
        for (AgentController supplier : suppliers) {
            supplier.start();
        }
        for (AgentController consumer : consumers) {
            consumer.start();
        }
        observer.start();
    }


    public static void main(String[] args) throws StaleProxyException {

        SmartGrid smartGrid = new SmartGrid("localhost", 1001, "main-container-1", true);
        smartGrid.startUp();
    }
}