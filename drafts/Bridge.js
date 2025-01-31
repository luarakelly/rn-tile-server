import { NativeModules } from "react-native";
const { HttpServer } = NativeModules;

// Start the server
HttpServer.startServer();

// Stop the server
HttpServer.stopServer();

/**
 * import { AppState } from 'react-native';
import { NativeModules } from 'react-native';

const { HttpServer } = NativeModules;

useEffect(() => {
  // Start the server when the app is active
  HttpServer.startServer();

  const appStateListener = AppState.addEventListener('change', (nextAppState) => {
    if (nextAppState === 'background') {
      HttpServer.stopServer();  // Stop server when the app goes into the background
    } else if (nextAppState === 'active') {
      HttpServer.startServer();  // Restart server when the app comes back
    }
  });

  // Cleanup the listener on unmount
  return () => {
    appStateListener.remove();
    HttpServer.stopServer();  // Stop the server when the component is unmounted
  };
}, []);

 */
