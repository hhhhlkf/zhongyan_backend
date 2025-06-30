package com.gosling.bms.service;

public interface CameraService {
    // Define methods related to camera operations here

    // Check if a camera is available for a specific task
    Boolean isCameraAvailable(String task);

    // Start or stop the camera for a specific task with a given speed
    Boolean startCamera(String task, Integer camSpeed);

    // Stop the camera for a specific task with a given speed
    Boolean stopCamera(String task);

}
