"use strict";

import httpServer from "./httpServer";

//The BridgeServer now only focuses on handling the server's lifecycle:
class BridgeServer {
  static server;

  constructor(serviceName, devMode = false) {
    if (!serviceName) {
      throw new Error("Invalid service name");
    }
    if (BridgeServer.server) {
      if (devMode) {
        BridgeServer.server.stop();
      } else {
        throw new Error(
          "Only one instance of HttpServer is allowed. Use HttpServer.server to access the instance."
        );
      }
    }

    this.serviceName = serviceName;
    BridgeServer.server = this;
  }

  listen(port) {
    if (port < 0 || port > 65535) {
      throw new Error("Invalid port number");
    }

    // Start the server using the native module
    httpServer.start(port, this.serviceName);
  }

  stop() {
    // Stop the server using the native module
    httpServer.stop();
  }
}

module.exports = {
  BridgeServer,
};
