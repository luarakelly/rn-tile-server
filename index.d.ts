declare module "react-native-http-bridge-refurbished" {
    function start(
        port: number,
        serviceName: string,
        callback: (request: {
            requestId: string;
            postData?: {};
            type: string;
            url: string;
        }) => void
    ): void;

    function stop(): void;

    function respondWithArray(
        requestId: string,
        code: number,
        type: string,
        body: Array<number> // This represents the byte array (e.g., a Uint8Array)
    ): void;

    function respondWithString(
        requestId: string,
        code: number,
        type: string,
        body: string // This represents a string (e.g., JSON, text response)
    ): void;

    export type RawRequest = {
        requestId: string;
        postData?: {};
        type: string;
        url: string;
    };

    export class Request<T> {
        public readonly requestId: string;
        public readonly postData?: {};
        public readonly type: string;
        public readonly url: string;

        constructor(rawRequest: RawRequest);

        public get data();
    }

    export class Response {
        private readonly requestId: string;
        public closed: boolean;

        constructor(requestId: string);

        public send(code: number, type: string, body: string | Array<number>);

        public json(obj: object, code?: number);

        public html(html: string, code?: number);
    }

    export type HttpCallback<T> = (
        request: Request<T>,
        response: Response,
    ) => Promise<object | void>;

    export type HttpCallbackContainer<T> = {
        method: string;
        url: string;
        callback: HttpCallback<T>;
    };

    export class BridgeServer {
        public static server: BridgeServer;

        public serviceName: string;
        private callbacks: HttpCallbackContainer<any>[];

        constructor(serviceName: string, devMode?: boolean);

        public get(url: string, callback: HttpCallback<any>);

        public post<T>(url: string, callback: HttpCallback<T>);

        public put<T>(url: string, callback: HttpCallback<T>);

        public delete<T>(url: string, callback: HttpCallback<T>);

        public patch<T>(url: string, callback: HttpCallback<T>);

        public use(callback: HttpCallback<any>);

        public listen(port: number);

        public stop();
    }
}
