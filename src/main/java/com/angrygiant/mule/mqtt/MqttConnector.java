/**
 * This file was automatically generated by the Mule Development Kit
 */

package com.angrygiant.mule.mqtt;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDefaultFilePersistence;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.mule.api.ConnectionException;
import org.mule.api.ConnectionExceptionCode;
import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.Connect;
import org.mule.api.annotations.ConnectionIdentifier;
import org.mule.api.annotations.Connector;
import org.mule.api.annotations.Disconnect;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.ValidateConnection;
import org.mule.api.annotations.display.Password;
import org.mule.api.annotations.param.ConnectionKey;
import org.mule.api.annotations.param.Default;
import org.mule.api.annotations.param.Optional;
import org.mule.api.annotations.param.Payload;
import org.mule.util.StringUtils;

/**
 * Mule MQTT Module
 * <p/>
 * Copyright (c) MuleSoft, Inc. All rights reserved. http://www.mulesoft.com
 * <p/>
 * <p>
 * The software in this package is published under the terms of the CPAL v1.0 license, a copy of
 * which has been included with this distribution in the LICENSE.md file.
 * </p>
 * <p>
 * Created with IntelliJ IDEA. User: dmiller@angrygiant.com Date: 9/21/12 Time: 9:57 AM
 * </p>
 * <p>
 * Module definition for publishing and subscribing to a given MQTT broker server.
 * </p>
 * 
 * @author dmiller@angrygiant.com
 */
@Connector(name = "mqtt", schemaVersion = "1.0", friendlyName = "MQTT", minMuleVersion = "3.3.0", description = "MQTT Module")
public class MqttConnector
{
    private static final Log LOGGER = LogFactory.getLog(MqttConnector.class);

    public static final int MQTT_DEFAULT_PORT = 1883;
    public static final String MQTT_DEFAULT_HOST = "localhost";

    /**
     * MQTT broker host name.
     */
    @Configurable
    @Optional
    @Default("localhost")
    private String brokerHostName = MQTT_DEFAULT_HOST;

    /**
     * MQTT broker port number.
     */
    @Configurable
    @Optional
    @Default("1883")
    private int brokerPort = MQTT_DEFAULT_PORT;

    /**
     * Clean Session.
     */
    @Configurable
    @Optional
    @Default("true")
    private boolean cleanSession;

    /**
     * Username to log into broker with.
     */
    @Configurable
    @Optional
    private String username;

    /**
     * Password to log into broker with.
     */
    @Configurable
    @Optional
    @Password
    private String password;

    /**
     * Connection Timeout.
     */
    @Configurable
    @Optional
    @Default("30")
    private int connectionTimeout = 30;

    /**
     * Last Will and Testimate Topic
     */
    @Configurable
    @Optional
    private String lwtTopicName;

    /**
     * Last Will and Testimate message.
     */
    @Configurable
    @Optional
    private String lwtMessage;

    /**
     * Last Will and Testimate QOS.
     */
    @Configurable
    @Optional
    @Default("2")
    private int lwtQos;

    /**
     * Last Will and Testimate retention.
     */
    @Configurable
    @Optional
    @Default("false")
    private boolean lwtRetained;

    /**
     * Keep-alive interval.
     */
    @Configurable
    @Optional
    @Default("60")
    private int keepAliveInterval = 60;

    /**
     * Directory on the machine where message persistence can be stored to disk.
     */
    @Configurable
    @Optional
    private String persistenceLocation;

    /**
     * Milliseconds of delay before subscription to a topic occurs. Gives the client time to
     * connect.
     */
    @Configurable
    @Optional
    @Default("500")
    private long subscriptionDelay;

    private MqttClient client;
    private MqttConnectOptions connectOptions = new MqttConnectOptions();

    /**
     * Connects the MQTT client.
     * 
     * @param clientId Client identifier for the broker.
     */
    @Connect
    public void connect(@ConnectionKey final String clientId) throws ConnectionException
    {
        final MqttClientPersistence clientPersistence = initializeClientPersistence();

        setupConnectOptions();

        final String brokerUrl = "tcp://" + getBrokerHostName() + ":" + getBrokerPort();

        try
        {
            LOGGER.debug("Creating client with ID of " + clientId);
            client = new MqttClient(brokerUrl, clientId, clientPersistence);
        }
        catch (final MqttException me)
        {
            throw new ConnectionException(ConnectionExceptionCode.UNKNOWN, null,
                "Mule has issues with the MQTT client", me);
        }

        if ((StringUtils.isNotBlank(getLwtTopicName())) && (StringUtils.isNotEmpty(getLwtMessage())))
        {
            LOGGER.debug("Setting up last will information...");
            final MqttTopic lwtTopic = client.getTopic(getLwtTopicName());
            connectOptions.setWill(lwtTopic, getLwtMessage().getBytes(), getLwtQos(), false);
            LOGGER.info("Last will information configured");
        }

        LOGGER.info("MQTT client successfully connected with ID: " + clientId + " at: " + brokerUrl);
    }

