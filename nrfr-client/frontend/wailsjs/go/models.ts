export namespace main {

    export class DeviceInfo {
        serial: string;
        state: string;
        product: string;
        model: string;

        static createFrom(source: any = {}) {
            return new DeviceInfo(source);
        }

        constructor(source: any = {}) {
            if ('string' === typeof source) source = JSON.parse(source);
            this.serial = source["serial"];
            this.state = source["state"];
            this.product = source["product"];
            this.model = source["model"];
        }
    }

}

