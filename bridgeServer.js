"use strict";

import httpServer from "./httpServer";

//The BridgeServer have to only focuses on handling the server's lifecycle and server assets:
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
  };

  storagePath(callback) {
    httpServer.storagePath(callback);
  }
  styleJson(style) {
    httpServer.styleJson(style);
  }
  
  listen(port, bindAddress, callback) {
    if (port < 0 || port > 65535) {
      throw new Error("Invalid port number");
    }

    // Start the server using the native module and pass a callback
    httpServer.start(port, bindAddress, this.serviceName, callback);
  };

  stop() {
    // Stop the server using the native module
    httpServer.stop();
  };
};

module.exports = {
  BridgeServer
};
