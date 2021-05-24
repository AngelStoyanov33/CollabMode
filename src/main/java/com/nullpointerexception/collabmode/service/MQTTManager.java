package com.nullpointerexception.collabmode.service;

import com.nullpointerexception.collabmode.controller.DashboardController;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;

public class MQTTManager {

    private class OnMessageCallback implements MqttCallback {
        public void connectionLost(Throwable cause) {
            // After the connection is lost, it usually reconnects here
            System.out.println("disconnect，you can reconnect");
        }

        public void messageArrived(String topic, MqttMessage message) throws Exception {
            // The messages obtained after subscribe will be executed here

            System.out.println("Received message topic: " + topic);
            System.out.println("Received message Qos: " + message.getQos());
            System.out.println("Received message content: " + new String(message.getPayload()));
        }

        public void deliveryComplete(IMqttDeliveryToken token) {
            System.out.println("deliveryComplete---------" + token.isComplete());
        }
    }


    private DashboardController dashboardRef;
    private static String currentClientId;
    private static String broker = "tcp://192.168.0.106:1883";
    private static MemoryPersistence memoryPersistence;

    private static MqttClient mqttClient;

    public MQTTManager(String clientID, DashboardController dashboardRef){
        this.dashboardRef = dashboardRef;
        memoryPersistence = new MemoryPersistence();
        currentClientId = clientID;
        try {
            mqttClient = new MqttClient(broker, clientID, memoryPersistence);
            MqttConnectOptions connectOptions = new MqttConnectOptions();
//            connectOptions.setUserName("development"); //TODO: CHANGE ME
//            connectOptions.setPassword("development".toCharArray());
            connectOptions.setCleanSession(true);
            mqttClient.setCallback(new OnMessageCallback());
            try {
                mqttClient.connect(connectOptions);
                System.out.println("Connected");
            } catch (MqttException e) {
                e.printStackTrace();
            }


        } catch (MqttException me) {
            System.out.println("reason " + me.getReasonCode());
            System.out.println("msg " + me.getMessage());
            System.out.println("loc " + me.getLocalizedMessage());
            System.out.println("cause " + me.getCause());
            System.out.println("excep " + me);
        }
    }

    public static void subscribe(String topic) throws MqttException {
        mqttClient.subscribe(topic);
    }

    public static void publish(String topic, String message) throws MqttException {
        String prefix = String.format("[%s] ", currentClientId);
        message = prefix + message;
        mqttClient.publish(topic, new MqttMessage(message.getBytes(StandardCharsets.UTF_8)));
    }

    public static MqttClient getMqttClient() {
        return mqttClient;
    }
}
