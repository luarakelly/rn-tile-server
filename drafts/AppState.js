/**
 * Server.java: The HTTP server is wrapped in a Server class (as you already had).
HttpServerModule: We created a React Native module to bridge the server's start and stop functionality to JavaScript.
AppState: We used AppState to manage the lifecycle of the server (start/stop based on whether the app is in the background or foreground).
EAS Build: We configure EAS Build to handle custom native code (without ejecting).
 * 
 * You can start and stop the server based on app lifecycle events. Use AppState to detect when the app is in the foreground or background:

 */
import { AppState } from "react-native";

useEffect(() => {
  const appStateListener = AppState.addEventListener(
    "change",
    (nextAppState) => {
      if (nextAppState === "background") {
        HttpServer.stopServer(); // Stop server when app goes into the background
      } else if (nextAppState === "active") {
        HttpServer.startServer(); // Restart server when app becomes active
      }
    }
  );

  return () => {
    appStateListener.remove();
  };
}, []);

/**
 * import React, { useEffect } from 'react';
import { AppState } from 'react-native';
import { NativeModules } from 'react-native';

const { HttpServer } = NativeModules;

const App = () => {
  useEffect(() => {
    // Start the server when the app mounts (initial load)
    HttpServer.startServer();

    // Set up listener for app state changes
    const appStateListener = AppState.addEventListener('change', (nextAppState) => {
      if (nextAppState === 'background') {
        HttpServer.stopServer();  // Stop the server when the app goes into the background
      } else if (nextAppState === 'active') {
        HttpServer.startServer();  // Restart the server when the app comes back to the foreground
      }
    });

    // Clean up the listener on unmount
    return () => {
      appStateListener.remove();
      HttpServer.stopServer();  // Optionally, stop the server when the component is unmounted
    };
  }, []);

  return (
    // Your app's UI goes here
    <></>
  );
};

export default App;

 */
