package org.openmuc.framework.app.modbusTO104;

import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.dataaccess.Channel;
import org.openmuc.framework.dataaccess.DataAccessService;
import org.openmuc.framework.dataaccess.RecordListener;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = {})
public final class ModbusApp {

    private static final Logger logger = LoggerFactory.getLogger(ModbusApp.class);

    private DataAccessService dataAccessService;
    private Channel modbusChannel;
    private RecordListener modbusListener;
    private boolean isDeviceConnected = true;

    private Iec104Server iec104Server;

    @Reference
    public void setDataAccessService(DataAccessService dataAccessService) {
        this.dataAccessService = dataAccessService;
    }

    @Activate
    private void activate() {
        logger.info("Activating Modbus to IEC 104 App");

        // Start IEC 104 server
        iec104Server = new Iec104Server(2404);
        iec104Server.start();

        // Get Modbus channel
        modbusChannel = dataAccessService.getChannel("register1");
        if (modbusChannel == null) {
            logger.error("Failed to get Modbus channel 'register1'. Check channels.xml configuration.");
            return;
        }

        // Set up reader
        modbusListener = new ModbusListener();
        modbusChannel.addListener(modbusListener);

        logger.info("Modbus to IEC 104 App activated successfully");
    }

    @Deactivate
    private void deactivate() {
        if (modbusChannel != null && modbusListener != null) {
            modbusChannel.removeListener(modbusListener);
        }
        if (iec104Server != null) {
            iec104Server.stop();
        }
        logger.info("Modbus to IEC 104 App deactivated successfully");
    }

    private class ModbusListener implements RecordListener {
        @Override
        public void newRecord(Record record) {
            if (record != null && record.getValue() != null && record.getFlag() == Flag.VALID) {
                isDeviceConnected = true;
                Number readValue = record.getValue().asShort();
                logger.info("Read value from Modbus: {}", readValue);

                // Forward value to IEC 104
                if (iec104Server != null) {
                    iec104Server.updateValue(1, readValue.toString());
                }
            } else {
                isDeviceConnected = false;
                logger.warn("Invalid record or device disconnected");
            }
        }
    }
}
