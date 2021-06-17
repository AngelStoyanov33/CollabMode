package com.nullpointerexception.collabmode.service;

import com.nullpointerexception.collabmode.controller.DashboardController;
import com.nullpointerexception.collabmode.model.User;
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
            System.out.println("[MQTT] Disconnected");
            cause.printStackTrace();
            try {
                mqttClient.reconnect();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }

        public void messageArrived(String topic, MqttMessage message) throws Exception {
            String messageString = new String(message.getPayload());

            System.out.println("----------------------------------------------");
            System.out.println("Message arrived @  " + topic);
            System.out.println("QoS: " + message.getQos());
            System.out.println("Content: " + messageString);
            System.out.println("----------------------------------------------");

            if (messageString.contains("Change detected on file")) {
                    if (Integer.parseInt(messageString.charAt(1) + "") == currentUserRef.getId()) { //The user is the one making the change
                        dashboardRef.forceSave();
                    } else {    //The user is the one loading the change
                        Thread.sleep(1000);
                        dashboardRef.forceLoad();
                    }

                }

        }

        public void deliveryComplete(IMqttDeliveryToken token) {
            System.out.println("deliveryComplete---------" + token.isComplete());
        }
    }


    private final DashboardController dashboardRef;
    private static User currentUserRef;
    private static String broker = "tcp://192.168.0.100:1883";
    private static MemoryPersistence memoryPersistence;

    private static MqttClient mqttClient;

    public MQTTManager(User userRef, DashboardController dashboardRef){
        this.dashboardRef = dashboardRef;
        memoryPersistence = new MemoryPersistence();
        currentUserRef = userRef;
        String clientID = currentUserRef.getId() + "";
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
        String prefix = String.format("[%s] ", currentUserRef.getId());
        message = prefix + message;
        mqttClient.publish(topic, new MqttMessage(message.getBytes(StandardCharsets.UTF_8)));
    }

    public static MqttClient getMqttClient() {
        return mqttClient;
    }
}
