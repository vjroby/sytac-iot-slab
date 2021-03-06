package com.sfirsov.sytac.iot.slab.service;

import com.sfirsov.sytac.iot.slab.device.Motor;
import com.sfirsov.sytac.iot.slab.model.MotorState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class DeviceControl {

    @Value("${ubidots.api.key}")
    private String ubidotsApiKey;

    @Value("${rpi.pin.motor1.pin1}")
    private int motor1Pin1;

    @Value("${rpi.pin.motor1.pin2}")
    private int motor1Pin2;

    private String ubidotsToken;

    private RestTemplate restTemplate = new RestTemplate();

    private Motor motor;

    private void initIfNot() {
        if(ubidotsToken == null) {
            try {
                ubidotsToken = getUbidotsToken();
            }
            catch (Exception e) {
                System.out.println("Cant obtain Ubidots token");
                return;
            }
        }

        if(motor == null) {
            try {
                motor = new Motor(motor1Pin1, motor1Pin2);
            }
            catch (UnsatisfiedLinkError e) {
                System.out.println("Cant initialize Raspberry Pi GPIO");
            }
        }
    }

    private String getUbidotsToken() {
        String getTockenUrl = "http://things.ubidots.com/api/v1.6/auth/token";

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Ubidots-ApiKey", ubidotsApiKey);

        HttpEntity<String> request = new HttpEntity("", headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(getTockenUrl, request, Map.class);
        Map<String, String> body = response.getBody();
        return body.get("token");
    }

    private MotorState getMotorState() {
        MotorState motorState = new MotorState();
        String getMotorEngagedUrl = "http://things.ubidots.com/api/v1.6/devices/wheel/wheel-1-engaged/lv?token=" + ubidotsToken;
        HttpEntity<Integer> motorEngaged = restTemplate.getForEntity(getMotorEngagedUrl, Integer.class);
        motorState.setEngaged(motorEngaged.getBody() > 0);

        return motorState;
    }

    @Scheduled(initialDelay = 10000, fixedRate = 500)
    private void fetchData() {
        try {
            initIfNot();

            MotorState motorState = getMotorState();
            System.out.println("Motor 1 engaged: " + motorState.isEngaged());

            motor.go(motorState);
        }
        catch (Exception e) {
            // If something went wrong - reset Ubidots token and init RPi pins again
            ubidotsToken = null;
            motor = null;
            System.out.println(e.getMessage());
        }
    }
}
