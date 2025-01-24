'use strict';

import httpServer from "./httpServer";

class Request {
  constructor(rawRequest, params = {}) {
    this.requestId = rawRequest.requestId;
    this.postData = rawRequest.postData;
    this.type = rawRequest.type;
    this.url = rawRequest.url;
    this.params = params; // Add dynamic URL parameters (e.g., { z: 10, x: 20, y: 30 })
  }
  get data() {
    return JSON.parse(this.postData);
  }
}
class Response {
  constructor(requestId) {
    this.requestId = requestId;
    this.closed = false;
  }

  send(code, type, body) {
    if (this.closed) {
      throw new Error('Response already sent');
    }
    if(type === 'application/x-protobuf'){
      // To send ReadableArray
      HttpServer.respondWithArray(this.requestId, code, type, body);
    } else {
      // To send a string
      HttpServer.respondWithString(this.requestId, code, type, body);
    }
    this.closed = true;
  }
  json(obj, code = 200) {
    this.send(code, 'application/json', JSON.stringify(obj));
  }
  html(html, code = 200) {
    return this.send(code, 'text/html', html);
  }
  protobuf(protobuf, code = 200) {
    return this.send(code, 'application/x-protobuf', protobuf);
  }
}

class BridgeServer {
  static server;

  constructor(serviceName, devMode=false) {
    if (!serviceName) {
      throw new Error('Invalid service name');
    }
    if (BridgeServer.server) {
      if (devMode) {
        BridgeServer.server.stop();
      } else {
        throw new Error(
          'Only one instance of HttpServer is allowed. Use HttpServer.server to access the instance.',
        );
      }
    }

    this.callbacks = [];

    this.serviceName = serviceName;
    BridgeServer.server = this;
  }

  // Add the matchRoute function here
  matchRoute(url, route) {
    const routeParts = route.split('/').filter(Boolean); // Split the route into parts
    const urlParts = url.split('/').filter(Boolean); // Split the requested URL into parts

    // If the number of parts doesn't match, return false
    if (routeParts.length !== urlParts.length) {
      return false;
    }

    let params = {}; // Store dynamic parameters
    for (let i = 0; i < routeParts.length; i++) {
      if (routeParts[i].startsWith(':')) {
        // If the part starts with ":", it's a dynamic parameter (e.g., :z)
        params[routeParts[i].substring(1)] = urlParts[i]; // Capture the parameter value
      } else if (routeParts[i] !== urlParts[i]) {
        return false; // If the part doesn't match, return false
      }
    }
    return params; // Return the captured parameters
  }

  // Update the existing route handlers
  get(url, callback) {
    this.callbacks.push({method: 'GET', url, callback});
  }
  post(url, callback) {
    this.callbacks.push({method: 'POST', url, callback});
  }
  put(url, callback) {
    this.callbacks.push({method: 'PUT', url, callback});
  }
  delete(url, callback) {
    this.callbacks.push({method: 'DELETE', url, callback});
  }
  patch(url, callback) {
    this.callbacks.push({method: 'PATCH', url, callback});
  }
  use(callback) {
    this.callbacks.push({method: '*', url: '*', callback});
  }

  listen(port) {
    if (port < 0 || port > 65535) {
      throw new Error('Invalid port number');
    }

    httpServer.start(port, this.serviceName, async rawRequest => {
      // Create the Request object, passing empty params initially
      const request = new Request(rawRequest, {});

       // Filter the callbacks using matchRoute to handle dynamic routes
      const callbacks = this.callbacks.filter(
        c => {
          if (c.method === request.type || c.method === '*') {
            const params = this.matchRoute(request.url, c.url);
            if (params !== false) {
              request.params = params; // Store dynamic parameters in request
              return true;
            }
          }
          return false; 
        }
          
      );

      for (const c of callbacks) {
        const response = new Response(request.requestId);
        const result = await c.callback(request, response);

        if (result) {
          response.json(result);
        }
        if (response.closed) {
          return;
        }
      }
    });
  }
  stop() {
    httpServer.stop();
  }
}
module.exports = {
    BridgeServer,
    Request,
    Response,
};
