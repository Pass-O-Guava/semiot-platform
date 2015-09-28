package ru.semiot.platform.drivers.winghouse.machinetool;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import ru.semiot.platform.deviceproxyservice.api.drivers.Device;
import ru.semiot.platform.deviceproxyservice.api.drivers.DeviceDriver;
import ru.semiot.platform.deviceproxyservice.api.drivers.DeviceManager;

public class DeviceDriverImpl implements DeviceDriver, ManagedService {

    private static final String UDP_PORT_KEY = Activator.SERVICE_PID + ".udp_port";
            
    private final List<Device> listDevices = Collections.synchronizedList(new ArrayList<Device>());

    private volatile DeviceManager deviceManager;

    private String templateDescription;
    private String templateButtonsObservation;
    private String templateWorkingStateObservation;
    private EventLoopGroup group;
    private Channel channel;
    private int port;

    public List<Device> listDevices() {
        return listDevices;
    }

    public void start() {
        System.out.println("Winghouse machine-tools driver started!");

        readTemplates();

        group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioDatagramChannel.class)
                    .option(ChannelOption.SO_BROADCAST, true)
                    .handler(new DeviceHandler(this));

            channel = b.bind(port).channel();
        } catch (Exception ex) {
            ex.printStackTrace();

            group.shutdownGracefully();
        }
    }

    public void stop() {
        if (channel != null) {
            channel.close();
        }
        if (group != null) {
            group.shutdownGracefully();
        }

        System.out.println("Winghouse machine-tools driver stopped!");
    }

    public void updated(Dictionary properties) throws ConfigurationException {
        synchronized(this) {
            System.out.println(properties == null);
            if(properties != null) {
                port = (Integer) properties.get(UDP_PORT_KEY);
            }
        }
    }

    public void publish(String topic, String message) {
        deviceManager.publish(topic, message);
    }

    public void addDevice(Device device) {
    	listDevices.add(device);
        deviceManager.register(device);
    }

    public boolean contains(Device device) {
        return listDevices.contains(device);
    }

    public String getTemplateDescription() {
        return templateDescription;
    }

    public String getTemplateButtonsObservation() {
        return templateButtonsObservation;
    }

    public String getTemplateWorkingStateObservation() {
        return templateWorkingStateObservation;
    }

    private void readTemplates() {
        try {
            this.templateDescription = IOUtils.toString(DeviceHandler.class
                    .getResourceAsStream("/ru/semiot/platform/drivers/winghouse/machinetool/descriptionMachineTools.ttl"));
            this.templateButtonsObservation = IOUtils.toString(DeviceHandler.class
                    .getResourceAsStream("/ru/semiot/platform/drivers/winghouse/machinetool/buttonsObservation.ttl"));
            this.templateWorkingStateObservation = IOUtils.toString(DeviceHandler.class
                    .getResourceAsStream("/ru/semiot/platform/drivers/winghouse/machinetool/workingStateObservation.ttl"));
        } catch (IOException ex) {
            System.out.println("Can't read templates");
            throw new IllegalArgumentException(ex);
        }
    }

}