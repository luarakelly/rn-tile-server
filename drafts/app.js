import React, { useState, useEffect } from "react";
import { AppState, View, Text, ActivityIndicator } from "react-native";
import { NativeModules } from "react-native";

const { HttpServer } = NativeModules;

const App = () => {
  const [isServerStarting, setIsServerStarting] = useState(true);

  useEffect(() => {
    // Start the server when the app mounts
    HttpServer.startServer();

    // Set up listener for app state changes
    const appStateListener = AppState.addEventListener(
      "change",
      (nextAppState) => {
        if (nextAppState === "background") {
          HttpServer.stopServer(); // Stop server when the app goes into the background
        } else if (nextAppState === "active") {
          HttpServer.startServer(); // Restart server when the app comes back to the foreground
        }
      }
    );

    // Mock delay to simulate server startup
    setTimeout(() => setIsServerStarting(false), 2000); // Simulate a 2-second startup

    // Cleanup the listener on unmount
    return () => {
      appStateListener.remove();
      HttpServer.stopServer();
    };
  }, []);

  if (isServerStarting) {
    return (
      <View>
        <ActivityIndicator size="large" color="#0000ff" />
        <Text>Starting server...</Text>
      </View>
    );
  }

  return (
    <View>
      {/* Main app UI goes here */}
      <Text>App is ready!</Text>
    </View>
  );
};

export default App;

/**
 * import React, { useEffect } from 'react';
import { AppState, View, Text } from 'react-native';
import { NativeModules } from 'react-native';

const { HttpServer } = NativeModules;

const App = () => {
  useEffect(() => {
    // Start the server when the app mounts
    HttpServer.startServer();

    // Set up listener for app state changes (background/foreground)
    const appStateListener = AppState.addEventListener('change', (nextAppState) => {
      if (nextAppState === 'background') {
        HttpServer.stopServer();  // Stop the server when the app goes into the background
      } else if (nextAppState === 'active') {
        HttpServer.startServer();  // Restart the server when the app comes back to the foreground
      }
    });

    // Cleanup listener on unmount
    return () => {
      appStateListener.remove();
      HttpServer.stopServer();  // Optionally stop server when app is closed or unmounted
    };
  }, []);

  return (
    <View>
      {/* Your map UI goes here *}
      <Text>App is running. Server is active.</Text>
    </View>
  );
};

export default App;

 */
