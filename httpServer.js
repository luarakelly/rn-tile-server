"use strict";

import { NativeModules } from "react-native";
const Server = NativeModules.HttpServer;

module.exports = {
  start: function (port, serviceName) {
    if (port === 80) {
      throw "Invalid server port specified. Port 80 is reserved.";
    }
    Server.start(port, serviceName);
  },

  stop: () => {
    Server.stop();
  }
}