    private MqttClientPersistence initializeClientPersistence() throws ConnectionException
    {
        if (StringUtils.isBlank(getPersistenceLocation()))
        {
            return null;
        }

        try
        {
            final MqttClientPersistence clientPersistence = new MqttDefaultFilePersistence(
                getPersistenceLocation());
            LOGGER.info("File persistence activated at: " + getPersistenceLocation());
            return clientPersistence;
        }
        catch (final MqttPersistenceException mpe)
        {
            throw new ConnectionException(ConnectionExceptionCode.UNKNOWN, "",
                "Error creating file persistence for messages", mpe);
        }
    }

    /**
     * Method that sets up the MqttConnectOptions class for use. This reads the settings given via
     * the mqtt:config element.
     */
    private void setupConnectOptions()
    {
        connectOptions.setCleanSession(isCleanSession());
        connectOptions.setConnectionTimeout(getConnectionTimeout());
        connectOptions.setKeepAliveInterval(getKeepAliveInterval());
        connectOptions.setUserName(getUsername());

        if (StringUtils.isNotBlank(getPassword()))
        {
            connectOptions.setPassword(getPassword().toCharArray());
        }
    }

    /**
     * Disconnects the client.
     * 
     * @throws MqttException
     */
    @Disconnect
    public void disconnect() throws MqttException
    {
        if (client.isConnected())
        {
            LOGGER.info("Diconnecting from MQTT broker...");
            client.disconnect();
        }

        client = null;
        connectOptions = null;
    }

    /**
     * Are we connected
     */
    @ValidateConnection
    public boolean isConnected()
    {
        return client.isConnected();
    }

    /**
     * Connection Identifier
     */
    @ConnectionIdentifier
    public String getClientId()
    {
        return client.getClientId();
    }

    public enum DeliveryQoS
    {
        FIRE_AND_FORGET(0), AT_LEAST_ONCE(1), ONLY_ONCE(2);

        private final int code;

        private DeliveryQoS(final int code)
        {
            this.code = code;
        }

        public int getCode()
        {
            return code;
        }
    }

    /**
     * Publish a message to a topic.
     * <p/>
     * {@sample.xml ../../../doc/mqtt-connector.xml.sample mqtt:publish}
     * 
     * @param topicName topic to publish message to.
     * @param waitForCompletionTimeOut time in milliseconds to wait for the delivery to occur.
     * @param qos qos level to use when publishing message.
     * @param messagePayload the payload that will be published over MQTT.
     * @return the {@link MqttDeliveryToken}, that contains a reference to the delivered
     *         {@link MqttMessage}.
     */
    @Processor
    public MqttDeliveryToken publish(final String topicName,
                                     @Optional final Long waitForCompletionTimeOut,
                                     @Optional @Default("AT_LEAST_ONCE") final DeliveryQoS qos,
                                     @Payload final byte[] messagePayload) throws MqttException, IOException
    {
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Retrieving topic '" + topicName + "'");
        }

