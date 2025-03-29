"use strict";

// httpServer.js
import { NativeModules } from "react-native";
const OkhttpInterceptor = NativeModules.HttpServer;

const HttpServer = {
  mapRef: null, // This will hold the reference to MapView

  // Initialize the interceptor and handle map reference
  initializeInterceptor: () => {
    return new Promise((resolve, reject) => {
      OkhttpInterceptor.initializeInterceptor()
        .then(() => {
          console.log("Interceptor initialized");

          // Inject the MapView reference into the OkhttpInterceptor for future requests
          if (HttpServer.mapRef) {
            OkhttpInterceptor.setMapView(HttpServer.mapRef);
          }

          resolve("Interceptor initialized successfully");
        })
        .catch((error) => {
          console.error("Interceptor initialization failed:", error);
          reject("Interceptor initialization failed");
        });
    });
  },

  // Set the MapView reference (if needed for dynamic updates)
  setMapView: (mapViewRef) => {
    HttpServer.mapRef = mapViewRef;
    OkhttpInterceptor.setMapView(HttpServer.mapRef); // Inject into the OkhttpInterceptor
  },

  // Cleanup or stop the interceptor and map reference
  cleanupInterceptor: () => {
    // Clear map reference
    if (HttpServer.mapRef) {
      HttpServer.mapRef = null;
      console.log("MapView reference cleared.");
    }

    // Cleanup the interceptor (this would depend on the OkhttpInterceptor's API)
    OkhttpInterceptor.cleanupInterceptor()
      .then(() => {
        console.log("Interceptor cleanup successful.");
      })
      .catch((error) => {
        console.error("Interceptor cleanup failed:", error);
      });
  },

  // Optionally, other methods to handle server shutdown can go here
  stopServer: () => {
    // Perform additional server stopping tasks (e.g., stopping background tasks)
    console.log("Server stopped.");
  },
};

// Exporting the module so you can access it easily
export default HttpServer;

/*
import { NativeModules } from "react-native";

const { OkhttpInterceptor } = NativeModules;

// Initialize the interceptor when the app starts
OkhttpInterceptor.initializeInterceptor()
  .then(() => console.log("Interceptor initialized"))
  .catch((error) => console.error("Interceptor failed:", error));


import { NativeModules } from "react-native";
const Server = NativeModules.HttpServer;

module.exports = {
  start: function (port, bindAddress, serviceName, callback) {
    if (port === 80) {
      throw "Invalid server port specified. Port 80 is reserved.";
    }
    Server.start(port, bindAddress, serviceName, callback);
  },

  stop: () => {
    Server.stop();
  },

  styleJson: (style) => {
    Server.styleJson(style);
  }
};

*/
