"use strict";

// httpServer.js - bridge module (HttpServer) that interfaces with the native module
import { NativeModules } from "react-native";
const OkhttpInterceptor = NativeModules.OkhttpInterceptor;

const HttpServer = {
  // Initialize the interceptor and handle map reference
  initializeInterceptor: (folder, file) => {
    return new Promise((resolve, reject) => {
      OkhttpInterceptor.initializeInterceptor(folder, file)
        .then(() => {
          console.log("Interceptor initialized");

          resolve("Interceptor initialized successfully");
        })
        .catch((error) => {
          console.error("Interceptor initialization failed:", error);
          reject("Interceptor initialization failed");
        });
    });
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