        final MqttTopic topic = client.getTopic(topicName);

        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Preparing message");
        }

        final MqttMessage mqttMessage = new MqttMessage(messagePayload);
        mqttMessage.setQos(qos.getCode());

        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Publishing message to broker");
        }

        final MqttDeliveryToken token = topic.publish(mqttMessage);

        if (waitForCompletionTimeOut != null)
        {
            LOGGER.debug("Waiting for completion for a maximum of " + waitForCompletionTimeOut + "ms");
            token.waitForCompletion(waitForCompletionTimeOut);
        }

        return token;
    }

    /**
     * Subscribe to a topic.
     * <p/>
     * {@sample.xml ../../../doc/mqtt-connector.xml.sample mqtt:subscribe}
     * 
     * @param subscriberId required subscriber id to use for subscription
     * @param topicName topic to publish message to
     * @param filter topic filter string, comma delimited if multiple (takes precedence over topic
     *            name)
     * @param qos qos level to use when publishing message
     * @param callback qos level to use when publishing message
     * @return
     */
    // @Source
    // public void subscribe(final String subscriberId,
    // @Optional final String topicName,
    // @Optional final String filter,
    // @Optional @Default("1") final int qos,
    // final SourceCallback callback) throws ConnectionException
    // {
    // LOGGER.info("Creating new client for topic subscription");
    // final MqttClient subscriberClient = connectClient(subscriberId);
    //
    // String[] filters;
    // int[] qoss;
    // LOGGER.info("Deciding whether filter or name is used...");
    //
    // if (StringUtils.isNotBlank(filter))
    // {
    // LOGGER.info("Building filters list");
    // filters = filter.split(",");
    //
    // LOGGER.debug("I have " + filters.length
    // + " filters defined.  Creating matching queue of qos levels");
    // qoss = new int[filters.length];
    //
    // for (int i = 0; i < filters.length; i++)
    // {
    // qoss[i] = qos;
    // }
    // }
    // else
    // {
    // filters = null;
    // qoss = null;
    // }
    //
    // try
    // {
    // subscriberClient.disconnect();
    // }
    // catch (final MqttException e)
    // {
    // LOGGER.warn("Pre-emptive disconnect called before subscription, errors occurred: " + e);
    // }
    //
    // try
    // {
    // subscriberClient.setCallback(new MqttTopicListener(subscriberClient, callback, topicName,
    // connectOptions, getSubscriptionDelay(), qos));
    // subscriberClient.connect(connectOptions);
    //
    // Thread.sleep(getSubscriptionDelay());
    //
    // if (filters != null)
    // {
    // LOGGER.debug("Subscribing to filters...");
    // subscriberClient.subscribe(filters, qoss);
    // }
    // else
    // {
    // LOGGER.debug("Subscribing to topic name...");
    // subscriberClient.subscribe(topicName, qos);
    // }
    // }
    // catch (final MqttException e)
    // {
    // LOGGER.error("MQTT Exceptions occurred subscribing", e);
    // throw new ConnectionException(ConnectionExceptionCode.UNKNOWN, null, "Subscription Error",
    // e);
    // }
    // catch (final InterruptedException ie)
    // {
    // LOGGER.error("Interrupt exception occurred sleeping before subscribing...");
    // throw new ConnectionException(ConnectionExceptionCode.UNKNOWN, null, "Interrupt Error", ie);
    // }
    //
    // LOGGER.info("Done subscribing to topic " + topicName);
    // }

    // Getters and Setters

    public MqttClient getMqttClient()
    {
        return client;
    }

    public String getBrokerHostName()
    {
        return brokerHostName;
    }

    public void setBrokerHostName(final String brokerHostName)
    {
        this.brokerHostName = brokerHostName;
    }

    public int getBrokerPort()
    {
        return brokerPort;
    }

    public void setBrokerPort(final int brokerPort)
    {
        this.brokerPort = brokerPort;
    }

    public boolean isCleanSession()
    {
        return cleanSession;
    }

    public void setCleanSession(final boolean cleanSession)
    {
        this.cleanSession = cleanSession;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(final String username)
    {
        this.username = username;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(final String password)
    {
        this.password = password;
    }

    public int getConnectionTimeout()
    {
        return connectionTimeout;
    }

    public void setConnectionTimeout(final int connectionTimeout)
    {
        this.connectionTimeout = connectionTimeout;
    }

    public String getLwtTopicName()
    {
        return lwtTopicName;
    }

    public void setLwtTopicName(final String lwtTopicName)
    {
        this.lwtTopicName = lwtTopicName;
    }

    public String getLwtMessage()
    {
        return lwtMessage;
    }

    public void setLwtMessage(final String lwtMessage)
    {
        this.lwtMessage = lwtMessage;
    }

    public int getKeepAliveInterval()
    {
        return keepAliveInterval;
    }

    public void setKeepAliveInterval(final int keepAliveInterval)
    {
        this.keepAliveInterval = keepAliveInterval;
    }

    public String getPersistenceLocation()
    {
        return persistenceLocation;
    }

    public void setPersistenceLocation(final String persistenceLocation)
    {
        this.persistenceLocation = persistenceLocation;
    }

    public int getLwtQos()
    {
        return lwtQos;
    }

    public void setLwtQos(final int lwtQos)
    {
        this.lwtQos = lwtQos;
    }

    public boolean isLwtRetained()
    {
        return lwtRetained;
    }

    public void setLwtRetained(final boolean lwtRetained)
    {
        this.lwtRetained = lwtRetained;
    }

    public long getSubscriptionDelay()
    {
        return subscriptionDelay;
    }

    public void setSubscriptionDelay(final long subscriptionDelay)
    {
        this.subscriptionDelay = subscriptionDelay;
    }
}
