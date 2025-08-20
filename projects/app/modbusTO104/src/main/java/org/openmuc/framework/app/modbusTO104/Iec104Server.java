package org.openmuc.framework.app.modbusTO104;

import org.openmuc.j60870.*;
import org.openmuc.j60870.IeNormalizedValue;
import org.openmuc.j60870.InformationObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Iec104Server {

    private final Logger logger = LoggerFactory.getLogger(Iec104Server.class);
    private final int port;
    private Server server;
    private Connection clientConnection; // single client

    public Iec104Server(int port) {
        this.port = port;
    }

    public void start() {
        try {
            server = new Server.Builder().setPort(port).build();
            server.start(new ServerEventListener() {
                @Override
                public void connectionIndication(Connection connection) {
                    clientConnection = connection;
                    logger.info("Client connected: {}", connection);

                    try {
                        connection.startDataTransfer(new ConnectionEventListener() {
                            @Override
                            public void newASdu(ASdu aSdu) {
                                logger.info("Received ASdu: {}", aSdu);

                                // Example: If client sends interrogation command (C_IC_NA_1), respond
                                if (aSdu.getTypeIdentification() == TypeId.C_IC_NA_1) {
                                    logger.info("Interrogation command received from client.");
                            }
                                }
                            @Override
                            public void connectionClosed(IOException e) {
                                clientConnection = null;
                                logger.info("Client disconnected");
                            }

                            public void dataTransferStateChanged(Connection connection, boolean isRunning) {}
                        }, 30000);
                    } catch (IOException | TimeoutException e) {
                        logger.error("Data transfer failed");
                        clientConnection = null;
                    }
                }

                @Override
                public void serverStoppedListeningIndication(IOException e) {
                    clientConnection = null;
                    logger.info("Server stopped");
                }

                @Override
                public void connectionAttemptFailed(IOException e) {
                    logger.warn("Connection attempt failed");
                }
            });

            logger.info("IEC 104 Server started on port {}", port);

        } catch (IOException e) {
            logger.error("Server start failed", e);
        }
    }

    public void updateValue(int ioa, String value) {
        if (clientConnection == null) {
            logger.warn("No client connected. IOA={}", ioa);
            return;
        }

        int intValue;
        try {
            intValue = Integer.parseInt(value);  // parse string to integer
        } catch (NumberFormatException e) {
            logger.error("Invalid value format: {}", value);
            return; // skip sending if parsing fails
        }

        try {
            ASdu asdu = new ASdu(
                    TypeId.M_ME_NA_1,
                    false,
                    CauseOfTransmission.SPONTANEOUS,
                    false,
                    false,
                    0,
                    1,
                    new InformationObject[]{
                            new InformationObject(ioa, new IeNormalizedValue[][]{{new IeNormalizedValue(intValue)}})
                    }
            );

            try {
                clientConnection.send(asdu);
                logger.info("Updated IOA={} with value={}", ioa, intValue);
            } catch (java.util.concurrent.RejectedExecutionException e) {
                // Handle the case where the internal executor is shut down
                logger.warn("Cannot send ASdu: executor terminated, closing connection. IOA={}", ioa);
                clientConnection.close();
                clientConnection = null;
            }

        } catch (IOException e) {
            logger.error("Failed to send ASdu");
            clientConnection.close();
            clientConnection = null;
        }
    }



    public void stop() {
        if (server != null) server.stop();
        if (clientConnection != null) {
            clientConnection.close();
            clientConnection = null;
        }
        logger.info("Server stopped");
    }
}
