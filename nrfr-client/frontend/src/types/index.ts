import type {main} from '../../wailsjs/go/models';

export type DeviceInfo = main.DeviceInfo;

export interface AppStatus {
    [key: string]: boolean;
}

export type Step = 1 | 2 | 3 | 4 | 5;
