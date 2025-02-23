"use strict";

import { NativeModules } from "react-native";

const { OkhttpInterceptor } = NativeModules;

// Initialize the interceptor when the app starts
OkhttpInterceptor.initializeInterceptor()
  .then(() => console.log("Interceptor initialized"))
  .catch((error) => console.error("Interceptor failed:", error));

/*
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
